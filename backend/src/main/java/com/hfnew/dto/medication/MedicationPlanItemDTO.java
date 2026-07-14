package com.hfnew.dto.medication;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MedicationPlanItemDTO {

    private Long id;
    private Long drugId;
    private String drugName;
    private String dosage;
    private String dosageUnit;
    private BigDecimal totalQuantity;
    private String dosagePerTime;
    private LocalDate depletionDate;
}
