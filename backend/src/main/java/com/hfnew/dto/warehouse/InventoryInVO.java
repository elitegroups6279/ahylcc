package com.hfnew.dto.warehouse;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InventoryInVO {
    private Long id;
    private Long materialId;
    private String materialName;
    private String supplier;
    private String purchaseOrderNo;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private LocalDate inDate;
    private Long operatorId;
    private String attachmentUrl;
    private String remark;
    private LocalDateTime createTime;
}
