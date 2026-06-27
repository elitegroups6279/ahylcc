package com.hfnew.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.dto.finance.WarehouseCostSummaryVO;
import com.hfnew.dto.finance.WarehouseExpenseLinkVO;
import com.hfnew.entity.BankAccount;
import com.hfnew.entity.ExpenseRecord;
import com.hfnew.entity.InventoryIn;
import com.hfnew.mapper.BankAccountMapper;
import com.hfnew.mapper.ExpenseRecordMapper;
import com.hfnew.mapper.InventoryInMapper;
import com.hfnew.mapper.MaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class WarehouseFinanceController {

    private final InventoryInMapper inventoryInMapper;
    private final MaterialMapper materialMapper;
    private final BankAccountMapper bankAccountMapper;
    private final ExpenseRecordMapper expenseRecordMapper;

    /**
     * Query inventory-in records that have linked expense records.
     */
    @GetMapping("/warehouse-expense-link")
    @PreAuthorize("hasAuthority('finance:expense')")
    public ResponseEntity<ApiResponse<PageResult<WarehouseExpenseLinkVO>>> listExpenseLinks(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        LambdaQueryWrapper<InventoryIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(InventoryIn::getExpenseRecordId);
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(InventoryIn::getInDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(InventoryIn::getInDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(InventoryIn::getInDate).orderByDesc(InventoryIn::getId);

        Page<InventoryIn> pageReq = new Page<>(page, size);
        IPage<InventoryIn> result = inventoryInMapper.selectPage(pageReq, wrapper);

        // Load material names
        Set<Long> materialIds = result.getRecords().stream()
                .map(InventoryIn::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> materialNames = new HashMap<>();
        if (!materialIds.isEmpty()) {
            materialMapper.selectBatchIds(materialIds)
                    .forEach(m -> materialNames.put(m.getId(), m.getName()));
        }

        // Load expense records to get bankAccountId per expenseRecordId
        Set<Long> expenseIds = result.getRecords().stream()
                .map(InventoryIn::getExpenseRecordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Long> expenseBankAccountMap = new HashMap<>();
        if (!expenseIds.isEmpty()) {
            expenseRecordMapper.selectBatchIds(expenseIds)
                    .forEach(e -> expenseBankAccountMap.put(e.getId(), e.getBankAccountId()));
        }

        // Load bank account names
        Set<Long> bankAccountIds = expenseBankAccountMap.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> bankAccountNames = new HashMap<>();
        if (!bankAccountIds.isEmpty()) {
            bankAccountMapper.selectBatchIds(bankAccountIds)
                    .forEach(a -> bankAccountNames.put(a.getId(), a.getAccountName()));
        }

        List<WarehouseExpenseLinkVO> list = result.getRecords().stream().map(in -> {
            WarehouseExpenseLinkVO vo = new WarehouseExpenseLinkVO();
            vo.setInventoryInId(in.getId());
            vo.setMaterialName(materialNames.getOrDefault(in.getMaterialId(), "未知物资"));
            vo.setSupplier(in.getSupplier());
            vo.setQuantity(in.getQuantity());
            vo.setUnitPrice(in.getUnitPrice());
            vo.setTotalAmount(in.getTotalAmount());
            vo.setInDate(in.getInDate());
            vo.setExpenseRecordId(in.getExpenseRecordId());
            Long bankAccountId = expenseBankAccountMap.get(in.getExpenseRecordId());
            vo.setBankAccountId(bankAccountId);
            vo.setBankAccountName(bankAccountId != null ? bankAccountNames.getOrDefault(bankAccountId, "未知账户") : null);
            return vo;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list)));
    }

    /**
     * Warehouse cost summary by month, optionally filtered by category.
     */
    @GetMapping("/warehouse-cost-summary")
    @PreAuthorize("hasAuthority('finance:expense')")
    public ResponseEntity<ApiResponse<WarehouseCostSummaryVO>> getCostSummary(
            @RequestParam String month,
            @RequestParam(required = false) String category
    ) {
        YearMonth ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        LambdaQueryWrapper<InventoryIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(InventoryIn::getInDate, start)
               .le(InventoryIn::getInDate, end)
               .orderByAsc(InventoryIn::getId);
        List<InventoryIn> records = inventoryInMapper.selectList(wrapper);

        // Load material categories
        Set<Long> materialIds = records.stream()
                .map(InventoryIn::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> materialCategories = new HashMap<>();
        if (!materialIds.isEmpty()) {
            materialMapper.selectBatchIds(materialIds)
                    .forEach(m -> materialCategories.put(m.getId(),
                            m.getCategory() != null ? m.getCategory() : "未分类"));
        }

        // Group by category
        Map<String, List<InventoryIn>> grouped = new LinkedHashMap<>();
        for (InventoryIn in : records) {
            String cat = materialCategories.getOrDefault(in.getMaterialId(), "未分类");
            if (category != null && !category.isBlank() && !category.equals(cat)) {
                continue;
            }
            grouped.computeIfAbsent(cat, k -> new ArrayList<>()).add(in);
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        List<WarehouseCostSummaryVO.CategoryCost> breakdown = new ArrayList<>();
        for (Map.Entry<String, List<InventoryIn>> entry : grouped.entrySet()) {
            WarehouseCostSummaryVO.CategoryCost cc = new WarehouseCostSummaryVO.CategoryCost();
            cc.setCategory(entry.getKey());
            BigDecimal catAmount = entry.getValue().stream()
                    .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            cc.setAmount(catAmount);
            cc.setCount(entry.getValue().size());
            breakdown.add(cc);
            totalCost = totalCost.add(catAmount);
        }

        WarehouseCostSummaryVO vo = new WarehouseCostSummaryVO();
        vo.setMonth(month);
        vo.setTotalCost(totalCost);
        vo.setCategoryBreakdown(breakdown);

        return ResponseEntity.ok(ApiResponse.success(vo));
    }
}
