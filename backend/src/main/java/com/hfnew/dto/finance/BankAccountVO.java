package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BankAccountVO {
    private Long id;
    private String accountName;
    private String accountType;
    private String bankName;
    private String accountNumber;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private String status;
    private LocalDateTime createTime;
}
