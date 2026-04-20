package com.hfnew.dto.subsidy;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SubsidyPolicyVO {
    private Long id;
    private String policyCode;
    private String policyName;
    private String category;
    private String disabilityLevel;
    private String calcType;
    private BigDecimal amount;
    private BigDecimal thresholdAmount;
    private BigDecimal deductAmount;
    private String payTarget;
    private Integer minStayDays;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer enabled;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
