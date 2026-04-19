package com.hfnew.dto.home;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HomeServiceOrderVO {
    private Long id;
    private Long elderlyId;
    private String elderlyName;
    private Long serviceItemId;
    private String serviceItemName;
    private LocalDateTime expectedTime;
    private String address;
    private String specialNote;
    private Long assignedStaffId;
    private String assignedStaffName;
    private String status;
    private LocalDateTime createTime;
}
