package com.hfnew.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.ApiResponse;
import com.hfnew.config.OpLog;
import com.hfnew.dto.warehouse.SupplierRequest;
import com.hfnew.entity.Supplier;
import com.hfnew.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supplier")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse:supplier')")
    @OpLog(module = "供应商管理", operation = "查询供应商列表")
    public ResponseEntity<ApiResponse<Page<Supplier>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success(supplierService.list(page, size, keyword)));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('warehouse:supplier')")
    @OpLog(module = "供应商管理", operation = "查询供应商选项")
    public ResponseEntity<ApiResponse<List<Supplier>>> options() {
        return ResponseEntity.ok(ApiResponse.success(supplierService.options()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse:supplier')")
    @OpLog(module = "供应商管理", operation = "新增供应商")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody SupplierRequest request) {
        return ResponseEntity.ok(ApiResponse.success(supplierService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('warehouse:supplier')")
    @OpLog(module = "供应商管理", operation = "更新供应商")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long id, @RequestBody SupplierRequest request) {
        supplierService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OpLog(module = "供应商管理", operation = "删除供应商")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
