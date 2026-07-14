package com.hfnew.dto.medication;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MedicationPlanCreateRequest {

    private Long elderlyId;
    private String frequencyType;
    private Integer intervalDays;
    private List<String> timeSlots;
    private LocalDate startDate;
    private String instructions;
    private String prescriberName;
    private String remark;
    private List<MedicationPlanItemDTO> items;
}
