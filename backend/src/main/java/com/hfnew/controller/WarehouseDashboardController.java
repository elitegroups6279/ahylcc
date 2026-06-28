package com.hfnew.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hfnew.common.ApiResponse;
import com.hfnew.entity.*;
import com.hfnew.mapper.*;
import com.hfnew.service.BudgetService;
import com.hfnew.service.InventoryBatchService;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/warehouse/dashboard")
@RequiredArgsConstructor
public class WarehouseDashboardController {

    private final WubaoAllocationMapper wubaoAllocationMapper;
    private final InventoryInMapper inventoryInMapper;
    private final InventoryOutMapper inventoryOutMapper;
    private final MaterialMapper materialMapper;
    private final BudgetService budgetService;
    private final InventoryBatchService inventoryBatchService;

    @Data
    public static class FundSummary {
        private BigDecimal monthlyAllocation = BigDecimal.ZERO;
        private BigDecimal purchasedAmount = BigDecimal.ZERO;
        private BigDecimal issuedAmount = BigDecimal.ZERO;
        private BigDecimal remainingBalance = BigDecimal.ZERO;
    }

    @Data
    public static class Alert {
        private String type;
        private String message;
        private String level;
        public Alert(String type, String message, String level) {
            this.type = type;
            this.message = message;
            this.level = level;
        }
    }

    @Data
    public static class ConsumptionTrendItem {
        private String month;
        private String category;
        private Integer totalQuantity = 0;
        private BigDecimal totalAmount = BigDecimal.ZERO;
    }

    @GetMapping("/fund-summary")
    @PreAuthorize("hasAuthority('warehouse:stock')")
    public ResponseEntity<ApiResponse<FundSummary>> getFundSummary() {
        LocalDate now = LocalDate.now();
        YearMonth ym = YearMonth.of(now.getYear(), now.getMonthValue());
        String monthStr = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        FundSummary summary = new FundSummary();

        // Monthly allocation from t_wubao_allocation
        LambdaQueryWrapper<WubaoAllocation> allocWrapper = new LambdaQueryWrapper<>();
        allocWrapper.eq(WubaoAllocation::getAllocateMonth, monthStr);
        List<WubaoAllocation> allocs = wubaoAllocationMapper.selectList(allocWrapper);
        BigDecimal allocation = allocs.stream()
                .map(a -> a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setMonthlyAllocation(allocation);

        // Purchased amount: sum totalAmount from inventory-in where supplyCategory=CENTRALIZED this month
        LambdaQueryWrapper<InventoryIn> inWrapper = new LambdaQueryWrapper<>();
        inWrapper.ge(InventoryIn::getInDate, monthStart)
                 .le(InventoryIn::getInDate, monthEnd)
                 .eq(InventoryIn::getSupplyCategory, "CENTRALIZED");
        List<InventoryIn> ins = inventoryInMapper.selectList(inWrapper);
        BigDecimal purchased = ins.stream()
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setPurchasedAmount(purchased);

        // Issued amount: estimate from inventory-out where supplyCategory=CENTRALIZED this month
        LambdaQueryWrapper<InventoryOut> outWrapper = new LambdaQueryWrapper<>();
        outWrapper.ge(InventoryOut::getOutDate, monthStart)
                  .le(InventoryOut::getOutDate, monthEnd)
                  .eq(InventoryOut::getSupplyCategory, "CENTRALIZED");
        List<InventoryOut> outs = inventoryOutMapper.selectList(outWrapper);

        // Load material names for display (no price on Material); use latest in record's unitPrice for estimation
        Set<Long> materialIds = outs.stream().map(InventoryOut::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BigDecimal> materialPrices = new HashMap<>();
        if (!materialIds.isEmpty()) {
            // Get latest unit price from inventory-in for each material
            for (Long matId : materialIds) {
                LambdaQueryWrapper<InventoryIn> priceWrapper = new LambdaQueryWrapper<>();
                priceWrapper.eq(InventoryIn::getMaterialId, matId)
                            .isNotNull(InventoryIn::getUnitPrice)
                            .orderByDesc(InventoryIn::getInDate)
                            .last("LIMIT 1");
                InventoryIn latest = inventoryInMapper.selectOne(priceWrapper);
                if (latest != null && latest.getUnitPrice() != null) {
                    materialPrices.put(matId, latest.getUnitPrice());
                }
            }
        }

        BigDecimal issued = BigDecimal.ZERO;
        for (InventoryOut out : outs) {
            BigDecimal price = materialPrices.getOrDefault(out.getMaterialId(), BigDecimal.ZERO);
            int qty = out.getQuantity() != null ? out.getQuantity() : 0;
            issued = issued.add(price.multiply(BigDecimal.valueOf(qty)));
        }
        summary.setIssuedAmount(issued);
        summary.setRemainingBalance(allocation.subtract(purchased));

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('warehouse:stock')")
    public ResponseEntity<ApiResponse<List<Alert>>> getAlerts() {
        List<Alert> alerts = new ArrayList<>();
        LocalDate now = LocalDate.now();
        YearMonth ym = YearMonth.of(now.getYear(), now.getMonthValue());
        String monthStr = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        // Fund usage check
        LambdaQueryWrapper<WubaoAllocation> allocWrapper = new LambdaQueryWrapper<>();
        allocWrapper.eq(WubaoAllocation::getAllocateMonth, monthStr);
        List<WubaoAllocation> allocs = wubaoAllocationMapper.selectList(allocWrapper);
        BigDecimal allocation = allocs.stream()
                .map(a -> a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LambdaQueryWrapper<InventoryIn> inWrapper = new LambdaQueryWrapper<>();
        inWrapper.ge(InventoryIn::getInDate, monthStart)
                 .le(InventoryIn::getInDate, monthEnd)
                 .eq(InventoryIn::getSupplyCategory, "CENTRALIZED");
        List<InventoryIn> ins = inventoryInMapper.selectList(inWrapper);
        BigDecimal purchased = ins.stream()
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (allocation.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = purchased.divide(allocation, 2, RoundingMode.HALF_UP);
            if (ratio.compareTo(BigDecimal.valueOf(0.8)) > 0) {
                int pct = ratio.multiply(BigDecimal.valueOf(100)).intValue();
                alerts.add(new Alert("FUND_USAGE_HIGH", "集中供养资金使用率已达" + pct + "%", "WARNING"));
            }
        }

        // Near expiry check
        List<InventoryBatch> nearExpiry = inventoryBatchService.getNearExpiry(30);
        if (!nearExpiry.isEmpty()) {
            alerts.add(new Alert("NEAR_EXPIRY", nearExpiry.size() + "种物资将在30天内过期", "WARNING"));
        }

        // Budget overrun check
        Budget budget = budgetService.getCurrentBudget("CENTRALIZED");
        if (budget != null && budget.getBudgetAmount() != null && budget.getUsedAmount() != null) {
            if (budget.getUsedAmount().compareTo(budget.getBudgetAmount()) > 0) {
                alerts.add(new Alert("BUDGET_OVERRUN", "本月集中供养采购已超预算", "DANGER"));
            }
        }

        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/consumption-trend")
    @PreAuthorize("hasAuthority('warehouse:stock')")
    public ResponseEntity<ApiResponse<List<ConsumptionTrendItem>>> getConsumptionTrend(
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) String supplyCategory
    ) {
        LocalDate now = LocalDate.now();
        YearMonth currentYm = YearMonth.of(now.getYear(), now.getMonthValue());
        YearMonth startYm = currentYm.minusMonths(months - 1);
        LocalDate startDate = startYm.atDay(1);
        LocalDate endDate = currentYm.atEndOfMonth();

        LambdaQueryWrapper<InventoryOut> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(InventoryOut::getOutDate, startDate)
               .le(InventoryOut::getOutDate, endDate);
        if (supplyCategory != null && !supplyCategory.isBlank()) {
            wrapper.eq(InventoryOut::getSupplyCategory, supplyCategory);
        }
        wrapper.orderByAsc(InventoryOut::getOutDate);
        List<InventoryOut> outs = inventoryOutMapper.selectList(wrapper);

        // Load latest unit prices for amount estimation
        Set<Long> materialIds = outs.stream().map(InventoryOut::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BigDecimal> materialPrices = new HashMap<>();
        if (!materialIds.isEmpty()) {
            for (Long matId : materialIds) {
                LambdaQueryWrapper<InventoryIn> priceWrapper = new LambdaQueryWrapper<>();
                priceWrapper.eq(InventoryIn::getMaterialId, matId)
                            .isNotNull(InventoryIn::getUnitPrice)
                            .orderByDesc(InventoryIn::getInDate)
                            .last("LIMIT 1");
                InventoryIn latest = inventoryInMapper.selectOne(priceWrapper);
                if (latest != null && latest.getUnitPrice() != null) {
                    materialPrices.put(matId, latest.getUnitPrice());
                }
            }
        }

        // Group by month+category
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, ConsumptionTrendItem> grouped = new LinkedHashMap<>();
        for (InventoryOut out : outs) {
            String month = out.getOutDate() != null ? out.getOutDate().format(monthFmt) : "unknown";
            String cat = out.getSupplyCategory() != null ? out.getSupplyCategory() : "UNKNOWN";
            String key = month + "|" + cat;
            ConsumptionTrendItem item = grouped.computeIfAbsent(key, k -> {
                ConsumptionTrendItem c = new ConsumptionTrendItem();
                c.setMonth(month);
                c.setCategory(cat);
                return c;
            });
            int qty = out.getQuantity() != null ? out.getQuantity() : 0;
            item.setTotalQuantity(item.getTotalQuantity() + qty);
            BigDecimal price = materialPrices.getOrDefault(out.getMaterialId(), BigDecimal.ZERO);
            item.setTotalAmount(item.getTotalAmount().add(price.multiply(BigDecimal.valueOf(qty))));
        }

        return ResponseEntity.ok(ApiResponse.success(new ArrayList<>(grouped.values())));
    }
}
