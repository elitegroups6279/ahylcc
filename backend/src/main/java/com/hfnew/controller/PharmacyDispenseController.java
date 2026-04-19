package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.pharmacy.DispenseConfirmRequest;
import com.hfnew.dto.pharmacy.DispenseOrderCreateRequest;
import com.hfnew.dto.pharmacy.DispenseOrderVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.DispenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pharmacy/dispense/orders")
@RequiredArgsConstructor
public class PharmacyDispenseController {

    private final DispenseService dispenseService;

    @GetMapping
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PageResult<DispenseOrderVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(dispenseService.listOrders(page, pageSize, status)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    @OpLog(module = "药物管理", operation = "创建发药单")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody DispenseOrderCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(dispenseService.createOrder(getCurrentUserId(), request)));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    @OpLog(module = "药物管理", operation = "确认发药")
    public ResponseEntity<ApiResponse<Object>> confirm(@PathVariable Long id, @RequestBody DispenseConfirmRequest request) {
        dispenseService.confirm(id, request);
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
