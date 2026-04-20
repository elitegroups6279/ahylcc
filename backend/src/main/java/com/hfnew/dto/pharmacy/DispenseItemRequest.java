package com.hfnew.dto.pharmacy;

import lombok.Data;

@Data
public class DispenseItemRequest {
    private Long drugId;
    private Integer quantity;
    private String dosage;
    private Long executorId;
    private String remark;
}
