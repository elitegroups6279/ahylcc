package com.hfnew.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.ApiResponse;
import com.hfnew.config.OpLog;
import com.hfnew.dto.warehouse.PurchaseReceiptCreateDTO;
import com.hfnew.entity.PurchaseReceipt;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.PurchaseReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-receipt")
@RequiredArgsConstructor
public class PurchaseReceiptController {

    private final PurchaseReceiptService purchaseReceiptService;

    @PreAuthorize("hasAuthority('warehouse:purchase')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PurchaseReceipt>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long purchaseRequestId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseReceiptService.list(page, size, purchaseRequestId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse:purchase')")
    @OpLog(module = "采购管理", operation = "创建采购收货单")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody PurchaseReceiptCreateDTO dto) {
        Long id = purchaseReceiptService.create(getCurrentUserId(), dto);
        return ResponseEntity.ok(ApiResponse.success(id));
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
