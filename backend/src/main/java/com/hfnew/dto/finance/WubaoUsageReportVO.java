package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class WubaoUsageReportVO {
    private String month;
    private BigDecimal totalAllocated;
    private BigDecimal totalExpensed;
    private BigDecimal netRemaining;
    private Integer elderCount;
    private List<ExpenseBreakdown> expenseBreakdown;

    @Data
    public static class ExpenseBreakdown {
        private String expenseType;
        private String supplyCategory;
        private BigDecimal amount;
        private Integer count;
    }
}
