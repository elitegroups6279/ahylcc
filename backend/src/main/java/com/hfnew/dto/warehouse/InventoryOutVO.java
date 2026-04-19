package com.hfnew.dto.warehouse;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InventoryOutVO {
    private Long id;
    private Long materialId;
    private String materialName;
    private String department;
    private String purpose;
    private Integer quantity;
    private Long operatorId;
    private LocalDate outDate;
    private String status;
    private String remark;
    private LocalDateTime createTime;
}
