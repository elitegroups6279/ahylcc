package com.hfnew.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BankTransactionCreateRequest {
    @NotBlank(message = "交易类型不能为空")
    private String transactionType;

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    private String counterparty;

    @NotNull(message = "交易日期不能为空")
    private LocalDate transactionDate;

    private String bizType;
    private Long bizId;
    private String description;
    private String receiptNo;
}
