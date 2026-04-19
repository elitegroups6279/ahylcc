package com.hfnew.dto.home;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HomeServiceOrderCreateRequest {
    private Long elderlyId;
    private Long serviceItemId;
    private LocalDateTime expectedTime;
    private String address;
    private String specialNote;
    private Long assignedStaffId;
}
