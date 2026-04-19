package com.hfnew.dto.finance;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseCreateRequest {
    private String expenseType;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String payee;
    private String description;
    private String remark;
}
