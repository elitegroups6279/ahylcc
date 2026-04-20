package com.hfnew.dto.pharmacy;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DrugBatchCreateRequest {
    private Long drugId;
    private String batchNo;
    private Integer quantity;
    private LocalDate expiryDate;
    private LocalDate inDate;
    private String supplier;
}
