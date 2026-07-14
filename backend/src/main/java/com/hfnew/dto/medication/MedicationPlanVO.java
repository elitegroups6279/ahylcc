package com.hfnew.dto.medication;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MedicationPlanVO {

    private Long id;
    private Long elderlyId;
    private String elderlyName;
    private String frequencyType;
    private Integer intervalDays;
    private String timeSlots;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer timesPerDay;
    private String instructions;
    private String prescriberName;
    private String status;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long remainingDays;
    private List<MedicationPlanItemDTO> items;
}
