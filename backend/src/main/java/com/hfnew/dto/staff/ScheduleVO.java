package com.hfnew.dto.staff;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleVO {
    private Long id;
    private Long staffId;
    private String staffName;
    private LocalDate scheduleDate;
    private String shiftType;
    private String remark;
    private LocalDateTime createTime;
}

