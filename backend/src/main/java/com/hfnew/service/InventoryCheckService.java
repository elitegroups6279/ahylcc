package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.warehouse.InventoryCheckCreateRequest;
import com.hfnew.dto.warehouse.InventoryCheckVO;
import com.hfnew.entity.InventoryCheck;
import com.hfnew.entity.Material;
import com.hfnew.entity.Stock;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.InventoryCheckMapper;
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
public class InventoryCheckService {

    private final InventoryCheckMapper inventoryCheckMapper;
    private final StockMapper stockMapper;
    private final MaterialMapper materialMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<InventoryCheckVO> list(int page, int pageSize) {
        Page<InventoryCheck> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<InventoryCheck> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(InventoryCheck::getCreateTime).orderByDesc(InventoryCheck::getId);
        IPage<InventoryCheck> result = inventoryCheckMapper.selectPage(pageReq, wrapper);

        Map<Long, String> nameMap = loadMaterialNames(result.getRecords().stream().map(InventoryCheck::getMaterialId).collect(Collectors.toList()));
        List<InventoryCheckVO> list = result.getRecords().stream().map(r -> toVO(r, nameMap.get(r.getMaterialId()))).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long create(Long operatorId, InventoryCheckCreateRequest request) {
        if (request.getMaterialId() == null) throw new BizException(400, 400, "请选择物资");
        if (request.getActualQuantity() == null || request.getActualQuantity() < 0) throw new BizException(400, 400, "实际数量不能小于0");
        Material m = materialMapper.selectById(request.getMaterialId());
        if (m == null) throw new BizException(404, 404, "物资不存在");

        Stock stock = stockMapper.selectByMaterialIdForUpdate(request.getMaterialId());
        int systemQty = stock == null || stock.getQuantity() == null ? 0 : stock.getQuantity();
        BigDecimal totalValueBefore = stock == null || stock.getTotalValue() == null ? BigDecimal.ZERO : stock.getTotalValue();
        BigDecimal avg = systemQty > 0 ? totalValueBefore.divide(new BigDecimal(systemQty), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        int actualQty = request.getActualQuantity();
        int diff = actualQty - systemQty;

        InventoryCheck ic = new InventoryCheck();
        ic.setMaterialId(request.getMaterialId());
        ic.setSystemQuantity(systemQty);
        ic.setActualQuantity(actualQty);
        ic.setDifference(diff);
        ic.setCheckDate(request.getCheckDate() == null ? LocalDate.now() : request.getCheckDate());
        ic.setOperatorId(operatorId);
        ic.setRemark(request.getRemark());
        inventoryCheckMapper.insert(ic);

        if (stock == null) {
            if (actualQty > 0) {
                Stock s = new Stock();
                s.setMaterialId(request.getMaterialId());
                s.setQuantity(actualQty);
                s.setTotalValue(avg.multiply(new BigDecimal(actualQty)).setScale(2, RoundingMode.HALF_UP));
                stockMapper.insert(s);
            }
        } else {
            stock.setQuantity(actualQty);
            BigDecimal newTotalValue = avg.multiply(new BigDecimal(actualQty)).setScale(2, RoundingMode.HALF_UP);
            stock.setTotalValue(actualQty <= 0 ? BigDecimal.ZERO : newTotalValue);
            stockMapper.updateById(stock);
        }

        return ic.getId();
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

    private InventoryCheckVO toVO(InventoryCheck r, String materialName) {
        InventoryCheckVO vo = new InventoryCheckVO();
        vo.setId(r.getId());
        vo.setMaterialId(r.getMaterialId());
        vo.setMaterialName(materialName);
        vo.setSystemQuantity(r.getSystemQuantity());
        vo.setActualQuantity(r.getActualQuantity());
        vo.setDifference(r.getDifference());
        vo.setCheckDate(r.getCheckDate());
        vo.setOperatorId(r.getOperatorId());
        vo.setRemark(r.getRemark());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}
