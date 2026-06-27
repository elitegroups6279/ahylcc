package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class WarehouseCostSummaryVO {
    private String month;
    private BigDecimal totalCost;
    private List<CategoryCost> categoryBreakdown;

    @Data
    public static class CategoryCost {
        private String category;
        private BigDecimal amount;
        /** number of purchases */
        private Integer count;
    }
}
