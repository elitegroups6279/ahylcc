package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.dto.finance.WubaoAllocateRequest;
import com.hfnew.dto.finance.WubaoAllocationVO;
import com.hfnew.dto.finance.WubaoMonthlySummaryVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.WubaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/wubao")
@RequiredArgsConstructor
public class WubaoController {

    private final WubaoService wubaoService;

    @PostMapping("/batch-allocate")
    public ResponseEntity<ApiResponse<WubaoAllocationVO>> batchAllocate(
            @RequestBody @Valid WubaoAllocateRequest req) {
        WubaoAllocationVO result = wubaoService.batchAllocate(req, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/allocation-records")
    public ResponseEntity<ApiResponse<PageResult<WubaoAllocationVO>>> listAllocations(
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<WubaoAllocationVO> result = wubaoService.listAllocations(month, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<ApiResponse<WubaoMonthlySummaryVO>> getMonthlySummary(
            @RequestParam String month) {
        WubaoMonthlySummaryVO result = wubaoService.getMonthlySummary(month);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth == null ? null : auth.getPrincipal();
        if (principal instanceof AuthUserPrincipal p) {
            return p.getUserId();
        }
        return null;
    }
}
