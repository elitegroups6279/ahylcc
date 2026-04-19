package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentVO {
    private Long id;
    private Long elderlyId;
    private String elderlyName;
    private BigDecimal amount;
    private String paymentMethod;
    private String sourceType;
    private String voucherUrl;
    private String receiptNo;
    private Long operatorId;
    private String remark;
    private String incomeType;
    private String description;
    private LocalDateTime createTime;
}
