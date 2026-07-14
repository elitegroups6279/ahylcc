package com.hfnew.dto.medication;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MedicationRecordVO {

    private Long id;
    private Long planId;
    private Long elderlyId;
    private String elderlyName;
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
