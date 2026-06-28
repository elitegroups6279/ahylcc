package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.dto.warehouse.BudgetRequest;
import com.hfnew.entity.Budget;
import com.hfnew.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse:budget')")
    public ResponseEntity<ApiResponse<PageResult<Budget>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String supplyCategory
    ) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.list(page, size, year, supplyCategory)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse:budget')")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody BudgetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('warehouse:budget')")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long id, @RequestBody BudgetRequest request) {
        budgetService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('warehouse:budget')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('warehouse:budget')")
    public ResponseEntity<ApiResponse<Budget>> getCurrentBudget(@RequestParam String supplyCategory) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getCurrentBudget(supplyCategory)));
    }
}
