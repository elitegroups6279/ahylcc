package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.warehouse.InventoryOutCreateRequest;
import com.hfnew.dto.warehouse.InventoryOutVO;
import com.hfnew.entity.InventoryOut;
import com.hfnew.entity.Material;
import com.hfnew.entity.Stock;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.InventoryOutMapper;
import com.hfnew.mapper.MaterialMapper;
import com.hfnew.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
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
public class InventoryOutService {

    private final InventoryOutMapper inventoryOutMapper;
    private final StockMapper stockMapper;
    private final MaterialMapper materialMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<InventoryOutVO> list(int page, int pageSize) {
        Page<InventoryOut> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<InventoryOut> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(InventoryOut::getCreateTime).orderByDesc(InventoryOut::getId);
        IPage<InventoryOut> result = inventoryOutMapper.selectPage(pageReq, wrapper);

        Map<Long, String> nameMap = loadMaterialNames(result.getRecords().stream().map(InventoryOut::getMaterialId).collect(Collectors.toList()));
        List<InventoryOutVO> list = result.getRecords().stream().map(r -> toVO(r, nameMap.get(r.getMaterialId()))).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long create(Long operatorId, InventoryOutCreateRequest request) {
        if (request.getMaterialId() == null) throw new BizException(400, 400, "请选择物资");
        if (request.getQuantity() == null || request.getQuantity() <= 0) throw new BizException(400, 400, "数量必须大于0");
        Material m = materialMapper.selectById(request.getMaterialId());
        if (m == null) throw new BizException(404, 404, "物资不存在");

        Stock stock = stockMapper.selectByMaterialIdForUpdate(request.getMaterialId());
        if (stock == null || stock.getQuantity() == null || stock.getQuantity() < request.getQuantity()) {
            throw new BizException(400, 400, "库存不足");
        }

        int qtyBefore = stock.getQuantity();
        BigDecimal totalValueBefore = stock.getTotalValue() == null ? BigDecimal.ZERO : stock.getTotalValue();
        BigDecimal avg = qtyBefore > 0 ? totalValueBefore.divide(new BigDecimal(qtyBefore), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal deductedValue = avg.multiply(new BigDecimal(request.getQuantity())).setScale(2, RoundingMode.HALF_UP);

        stock.setQuantity(qtyBefore - request.getQuantity());
        BigDecimal newValue = totalValueBefore.subtract(deductedValue);
        if (stock.getQuantity() <= 0) {
            stock.setQuantity(0);
            stock.setTotalValue(BigDecimal.ZERO);
        } else {
            stock.setTotalValue(newValue.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newValue);
        }
        stockMapper.updateById(stock);

        InventoryOut out = new InventoryOut();
        out.setMaterialId(request.getMaterialId());
        out.setDepartment(request.getDepartment());
        out.setPurpose(request.getPurpose());
        out.setQuantity(request.getQuantity());
        out.setOperatorId(operatorId);
        out.setOutDate(request.getOutDate() == null ? LocalDate.now() : request.getOutDate());
        out.setStatus("APPROVED");
        out.setRemark(request.getRemark());
        inventoryOutMapper.insert(out);

        return out.getId();
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

    private InventoryOutVO toVO(InventoryOut out, String materialName) {
        InventoryOutVO vo = new InventoryOutVO();
        vo.setId(out.getId());
        vo.setMaterialId(out.getMaterialId());
        vo.setMaterialName(materialName);
        vo.setDepartment(out.getDepartment());
        vo.setPurpose(out.getPurpose());
        vo.setQuantity(out.getQuantity());
        vo.setOperatorId(out.getOperatorId());
        vo.setOutDate(out.getOutDate());
        vo.setStatus(out.getStatus());
        vo.setRemark(out.getRemark());
        vo.setCreateTime(out.getCreateTime());
        return vo;
    }
}
