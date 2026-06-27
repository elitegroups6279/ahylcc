package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FundFlowSummaryVO {
    private List<MonthlyFlow> monthlyFlows;

    @Data
    public static class MonthlyFlow {
        private String month;
        private BigDecimal basicIncome;
        private BigDecimal basicExpense;
        private BigDecimal basicNet;
        private BigDecimal generalIncome;
        private BigDecimal generalExpense;
        private BigDecimal generalNet;
    }
}
