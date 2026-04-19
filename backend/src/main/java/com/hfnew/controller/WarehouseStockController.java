package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.dto.warehouse.StockVO;
import com.hfnew.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouse/stocks")
@RequiredArgsConstructor
public class WarehouseStockController {

    private final StockService stockService;

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse:stock')")
    public ResponseEntity<ApiResponse<PageResult<StockVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "false") boolean warningOnly
    ) {
        return ResponseEntity.ok(ApiResponse.success(stockService.list(page, pageSize, warningOnly)));
    }
}
