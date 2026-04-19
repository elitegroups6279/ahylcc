package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.dto.notify.NotificationSummaryResponse;
import com.hfnew.dto.notify.ReimbursementNoticeItem;
import com.hfnew.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<NotificationSummaryResponse>> summary() {
        NotificationSummaryResponse resp = new NotificationSummaryResponse();
        resp.setPendingReimbursementCount(notificationService.countPendingReimbursements());
        resp.setFeeWarningCount(notificationService.countFeeWarnings());
        resp.setStockWarningCount(notificationService.countStockWarnings());
        resp.setDrugExpiryWarningCount(notificationService.countDrugExpiryWarnings());
        resp.setContractExpiringCount(notificationService.countContractExpiring());
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/reimbursements")
    public ResponseEntity<ApiResponse<List<ReimbursementNoticeItem>>> reimbursements(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.listPendingReimbursements(limit)));
    }
}
