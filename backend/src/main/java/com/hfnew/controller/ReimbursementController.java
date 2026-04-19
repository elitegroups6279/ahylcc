package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.finance.ReimbursementCreateRequest;
import com.hfnew.dto.finance.ReimbursementRejectRequest;
import com.hfnew.dto.finance.ReimbursementVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.ReimbursementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/reimbursements")
@RequiredArgsConstructor
public class ReimbursementController {

    private final ReimbursementService reimbursementService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<ReimbursementVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success(reimbursementService.list(page, pageSize, status, keyword)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReimbursementVO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reimbursementService.getById(id)));
    }

    @PostMapping
    @OpLog(module = "报账管理", operation = "创建报账单")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody ReimbursementCreateRequest request) {
        Long id = reimbursementService.create(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(id));
    }

    @PutMapping("/{id}/approve")
    @OpLog(module = "报账管理", operation = "审批通过")
    public ResponseEntity<ApiResponse<Object>> approve(@PathVariable Long id) {
        reimbursementService.approve(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/{id}/reject")
    @OpLog(module = "报账管理", operation = "审批驳回")
    public ResponseEntity<ApiResponse<Object>> reject(@PathVariable Long id, @RequestBody ReimbursementRejectRequest request) {
        reimbursementService.reject(id, getCurrentUserId(), request.getRejectReason());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/{id}/mark-paid")
    @OpLog(module = "报账管理", operation = "标记已支付")
    public ResponseEntity<ApiResponse<Object>> markPaid(@PathVariable Long id) {
        reimbursementService.markPaid(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "报账管理", operation = "删除报账单")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        reimbursementService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
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
