package com.hfnew.dto.notify;

import lombok.Data;

@Data
public class NotificationSummaryResponse {
    private Integer pendingReimbursementCount;
    private Integer feeWarningCount;
    private Integer stockWarningCount;
    private Integer drugExpiryWarningCount;
    private Integer contractExpiringCount;
}
