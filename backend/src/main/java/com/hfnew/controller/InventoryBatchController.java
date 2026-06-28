package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.entity.InventoryBatch;
import com.hfnew.service.InventoryBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-batch")
@RequiredArgsConstructor
public class InventoryBatchController {

    private final InventoryBatchService inventoryBatchService;

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse:stock')")
    public ResponseEntity<ApiResponse<PageResult<InventoryBatch>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String supplyCategory
    ) {
        return ResponseEntity.ok(ApiResponse.success(inventoryBatchService.list(page, size, materialId, supplyCategory)));
    }

    @GetMapping("/near-expiry")
    @PreAuthorize("hasAuthority('warehouse:stock')")
    public ResponseEntity<ApiResponse<List<InventoryBatch>>> getNearExpiry(
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(ApiResponse.success(inventoryBatchService.getNearExpiry(days)));
    }
}
