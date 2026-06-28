package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.warehouse.InventoryInCreateRequest;
import com.hfnew.dto.warehouse.InventoryInVO;
import com.hfnew.entity.Budget;
import com.hfnew.entity.ExpenseRecord;
import com.hfnew.entity.InventoryIn;
import com.hfnew.entity.Material;
import com.hfnew.entity.Stock;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.BudgetMapper;
import com.hfnew.mapper.ExpenseRecordMapper;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryInService {

    private final InventoryInMapper inventoryInMapper;
    private final StockMapper stockMapper;
    private final MaterialMapper materialMapper;
    private final ExpenseRecordMapper expenseRecordMapper;
    private final BankAccountService bankAccountService;
    private final BudgetService budgetService;
    private final BudgetMapper budgetMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<InventoryInVO> list(int page, int pageSize, String supplyCategory) {
        Page<InventoryIn> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<InventoryIn> wrapper = new LambdaQueryWrapper<>();
        if (supplyCategory != null && !supplyCategory.isBlank()) {
            wrapper.eq(InventoryIn::getSupplyCategory, supplyCategory);
        }
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
        in.setRemark(request.getRemark());
        in.setSupplyCategory(request.getSupplyCategory() != null ? request.getSupplyCategory() : "SOCIAL");
        in.setAllocationId(request.getAllocationId());
        inventoryInMapper.insert(in);

        if (in.getAllocationId() != null && in.getTotalAmount() != null) {
            // Update budget used_amount
            Budget budget = budgetService.getCurrentBudget(in.getSupplyCategory());
            if (budget != null) {
                budget.setUsedAmount(budget.getUsedAmount().add(in.getTotalAmount()));
                budgetMapper.updateById(budget);
            }
        }

        Stock stock = stockMapper.selectByMaterialAndCategoryForUpdate(request.getMaterialId(), in.getSupplyCategory());
        if (stock == null) {
            Stock s = new Stock();
            s.setMaterialId(request.getMaterialId());
            s.setSupplyCategory(in.getSupplyCategory());
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

        // Sync expense record if requested
        if (Boolean.TRUE.equals(request.getSyncExpense())) {
            Material material = materialMapper.selectById(request.getMaterialId());
            String materialName = material != null ? material.getName() : "未知物资";

            ExpenseRecord expense = new ExpenseRecord();
            expense.setExpenseType("SUPPLIES");
            expense.setSupplyCategory(in.getSupplyCategory());
            expense.setAmount(totalAmount);
            expense.setExpenseDate(in.getInDate());
            expense.setPayee(in.getSupplier());
            expense.setDescription("采购入库-" + materialName + " x" + in.getQuantity());
            expense.setBankAccountId(request.getBankAccountId());
            expense.setOperatorId(operatorId);
            expenseRecordMapper.insert(expense);

            // Link back to inventory-in
            in.setExpenseRecordId(expense.getId());
            inventoryInMapper.updateById(in);

            // Record bank transaction
            bankAccountService.recordExpenseTransaction(
                    expense.getId(), expense.getAmount(), "SUPPLIES",
                    expense.getDescription(), request.getBankAccountId());
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
        vo.setSupplyCategory(in.getSupplyCategory());
        vo.setAllocationId(in.getAllocationId());
        vo.setCreateTime(in.getCreateTime());
        return vo;
    }

    @Transactional
    public void update(Long id, InventoryInCreateRequest request) {
        InventoryIn in = inventoryInMapper.selectById(id);
        if (in == null) throw new BizException(404, 404, "入库记录不存在");

        // Capture old values for stock sync BEFORE applying changes
        Long oldMaterialId = in.getMaterialId();
        String oldSupplyCategory = in.getSupplyCategory() != null ? in.getSupplyCategory() : "SOCIAL";
        int oldQty = in.getQuantity() == null ? 0 : in.getQuantity();
        BigDecimal oldTotalAmount = in.getTotalAmount() == null ? BigDecimal.ZERO : in.getTotalAmount();

        // Apply updates to the entity
        if (request.getMaterialId() != null) in.setMaterialId(request.getMaterialId());
        if (request.getSupplier() != null) in.setSupplier(request.getSupplier());
        if (request.getPurchaseOrderNo() != null) in.setPurchaseOrderNo(request.getPurchaseOrderNo());
        if (request.getQuantity() != null) in.setQuantity(request.getQuantity());
        if (request.getUnitPrice() != null) in.setUnitPrice(request.getUnitPrice());
        if (request.getTotalAmount() != null) in.setTotalAmount(request.getTotalAmount());
        if (request.getInDate() != null) in.setInDate(request.getInDate());
        if (request.getRemark() != null) in.setRemark(request.getRemark());
        if (request.getSupplyCategory() != null) in.setSupplyCategory(request.getSupplyCategory());
        if (request.getAllocationId() != null) in.setAllocationId(request.getAllocationId());
        inventoryInMapper.updateById(in);

        // Sync stock: compute new values after update applied
        Long newMaterialId = in.getMaterialId();
        String newSupplyCategory = in.getSupplyCategory() != null ? in.getSupplyCategory() : "SOCIAL";
        int newQty = in.getQuantity() == null ? 0 : in.getQuantity();
        BigDecimal newTotalAmount = in.getTotalAmount() == null ? BigDecimal.ZERO : in.getTotalAmount();

        boolean materialChanged = !Objects.equals(oldMaterialId, newMaterialId)
                || !Objects.equals(oldSupplyCategory, newSupplyCategory);

        if (materialChanged) {
            // Subtract old quantity/value from old stock
            Stock oldStock = stockMapper.selectByMaterialAndCategoryForUpdate(oldMaterialId, oldSupplyCategory);
            if (oldStock != null) {
                int existingQty = oldStock.getQuantity() == null ? 0 : oldStock.getQuantity();
                BigDecimal existingValue = oldStock.getTotalValue() == null ? BigDecimal.ZERO : oldStock.getTotalValue();
                int remainQty = existingQty - oldQty;
                if (remainQty < 0) remainQty = 0;
                BigDecimal remainValue = existingValue.subtract(oldTotalAmount);
                if (remainValue.compareTo(BigDecimal.ZERO) < 0) remainValue = BigDecimal.ZERO;
                oldStock.setQuantity(remainQty);
                oldStock.setTotalValue(remainValue);
                stockMapper.updateById(oldStock);
            }
            // Add new quantity/value to new stock
            Stock newStock = stockMapper.selectByMaterialAndCategoryForUpdate(newMaterialId, newSupplyCategory);
            if (newStock == null) {
                Stock s = new Stock();
                s.setMaterialId(newMaterialId);
                s.setSupplyCategory(newSupplyCategory);
                s.setQuantity(newQty);
                s.setTotalValue(newTotalAmount);
                stockMapper.insert(s);
            } else {
                int existingQty = newStock.getQuantity() == null ? 0 : newStock.getQuantity();
                BigDecimal existingValue = newStock.getTotalValue() == null ? BigDecimal.ZERO : newStock.getTotalValue();
                newStock.setQuantity(existingQty + newQty);
                newStock.setTotalValue(existingValue.add(newTotalAmount));
                stockMapper.updateById(newStock);
            }
        } else {
            // Same material/category - adjust by diff
            int diff = newQty - oldQty;
            BigDecimal diffAmount = newTotalAmount.subtract(oldTotalAmount);
            if (diff != 0 || diffAmount.compareTo(BigDecimal.ZERO) != 0) {
                Stock stock = stockMapper.selectByMaterialAndCategoryForUpdate(newMaterialId, newSupplyCategory);
                if (stock != null) {
                    int existingQty = stock.getQuantity() == null ? 0 : stock.getQuantity();
                    BigDecimal existingValue = stock.getTotalValue() == null ? BigDecimal.ZERO : stock.getTotalValue();
                    int updatedQty = existingQty + diff;
                    if (updatedQty < 0) updatedQty = 0;
                    BigDecimal updatedValue = existingValue.add(diffAmount);
                    if (updatedValue.compareTo(BigDecimal.ZERO) < 0) updatedValue = BigDecimal.ZERO;
                    stock.setQuantity(updatedQty);
                    stock.setTotalValue(updatedValue);
                    stockMapper.updateById(stock);
                }
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        InventoryIn in = inventoryInMapper.selectById(id);
        if (in == null) throw new BizException(404, 404, "入库记录不存在");

        // Subtract quantity/value from stock BEFORE deleting
        Long materialId = in.getMaterialId();
        String supplyCategory = in.getSupplyCategory() != null ? in.getSupplyCategory() : "SOCIAL";
        int qty = in.getQuantity() == null ? 0 : in.getQuantity();
        BigDecimal totalAmount = in.getTotalAmount() == null ? BigDecimal.ZERO : in.getTotalAmount();

        Stock stock = stockMapper.selectByMaterialAndCategoryForUpdate(materialId, supplyCategory);
        if (stock != null) {
            int existingQty = stock.getQuantity() == null ? 0 : stock.getQuantity();
            BigDecimal existingValue = stock.getTotalValue() == null ? BigDecimal.ZERO : stock.getTotalValue();
            int remainQty = existingQty - qty;
            if (remainQty < 0) remainQty = 0;
            BigDecimal remainValue = existingValue.subtract(totalAmount);
            if (remainValue.compareTo(BigDecimal.ZERO) < 0) remainValue = BigDecimal.ZERO;
            stock.setQuantity(remainQty);
            stock.setTotalValue(remainValue);
            stockMapper.updateById(stock);
        }

        inventoryInMapper.deleteById(id);
    }
}
