package com.hfnew.dto.pharmacy;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ExpiryWarningItem {
    private Long batchId;
    private Long drugId;
    private String drugName;
    private String batchNo;
    private Integer remaining;
    private LocalDate expiryDate;
    private Integer daysToExpiry;
}
