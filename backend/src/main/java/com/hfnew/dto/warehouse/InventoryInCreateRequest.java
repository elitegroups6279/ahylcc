package com.hfnew.dto.warehouse;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InventoryInCreateRequest {
    private Long materialId;
    private String supplier;
    private String purchaseOrderNo;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private LocalDate inDate;
    private String attachmentUrl;
    private String remark;
}
