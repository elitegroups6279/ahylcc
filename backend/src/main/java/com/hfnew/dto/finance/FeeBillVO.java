package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FeeBillVO {
    private Long id;
    private Long elderlyId;
    private String elderlyName;
    private String billMonth;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private Integer stayDays;
    private Integer leaveDays;
    private String billingRule;
    private BigDecimal baseFee;
    private BigDecimal longCareAmount;
    private BigDecimal couponDeduct;
    private BigDecimal subsidyAmount;
    private BigDecimal personalSubsidy;
    private BigDecimal familyPayable;
    private BigDecimal govPayable;
    private String subsidyDetail;
    private String status;
    private LocalDateTime createTime;
}
