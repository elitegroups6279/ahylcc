package com.hfnew.dto.warehouse;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryCheckCreateRequest {
    private Long materialId;
    private Integer actualQuantity;
    private LocalDate checkDate;
    private String remark;
}
