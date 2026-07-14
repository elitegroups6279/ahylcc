package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_medication_record")
public class MedicationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long elderlyId;

    private Long drugId;

    private String drugName;

    private String dosage;

    private LocalDate recordDate;

    private String timeSlot;

    private String status;

    private Long executorId;

    private String executorName;

    private LocalDateTime executedAt;

    private String skipReason;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long orgId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
