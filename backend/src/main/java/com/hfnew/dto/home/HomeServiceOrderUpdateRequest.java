package com.hfnew.dto.home;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HomeServiceOrderUpdateRequest {
    private Long serviceItemId;
    private LocalDateTime expectedTime;
    private String address;
    private String specialNote;
    private Long assignedStaffId;
    private String status;
}
