package com.hfnew.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.ApiResponse;
import com.hfnew.config.OpLog;
import com.hfnew.dto.warehouse.PurchaseRequestCreateDTO;
import com.hfnew.entity.PurchaseRequest;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-request")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse:purchase')")
    public ResponseEntity<ApiResponse<Page<PurchaseRequest>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String supplyCategory
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseRequestService.list(page, size, status, supplyCategory)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse:purchase')")
    @OpLog(module = "采购管理", operation = "创建采购申请")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody PurchaseRequestCreateDTO dto) {
        Long id = purchaseRequestService.create(getCurrentUserId(), dto);
        return ResponseEntity.ok(ApiResponse.success(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('warehouse:purchase:approve')")
    @OpLog(module = "采购管理", operation = "审批通过采购申请")
    public ResponseEntity<ApiResponse<Object>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String remark = body != null ? body.getOrDefault("remark", "") : "";
        purchaseRequestService.approve(id, getCurrentUserId(), remark);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('warehouse:purchase:approve')")
    @OpLog(module = "采购管理", operation = "驳回采购申请")
    public ResponseEntity<ApiResponse<Object>> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String remark = body != null ? body.getOrDefault("remark", "") : "";
        purchaseRequestService.reject(id, getCurrentUserId(), remark);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 获取已审批通过且验收合格的采购申请列表（供入库FROM_PURCHASE模式使用）
     */
    @GetMapping("/ready-for-inbound")
    @PreAuthorize("hasAuthority('warehouse:in')")
    public ResponseEntity<ApiResponse<List<PurchaseRequest>>> listReadyForInbound() {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseRequestService.listReadyForInbound()));
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
