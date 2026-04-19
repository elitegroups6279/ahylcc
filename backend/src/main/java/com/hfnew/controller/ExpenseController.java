package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.finance.ExpenseCreateRequest;
import com.hfnew.dto.finance.ExpenseVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.ExpenseRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseRecordService expenseRecordService;

    @GetMapping("/api/finance/expenses")
    public ResponseEntity<ApiResponse<PageResult<ExpenseVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(expenseRecordService.list(page, pageSize, expenseType, startDate, endDate)));
    }

    @PostMapping("/api/finance/expenses")
    @OpLog(module = "收支管理", operation = "登记支出")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody ExpenseCreateRequest request) {
        Long id = expenseRecordService.create(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(id));
    }

    @DeleteMapping("/api/finance/expenses/{id}")
    @OpLog(module = "收支管理", operation = "删除支出")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        expenseRecordService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/api/finance/cashflow/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(ApiResponse.success(expenseRecordService.getCashflowSummary(month)));
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
