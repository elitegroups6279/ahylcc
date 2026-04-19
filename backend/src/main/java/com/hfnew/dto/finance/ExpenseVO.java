package com.hfnew.dto.finance;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExpenseVO {
    private Long id;
    private String expenseType;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String payee;
    private String description;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createTime;
}
