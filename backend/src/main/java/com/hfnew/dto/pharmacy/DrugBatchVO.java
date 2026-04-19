package com.hfnew.dto.pharmacy;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DrugBatchVO {
    private Long id;
    private Long drugId;
    private String drugName;
    private String batchNo;
    private Integer quantity;
    private Integer remaining;
    private LocalDate expiryDate;
    private LocalDate inDate;
    private String supplier;
    private Long operatorId;
    private LocalDateTime createTime;
}
