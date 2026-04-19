package com.hfnew.dto.notify;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReimbursementNoticeItem {
    private Long id;
    private BigDecimal amount;
    private String reason;
    private String status;
    private LocalDateTime createTime;
}
