package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.config.OpLog;
import com.hfnew.dto.finance.FundFlowSummaryVO;
import com.hfnew.dto.finance.ReconciliationReportVO;
import com.hfnew.dto.finance.WubaoUsageReportVO;
import com.hfnew.service.FinanceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/reports")
@RequiredArgsConstructor
public class FinanceReportController {

    private final FinanceReportService financeReportService;

    @GetMapping("/account-reconciliation")
    @OpLog(module = "财务报表", operation = "账户对账")
    public ResponseEntity<ApiResponse<ReconciliationReportVO>> accountReconciliation(
            @RequestParam Long accountId,
            @RequestParam String month) {
        return ResponseEntity.ok(ApiResponse.success(financeReportService.accountReconciliation(accountId, month)));
    }

    @GetMapping("/wubao-usage")
    @OpLog(module = "财务报表", operation = "五保资金使用")
    public ResponseEntity<ApiResponse<WubaoUsageReportVO>> wubaoUsage(
            @RequestParam String month) {
        return ResponseEntity.ok(ApiResponse.success(financeReportService.wubaoUsage(month)));
    }

    @PreAuthorize("hasAuthority('report:export')")
    @GetMapping("/fund-flow")
    @OpLog(module = "财务报表", operation = "资金流向")
    public ResponseEntity<ApiResponse<FundFlowSummaryVO>> fundFlow(
            @RequestParam String startMonth,
            @RequestParam String endMonth) {
        return ResponseEntity.ok(ApiResponse.success(financeReportService.fundFlow(startMonth, endMonth)));
    }
}
