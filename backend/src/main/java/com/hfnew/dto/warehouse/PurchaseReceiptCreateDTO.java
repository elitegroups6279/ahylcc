package com.hfnew.dto.warehouse;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PurchaseReceiptCreateDTO {
    private Long purchaseRequestId;

    private String inspectResult;

    private Integer actualQuantity;

    private LocalDate receiptDate;

    private String remark;
}
