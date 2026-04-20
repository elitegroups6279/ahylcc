package com.hfnew.dto.notify;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeWarningItem {
    private Long elderlyId;
    private String name;
    private Integer remainingDays;
    private BigDecimal balance;
}
