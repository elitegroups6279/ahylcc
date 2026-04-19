package com.hfnew.dto.staff;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AttendanceUpsertRequest {
    private Long staffId;
    private LocalDate attendanceDate;
    private LocalDateTime clockInTime;
    private LocalDateTime clockOutTime;
    private String status;
    private String remark;
}
