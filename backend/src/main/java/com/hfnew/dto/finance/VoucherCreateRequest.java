package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class VoucherCreateRequest {
    private String voucherType;
    private String category;
    private BigDecimal amount;
    private Long relatedId;
    private String attachmentUrl;
    private String description;
    private LocalDate voucherDate;
}
