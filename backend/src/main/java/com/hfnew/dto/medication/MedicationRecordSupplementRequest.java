package com.hfnew.dto.medication;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MedicationRecordSupplementRequest {

    private String supplementType; // CONFIRM, SKIP, REFUSE
    private Long executorId;
    private String executorName;
    private String reason;
}
