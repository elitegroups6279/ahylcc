package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReconciliationReportVO {
    private Long accountId;
    private String accountName;
    private String accountType;
    private String month;
    private BigDecimal openingBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal closingBalance;
    private List<BankTransactionVO> transactions;
}
