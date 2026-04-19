package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class VoucherVO {
    private Long id;
    private String voucherNo;
    private String voucherType;
    private String category;
    private BigDecimal amount;
    private Long relatedId;
    private String attachmentUrl;
    private String description;
    private Long operatorId;
    private LocalDate voucherDate;
    private LocalDateTime createTime;
}
