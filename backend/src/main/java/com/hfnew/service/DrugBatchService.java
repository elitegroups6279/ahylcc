package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.pharmacy.DrugBatchCreateRequest;
import com.hfnew.dto.pharmacy.DrugBatchVO;
import com.hfnew.entity.Drug;
import com.hfnew.entity.DrugBatch;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.DrugBatchMapper;
import com.hfnew.mapper.DrugMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DrugBatchService {

    private final DrugBatchMapper drugBatchMapper;
    private final DrugMapper drugMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<DrugBatchVO> list(int page, int pageSize, Long drugId) {
        Page<DrugBatch> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<DrugBatch> wrapper = new LambdaQueryWrapper<>();
        if (drugId != null) wrapper.eq(DrugBatch::getDrugId, drugId);
        wrapper.orderByDesc(DrugBatch::getId);
        IPage<DrugBatch> result = drugBatchMapper.selectPage(pageReq, wrapper);
        Map<Long, String> nameMap = loadDrugNames(result.getRecords().stream().map(DrugBatch::getDrugId).collect(Collectors.toList()));
        List<DrugBatchVO> list = result.getRecords().stream().map(b -> toVO(b, nameMap.get(b.getDrugId()))).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long create(Long operatorId, DrugBatchCreateRequest request) {
        if (request.getDrugId() == null) throw new BizException(400, 400, "请选择药品");
        if (request.getQuantity() == null || request.getQuantity() <= 0) throw new BizException(400, 400, "数量必须大于0");
        if (request.getExpiryDate() == null) throw new BizException(400, 400, "请选择有效期");
        Drug d = drugMapper.selectById(request.getDrugId());
        if (d == null) throw new BizException(404, 404, "药品不存在");

        DrugBatch b = new DrugBatch();
        b.setDrugId(request.getDrugId());
        b.setBatchNo(request.getBatchNo());
        b.setQuantity(request.getQuantity());
        b.setRemaining(request.getQuantity());
        b.setExpiryDate(request.getExpiryDate());
        b.setInDate(request.getInDate() == null ? LocalDate.now() : request.getInDate());
        b.setSupplier(request.getSupplier());
        b.setOperatorId(operatorId);
        drugBatchMapper.insert(b);
        return b.getId();
    }

    private Map<Long, String> loadDrugNames(List<Long> drugIds) {
        Map<Long, String> map = new HashMap<>();
        if (drugIds == null || drugIds.isEmpty()) return map;
        List<Long> ids = drugIds.stream().distinct().filter(x -> x != null).collect(Collectors.toList());
        if (ids.isEmpty()) return map;
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, name FROM t_drug WHERE deleted = 0 AND id IN (" + in + ")";
        jdbcTemplate.query(sql, (RowCallbackHandler) rs -> map.put(rs.getLong("id"), rs.getString("name")), ids.toArray());
        return map;
    }

    private DrugBatchVO toVO(DrugBatch b, String drugName) {
        DrugBatchVO vo = new DrugBatchVO();
        vo.setId(b.getId());
        vo.setDrugId(b.getDrugId());
        vo.setDrugName(drugName);
        vo.setBatchNo(b.getBatchNo());
        vo.setQuantity(b.getQuantity());
        vo.setRemaining(b.getRemaining());
        vo.setExpiryDate(b.getExpiryDate());
        vo.setInDate(b.getInDate());
        vo.setSupplier(b.getSupplier());
        vo.setOperatorId(b.getOperatorId());
        vo.setCreateTime(b.getCreateTime());
        return vo;
    }
}
