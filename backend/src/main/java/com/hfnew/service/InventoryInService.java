package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.warehouse.InventoryInCreateRequest;
import com.hfnew.dto.warehouse.InventoryInVO;
import com.hfnew.entity.InventoryIn;
import com.hfnew.entity.Stock;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.InventoryInMapper;
import com.hfnew.mapper.MaterialMapper;
import com.hfnew.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryInService {

    private final InventoryInMapper inventoryInMapper;
    private final StockMapper stockMapper;
    private final MaterialMapper materialMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<InventoryInVO> list(int page, int pageSize) {
        Page<InventoryIn> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<InventoryIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(InventoryIn::getCreateTime).orderByDesc(InventoryIn::getId);
        IPage<InventoryIn> result = inventoryInMapper.selectPage(pageReq, wrapper);

        Map<Long, String> nameMap = loadMaterialNames(result.getRecords().stream().map(InventoryIn::getMaterialId).collect(Collectors.toList()));
        List<InventoryInVO> list = result.getRecords().stream().map(r -> toVO(r, nameMap.get(r.getMaterialId()))).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long create(Long operatorId, InventoryInCreateRequest request) {
        if (request.getMaterialId() == null) throw new BizException(400, 400, "请选择物资");
        if (request.getQuantity() == null || request.getQuantity() <= 0) throw new BizException(400, 400, "数量必须大于0");
        if (materialMapper.selectById(request.getMaterialId()) == null) throw new BizException(404, 404, "物资不存在");

        BigDecimal totalAmount = request.getTotalAmount();
        if (totalAmount == null) {
            BigDecimal unitPrice = request.getUnitPrice() == null ? BigDecimal.ZERO : request.getUnitPrice();
            totalAmount = unitPrice.multiply(new BigDecimal(request.getQuantity())).setScale(2, RoundingMode.HALF_UP);
        }

        InventoryIn in = new InventoryIn();
        in.setMaterialId(request.getMaterialId());
        in.setSupplier(request.getSupplier());
        in.setPurchaseOrderNo(request.getPurchaseOrderNo());
        in.setQuantity(request.getQuantity());
        in.setUnitPrice(request.getUnitPrice());
        in.setTotalAmount(totalAmount);
        in.setInDate(request.getInDate() == null ? LocalDate.now() : request.getInDate());
        in.setOperatorId(operatorId);
        in.setAttachmentUrl(request.getAttachmentUrl());
        in.setRemark(request.getRemark());
        inventoryInMapper.insert(in);

        Stock stock = stockMapper.selectByMaterialIdForUpdate(request.getMaterialId());
        if (stock == null) {
            Stock s = new Stock();
            s.setMaterialId(request.getMaterialId());
            s.setQuantity(request.getQuantity());
            s.setTotalValue(totalAmount);
            stockMapper.insert(s);
        } else {
            int oldQty = stock.getQuantity() == null ? 0 : stock.getQuantity();
            BigDecimal oldValue = stock.getTotalValue() == null ? BigDecimal.ZERO : stock.getTotalValue();
            stock.setQuantity(oldQty + request.getQuantity());
            stock.setTotalValue(oldValue.add(totalAmount));
            stockMapper.updateById(stock);
        }

        return in.getId();
    }

    private Map<Long, String> loadMaterialNames(List<Long> materialIds) {
        Map<Long, String> map = new HashMap<>();
        if (materialIds == null || materialIds.isEmpty()) return map;
        List<Long> ids = materialIds.stream().distinct().filter(x -> x != null).collect(Collectors.toList());
        if (ids.isEmpty()) return map;
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, name FROM t_material WHERE deleted = 0 AND id IN (" + in + ")";
        jdbcTemplate.query(sql, (RowCallbackHandler) rs -> map.put(rs.getLong("id"), rs.getString("name")), ids.toArray());
        return map;
    }

    private InventoryInVO toVO(InventoryIn in, String materialName) {
        InventoryInVO vo = new InventoryInVO();
        vo.setId(in.getId());
        vo.setMaterialId(in.getMaterialId());
        vo.setMaterialName(materialName);
        vo.setSupplier(in.getSupplier());
        vo.setPurchaseOrderNo(in.getPurchaseOrderNo());
        vo.setQuantity(in.getQuantity());
        vo.setUnitPrice(in.getUnitPrice());
        vo.setTotalAmount(in.getTotalAmount());
        vo.setInDate(in.getInDate());
        vo.setOperatorId(in.getOperatorId());
        vo.setAttachmentUrl(in.getAttachmentUrl());
        vo.setRemark(in.getRemark());
        vo.setCreateTime(in.getCreateTime());
        return vo;
    }
}
