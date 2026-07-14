package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_medication_plan_item")
public class MedicationPlanItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long drugId;

    private String drugName;

    private String dosage;

    private String dosageUnit;

    private BigDecimal totalQuantity;

    private String dosagePerTime;

    private LocalDate depletionDate;

    @TableField(fill = FieldFill.INSERT)
    private Long orgId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
