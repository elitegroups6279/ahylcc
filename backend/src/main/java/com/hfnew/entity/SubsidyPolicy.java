package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_subsidy_policy")
public class SubsidyPolicy {

    @TableId(type = IdType.AUTO)
    private Long id;

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

    private Integer enabled;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
