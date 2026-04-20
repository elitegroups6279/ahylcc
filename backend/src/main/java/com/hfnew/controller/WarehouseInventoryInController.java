package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.warehouse.InventoryInCreateRequest;
import com.hfnew.dto.warehouse.InventoryInVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.InventoryInService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouse/in")
@RequiredArgsConstructor
public class WarehouseInventoryInController {

    private final InventoryInService inventoryInService;

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse:in')")
    public ResponseEntity<ApiResponse<PageResult<InventoryInVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(ApiResponse.success(inventoryInService.list(page, pageSize)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse:in')")
    @OpLog(module = "仓库管理", operation = "入库登记")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody InventoryInCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryInService.create(getCurrentUserId(), request)));
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
