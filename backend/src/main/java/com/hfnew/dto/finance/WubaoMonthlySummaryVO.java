package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WubaoMonthlySummaryVO {

    private String month;

    private Integer elderCount;

    private BigDecimal livingFeeTotal;

    private BigDecimal careFeeTotal;

    private BigDecimal totalAllocated;

    private BigDecimal totalExpensed; // 一般户当月支出

    private BigDecimal balance; // 一般户当前余额
}
