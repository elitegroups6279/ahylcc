package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BankDashboardVO {
    private Long basicAccountId;
    private String basicAccountName;
    private BigDecimal basicBalance;

    private Long generalAccountId;
    private String generalAccountName;
    private BigDecimal generalBalance;

    private BigDecimal basicMonthlyIncome;
    private BigDecimal basicMonthlyExpense;

    private BigDecimal generalMonthlyIncome;
    private BigDecimal generalMonthlyExpense;
}
