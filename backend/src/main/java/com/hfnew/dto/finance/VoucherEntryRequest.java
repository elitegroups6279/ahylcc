package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherEntryRequest {

    private String summary;

    private Long subjectId;

    private BigDecimal debitAmount;

    private BigDecimal creditAmount;
}
