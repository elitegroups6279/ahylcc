package com.hfnew.dto.staff;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AttendanceVO {
    private Long id;
    private Long staffId;
    private String staffName;
    private LocalDate attendanceDate;
    private LocalDateTime clockInTime;
    private LocalDateTime clockOutTime;
    private String status;
    private String remark;
    private LocalDateTime createTime;
}
