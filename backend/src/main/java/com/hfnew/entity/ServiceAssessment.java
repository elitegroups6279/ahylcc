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
@TableName("t_service_assessment")
public class ServiceAssessment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String assessmentNo;

    private LocalDate assessmentDate;

    private String assessorName;

    private String assessorOrg;

    private String assessmentPeriod;

    private Long elderlyId;

    private String elderlyName;

    private String serviceAddress;

    private Integer agreementSigned;

    private Integer agreementComplete;

    private Integer planFormulated;

    private Integer planMatchesNeeds;

    private Integer agreementScore;

    private Integer serviceOnTime;

    private Integer staffIdentified;

    private Integer riskInformed;

    private Integer servicePerPlan;

    private Integer emergencyHandled;

    private Integer acceptanceDone;

    private Integer fulfillmentScore;

    private Integer recordComplete;

    private Integer recordTimely;

    private Integer recordAccurate;

    private Integer recordScore;

    private Integer elderlySatisfaction;

    private String satisfactionMethod;

    private Integer totalScore;

    private String grade;

    private String issuesFound;

    private String improvementMeasures;

    private LocalDate improvementDeadline;

    private String photoUrls;

    private String assessorSignatureUrl;

    private String orgSignatureUrl;

    private String status;

    private Long creatorId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
