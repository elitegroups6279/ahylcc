package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.warehouse.InventoryCheckCreateRequest;
import com.hfnew.dto.warehouse.InventoryCheckVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.InventoryCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouse/checks")
@RequiredArgsConstructor
public class WarehouseInventoryCheckController {

    private final InventoryCheckService inventoryCheckService;

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse:check')")
    public ResponseEntity<ApiResponse<PageResult<InventoryCheckVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(ApiResponse.success(inventoryCheckService.list(page, pageSize)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse:check')")
    @OpLog(module = "仓库管理", operation = "盘点登记")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody InventoryCheckCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryCheckService.create(getCurrentUserId(), request)));
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
