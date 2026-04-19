package com.hfnew.dto.subsidy;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SubsidyPolicyRequest {
    private String policyCode;
    private String policyName;
    private String category;        // SOCIAL/WU_BAO/LOW_BAO/ALL
    private String disabilityLevel; // null/MODERATE/SEVERE
    private String calcType;        // FIXED_MONTHLY/DAILY_RATE/THRESHOLD_DEDUCT
    private BigDecimal amount;
    private BigDecimal thresholdAmount;
    private BigDecimal deductAmount;
    private String payTarget;       // ORG/PERSONAL
    private Integer minStayDays;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private String remark;
}
