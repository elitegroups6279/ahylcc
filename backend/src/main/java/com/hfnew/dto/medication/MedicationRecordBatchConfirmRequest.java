package com.hfnew.dto.medication;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MedicationRecordBatchConfirmRequest {

    private List<Long> recordIds;
    private Long executorId;
    private String executorName;
    private LocalDate date;
}
