package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WubaoAllocationVO {

    private Long id;

    private String allocateMonth;

    private Integer elderCount; // 五保人数

    private BigDecimal livingFeeTotal; // 生活费合计 (800 × count)

    private BigDecimal careFeeTotal; // 照料补助合计 (2000 × count)

    private BigDecimal totalAmount; // 总金额

    private String counterparty;

    private String receiptNo;

    private Long operatorId;

    private LocalDateTime createTime;
}
