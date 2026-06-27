package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BankTransactionVO {
    private Long id;
    private Long bankAccountId;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String counterparty;
    private LocalDate transactionDate;
    private String bizType;
    private Long bizId;
    private String description;
    private String receiptNo;
    private Long operatorId;
    private LocalDateTime createTime;
}
