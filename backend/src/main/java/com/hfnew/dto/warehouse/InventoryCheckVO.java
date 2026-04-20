package com.hfnew.dto.warehouse;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InventoryCheckVO {
    private Long id;
    private Long materialId;
    private String materialName;
    private Integer systemQuantity;
    private Integer actualQuantity;
    private Integer difference;
    private LocalDate checkDate;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}
