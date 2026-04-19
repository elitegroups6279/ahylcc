package com.hfnew.dto.warehouse;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryOutCreateRequest {
    private Long materialId;
    private String department;
    private String purpose;
    private Integer quantity;
    private LocalDate outDate;
    private String remark;
}
