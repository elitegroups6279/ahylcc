package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VoucherHeaderVO {

    private Long id;

    private String voucherWord;

    private String voucherNo;

    private LocalDate voucherDate;

    private Integer attachmentCount;

    private String description;

    private String status;

    private String relatedBizType;

    private Long relatedBizId;

    private Long creatorId;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    private String rejectReason;

    private LocalDateTime createTime;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;

    private List<VoucherEntryVO> entries;
}
