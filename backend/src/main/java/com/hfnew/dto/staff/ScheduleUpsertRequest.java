package com.hfnew.dto.staff;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ScheduleUpsertRequest {
    private Long staffId;
    private LocalDate scheduleDate;
    private String shiftType;
    private String remark;
}

