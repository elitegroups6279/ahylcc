package com.hfnew.dto.warehouse;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseRequestCreateDTO {
    /** JSON array of purchase items */
    private String itemsJson;

    private BigDecimal totalAmount;

    private String supplyCategory;

    private Long allocationId;

    private Long supplierId;

    private String remark;
}
