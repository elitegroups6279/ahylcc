package com.hfnew.dto.home;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HomeServiceRecordCreateRequest {
    private Long orderId;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private String serviceContent;
    private String signatureUrl;
    private Integer rating;
    private BigDecimal amount;
    private String paymentStatus;
    private String remark;
}
