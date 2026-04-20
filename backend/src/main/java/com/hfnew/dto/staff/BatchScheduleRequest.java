package com.hfnew.dto.staff;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BatchScheduleRequest {
    private Long staffId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String shiftType;
}
