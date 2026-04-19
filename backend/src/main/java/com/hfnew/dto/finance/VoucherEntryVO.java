package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherEntryVO {

    private Long id;

    private Integer lineNo;

    private String summary;

    private Long subjectId;

    private String subjectCode;

    private String subjectName;

    private BigDecimal debitAmount;

    private BigDecimal creditAmount;
}
