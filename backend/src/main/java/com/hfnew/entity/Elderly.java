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
@TableName("t_elderly")
public class Elderly {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String uniqueNo;

    private String name;

    private String idCard;

    private Integer gender;

    private LocalDate birthDate;

    private Integer age;

    private String region;

    private String ethnicity;

    private String photoUrl;

    private LocalDate admissionDate;

    private Long bedId;

    private String category;

    private String careLevel;

    private String disabilityLevel; // INTACT(能力完好), MILD(轻度失能), MODERATE(中度失能), SEVERE(重度失能), TOTAL(完全失能); legacy: SELF_CARE

    private Integer enableLongCare;

    private Integer enableCoupon;

    private String nursingNeeds;

    private String medicalHistory;

    private String hospital;

    private String doctor;

    private String regularMedication;

    private BigDecimal contractMonthlyFee;

    private BigDecimal deposit;

    private LocalDate contractStartDate;

    private Integer contractMonths;

    private String contractAttachmentUrl;

    private String paymentMethod;

    private String bankAccount;

    private String status;

    private LocalDate dischargeDate;

    private String dischargeReason;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
