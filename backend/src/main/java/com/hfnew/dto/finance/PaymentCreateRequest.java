package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentCreateRequest {
    private Long elderlyId;
    private BigDecimal amount;
    private String paymentMethod;
    private String sourceType;
    private String voucherUrl;
    private String receiptNo;
    private String remark;
    private String incomeType;
    private String description;
    private LocalDate paymentDate;
}
