package com.hfnew.dto.medication;

import lombok.Data;

@Data
public class MedicationRecordActionRequest {

    private Long executorId;
    private String executorName;
    private String reason;
}
