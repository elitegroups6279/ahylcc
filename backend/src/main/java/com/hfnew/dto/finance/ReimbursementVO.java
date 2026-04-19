package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReimbursementVO {
    private Long id;
    private Long applicantId;
    private BigDecimal amount;
    private String reason;
    private String attachmentUrls;
    private String status;
    private Long approverId;
    private LocalDateTime approveTime;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String rejectReason;
    private LocalDateTime createTime;
}
