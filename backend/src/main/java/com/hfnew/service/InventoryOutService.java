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
import com.hfnew.util.BatchNameLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryOutService {

    private final InventoryOutMapper inventoryOutMapper;
    private final StockMapper stockMapper;
    private final MaterialMapper materialMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<InventoryOutVO> list(int page, int pageSize, String supplyCategory) {
        Page<InventoryOut> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<InventoryOut> wrapper = new LambdaQueryWrapper<>();
        if (supplyCategory != null && !supplyCategory.isBlank()) {
            wrapper.eq(InventoryOut::getSupplyCategory, supplyCategory);
        }
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

        String supplyCategory = request.getSupplyCategory() != null ? request.getSupplyCategory() : "SOCIAL";
        Stock stock = stockMapper.selectByMaterialAndCategoryForUpdate(request.getMaterialId(), supplyCategory);
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
        out.setSpecification(request.getSpecification());
        out.setSupplyCategory(supplyCategory);
        out.setRecipientStaffId(request.getRecipientStaffId());
        out.setRecipientName(request.getRecipientName());
        out.setRecipientSignUrl(request.getRecipientSignUrl());
        out.setRemark(request.getRemark());
        inventoryOutMapper.insert(out);

        return out.getId();
    }

    private Map<Long, String> loadMaterialNames(List<Long> materialIds) {
        Set<Long> ids = materialIds == null ? Set.of() : materialIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        return BatchNameLoader.loadNames(jdbcTemplate, "t_material", "id", "name", ids);
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
        vo.setSpecification(out.getSpecification());
        vo.setSupplyCategory(out.getSupplyCategory());
        vo.setRecipientStaffId(out.getRecipientStaffId());
        vo.setRecipientName(out.getRecipientName());
        vo.setRecipientSignUrl(out.getRecipientSignUrl());
        vo.setRemark(out.getRemark());
        vo.setCreateTime(out.getCreateTime());
        return vo;
    }

    @Transactional
    public void update(Long id, InventoryOutCreateRequest request) {
        InventoryOut out = inventoryOutMapper.selectById(id);
        if (out == null) throw new BizException(404, 404, "出库记录不存在");

        // Capture old values for stock sync BEFORE applying changes
        Long oldMaterialId = out.getMaterialId();
        String oldSupplyCategory = out.getSupplyCategory() != null ? out.getSupplyCategory() : "SOCIAL";
        int oldQty = out.getQuantity() == null ? 0 : out.getQuantity();

        // Apply updates to the entity
        if (request.getMaterialId() != null) out.setMaterialId(request.getMaterialId());
        if (request.getDepartment() != null) out.setDepartment(request.getDepartment());
        if (request.getPurpose() != null) out.setPurpose(request.getPurpose());
        if (request.getQuantity() != null) out.setQuantity(request.getQuantity());
        if (request.getSpecification() != null) out.setSpecification(request.getSpecification());
        if (request.getSupplyCategory() != null) out.setSupplyCategory(request.getSupplyCategory());
        if (request.getRecipientStaffId() != null) out.setRecipientStaffId(request.getRecipientStaffId());
        if (request.getRecipientName() != null) out.setRecipientName(request.getRecipientName());
        if (request.getRecipientSignUrl() != null) out.setRecipientSignUrl(request.getRecipientSignUrl());
        if (request.getOutDate() != null) out.setOutDate(request.getOutDate());
        if (request.getRemark() != null) out.setRemark(request.getRemark());
        inventoryOutMapper.updateById(out);

        // Sync stock: outbound reduces stock
        Long newMaterialId = out.getMaterialId();
        String newSupplyCategory = out.getSupplyCategory() != null ? out.getSupplyCategory() : "SOCIAL";
        int newQty = out.getQuantity() == null ? 0 : out.getQuantity();

        boolean materialChanged = !Objects.equals(oldMaterialId, newMaterialId)
                || !Objects.equals(oldSupplyCategory, newSupplyCategory);

        if (materialChanged) {
            // Return old quantity to old stock
            Stock oldStock = stockMapper.selectByMaterialAndCategoryForUpdate(oldMaterialId, oldSupplyCategory);
            if (oldStock != null) {
                int existingQty = oldStock.getQuantity() == null ? 0 : oldStock.getQuantity();
                oldStock.setQuantity(existingQty + oldQty);
                stockMapper.updateById(oldStock);
            }
            // Subtract new quantity from new stock
            Stock newStock = stockMapper.selectByMaterialAndCategoryForUpdate(newMaterialId, newSupplyCategory);
            if (newStock != null) {
                int existingQty = newStock.getQuantity() == null ? 0 : newStock.getQuantity();
                int remainQty = existingQty - newQty;
                if (remainQty < 0) {
                    throw new BizException(400, 400, "库存不足");
                }
                newStock.setQuantity(remainQty);
                stockMapper.updateById(newStock);
            }
        } else {
            // Same material/category - adjust by diff (more outbound = less stock)
            int diff = newQty - oldQty;
            if (diff != 0) {
                Stock stock = stockMapper.selectByMaterialAndCategoryForUpdate(newMaterialId, newSupplyCategory);
                if (stock != null) {
                    int existingQty = stock.getQuantity() == null ? 0 : stock.getQuantity();
                    int remainQty = existingQty - diff;
                    if (remainQty < 0) {
                        throw new BizException(400, 400, "库存不足");
                    }
                    stock.setQuantity(remainQty);
                    stockMapper.updateById(stock);
                }
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        InventoryOut out = inventoryOutMapper.selectById(id);
        if (out == null) throw new BizException(404, 404, "出库记录不存在");

        // Return quantity to stock BEFORE deleting
        Long materialId = out.getMaterialId();
        String supplyCategory = out.getSupplyCategory() != null ? out.getSupplyCategory() : "SOCIAL";
        int qty = out.getQuantity() == null ? 0 : out.getQuantity();

        Stock stock = stockMapper.selectByMaterialAndCategoryForUpdate(materialId, supplyCategory);
        if (stock != null) {
            int existingQty = stock.getQuantity() == null ? 0 : stock.getQuantity();
            stock.setQuantity(existingQty + qty);
            stockMapper.updateById(stock);
        }

        inventoryOutMapper.deleteById(id);
    }
}
