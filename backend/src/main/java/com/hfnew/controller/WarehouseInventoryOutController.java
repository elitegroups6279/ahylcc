package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.warehouse.InventoryOutCreateRequest;
import com.hfnew.dto.warehouse.InventoryOutVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.InventoryOutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouse/out")
@RequiredArgsConstructor
public class WarehouseInventoryOutController {

    private final InventoryOutService inventoryOutService;

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse:out')")
    public ResponseEntity<ApiResponse<PageResult<InventoryOutVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(ApiResponse.success(inventoryOutService.list(page, pageSize)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse:out')")
    @OpLog(module = "仓库管理", operation = "出库登记")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody InventoryOutCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryOutService.create(getCurrentUserId(), request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OpLog(module = "仓库管理", operation = "修改出库记录")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long id, @RequestBody InventoryOutCreateRequest request) {
        inventoryOutService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OpLog(module = "仓库管理", operation = "删除出库记录")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        inventoryOutService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
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
