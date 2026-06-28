package com.hfnew.dto.warehouse;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetRequest {
    private Integer year;
    private Integer month;
    private String supplyCategory;
    private BigDecimal budgetAmount;
    private String remark;
}
