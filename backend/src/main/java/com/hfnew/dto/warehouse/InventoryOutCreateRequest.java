package com.hfnew.dto.warehouse;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryOutCreateRequest {
    private Long materialId;
    private String department;
    private String purpose;
    private Integer quantity;
    private String specification;
    private String supplyCategory;
    private LocalDate outDate;
    private Long recipientStaffId;
    private String recipientName;
    private String recipientSignUrl;
    private String remark;
}
