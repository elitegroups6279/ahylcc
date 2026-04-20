package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReimbursementCreateRequest {
    private BigDecimal amount;
    private String reason;
    private String attachmentUrls;
}
