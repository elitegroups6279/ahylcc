package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.pharmacy.DispenseConfirmRequest;
import com.hfnew.dto.pharmacy.DispenseItemRequest;
import com.hfnew.dto.pharmacy.DispenseOrderCreateRequest;
import com.hfnew.dto.pharmacy.DispenseOrderVO;
import com.hfnew.entity.DispenseOrder;
import com.hfnew.entity.DispenseRecord;
import com.hfnew.entity.DrugBatch;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.DispenseOrderMapper;
import com.hfnew.mapper.DispenseRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispenseService {

    private final DispenseOrderMapper dispenseOrderMapper;
    private final DispenseRecordMapper dispenseRecordMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<DispenseOrderVO> listOrders(int page, int pageSize, String status) {
        Page<DispenseOrder> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<DispenseOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) wrapper.eq(DispenseOrder::getStatus, status);
        wrapper.orderByDesc(DispenseOrder::getCreateTime).orderByDesc(DispenseOrder::getId);
        IPage<DispenseOrder> result = dispenseOrderMapper.selectPage(pageReq, wrapper);

        Map<Long, String> elderlyNameMap = loadElderlyNames(result.getRecords().stream().map(DispenseOrder::getElderlyId).collect(Collectors.toList()));
        List<DispenseOrderVO> list = result.getRecords().stream().map(o -> toVO(o, elderlyNameMap.get(o.getElderlyId()))).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long createOrder(Long operatorId, DispenseOrderCreateRequest request) {
        if (request.getElderlyId() == null) throw new BizException(400, 400, "请选择老人");
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_elderly WHERE deleted = 0 AND id = ?",
                Integer.class,
                request.getElderlyId()
        );
        if (cnt == null || cnt == 0) throw new BizException(404, 404, "老人不存在");

        DispenseOrder order = new DispenseOrder();
        order.setElderlyId(request.getElderlyId());
        order.setOrderDate(request.getOrderDate() == null ? LocalDate.now() : request.getOrderDate());
        order.setStatus("PENDING");
        order.setOperatorId(operatorId);
        order.setRemark(request.getRemark());
        dispenseOrderMapper.insert(order);
        return order.getId();
    }

    @Transactional
    public void confirm(Long orderId, DispenseConfirmRequest request) {
        DispenseOrder order = dispenseOrderMapper.selectById(orderId);
        if (order == null) throw new BizException(404, 404, "发药单不存在");
        if (!Objects.equals(order.getStatus(), "PENDING")) throw new BizException(400, 400, "当前状态不可发药");
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) throw new BizException(400, 400, "请填写发药明细");

        LocalDateTime now = LocalDateTime.now();

        for (DispenseItemRequest item : request.getItems()) {
            if (item == null) continue;
            if (item.getDrugId() == null) throw new BizException(400, 400, "请选择药品");
            if (item.getQuantity() == null || item.getQuantity() <= 0) throw new BizException(400, 400, "数量必须大于0");

            int need = item.getQuantity();
            List<DrugBatch> batches = jdbcTemplate.query(
                    """
                            SELECT id, drug_id, batch_no, quantity, remaining, expiry_date, in_date, supplier, operator_id, deleted, create_time, update_time
                            FROM t_drug_batch
                            WHERE deleted = 0 AND drug_id = ? AND remaining > 0
                            ORDER BY expiry_date ASC, in_date ASC, id ASC
                            FOR UPDATE
                            """,
                    (rs, rowNum) -> {
                        DrugBatch b = new DrugBatch();
                        b.setId(rs.getLong("id"));
                        b.setDrugId(rs.getLong("drug_id"));
                        b.setBatchNo(rs.getString("batch_no"));
                        b.setQuantity(rs.getInt("quantity"));
                        b.setRemaining(rs.getInt("remaining"));
                        b.setExpiryDate(rs.getDate("expiry_date").toLocalDate());
                        b.setInDate(rs.getDate("in_date").toLocalDate());
                        b.setSupplier(rs.getString("supplier"));
                        Object op = rs.getObject("operator_id");
                        if (op != null) b.setOperatorId(((Number) op).longValue());
                        return b;
                    },
                    item.getDrugId()
            );

            int available = batches.stream().mapToInt(b -> b.getRemaining() == null ? 0 : b.getRemaining()).sum();
            if (available < need) {
                throw new BizException(400, 400, "药品库存不足");
            }

            for (DrugBatch b : batches) {
                if (need <= 0) break;
                int remain = b.getRemaining() == null ? 0 : b.getRemaining();
                if (remain <= 0) continue;
                int take = Math.min(remain, need);

                int updated = jdbcTemplate.update(
                        "UPDATE t_drug_batch SET remaining = remaining - ?, update_time = CURRENT_TIMESTAMP WHERE deleted = 0 AND id = ? AND remaining >= ?",
                        take,
                        b.getId(),
                        take
                );
                if (updated <= 0) {
                    throw new BizException(409, 409, "药品批次库存变化，请重试");
                }

                DispenseRecord record = new DispenseRecord();
                record.setOrderId(orderId);
                record.setDrugId(item.getDrugId());
                record.setBatchId(b.getId());
                record.setDosage(item.getDosage());
                record.setQuantity(take);
                record.setDispenseTime(now);
                record.setExecutorId(item.getExecutorId());
                record.setRemark(item.getRemark());
                dispenseRecordMapper.insert(record);

                need -= take;
            }
        }

        order.setStatus("DISPENSED");
        dispenseOrderMapper.updateById(order);
    }

    private Map<Long, String> loadElderlyNames(List<Long> elderlyIds) {
        Map<Long, String> map = new HashMap<>();
        if (elderlyIds == null || elderlyIds.isEmpty()) return map;
        List<Long> ids = elderlyIds.stream().distinct().filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) return map;
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, name FROM t_elderly WHERE deleted = 0 AND id IN (" + in + ")";
        jdbcTemplate.query(sql, (RowCallbackHandler) rs -> map.put(rs.getLong("id"), rs.getString("name")), ids.toArray());
        return map;
    }

    private DispenseOrderVO toVO(DispenseOrder o, String elderlyName) {
        DispenseOrderVO vo = new DispenseOrderVO();
        vo.setId(o.getId());
        vo.setElderlyId(o.getElderlyId());
        vo.setElderlyName(elderlyName);
        vo.setOrderDate(o.getOrderDate());
        vo.setStatus(o.getStatus());
        vo.setOperatorId(o.getOperatorId());
        vo.setRemark(o.getRemark());
        vo.setCreateTime(o.getCreateTime());
        return vo;
    }
}
