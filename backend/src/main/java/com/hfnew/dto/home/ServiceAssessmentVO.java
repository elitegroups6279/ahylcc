package com.hfnew.dto.home;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ServiceAssessmentVO {

    private Long id;

    private String assessmentNo;

    private LocalDate assessmentDate;

    private String assessorName;

    private String assessorOrg;

    private String assessmentPeriod;

    private Long elderlyId;

    private String elderlyName;

    private String serviceAddress;

    // 服务协议评分项
    private Integer agreementSigned;

    private Integer agreementComplete;

    private Integer planFormulated;

    private Integer planMatchesNeeds;

    private Integer agreementScore;

    // 服务履行评分项
    private Integer serviceOnTime;

    private Integer staffIdentified;

    private Integer riskInformed;

    private Integer servicePerPlan;

    private Integer emergencyHandled;

    private Integer acceptanceDone;

    private Integer fulfillmentScore;

    // 服务记录评分项
    private Integer recordComplete;

    private Integer recordTimely;

    private Integer recordAccurate;

    private Integer recordScore;

    // 满意度
    private Integer elderlySatisfaction;

    private String satisfactionMethod;

    // 总分和等级
    private Integer totalScore;

    private String grade;

    // 改进措施
    private String issuesFound;

    private String improvementMeasures;

    private LocalDate improvementDeadline;

    // 附件
    private List<String> photoUrls;

    private String assessorSignatureUrl;

    private String orgSignatureUrl;

    // 状态
    private String status;

    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
