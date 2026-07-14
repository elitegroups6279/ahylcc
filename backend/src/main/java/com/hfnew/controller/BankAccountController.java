package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.finance.BankAccountCreateRequest;
import com.hfnew.dto.finance.BankAccountVO;
import com.hfnew.dto.finance.BankDashboardVO;
import com.hfnew.dto.finance.BankTransactionCreateRequest;
import com.hfnew.dto.finance.BankTransactionVO;
import com.hfnew.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/finance/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:bank-account')")
    @OpLog(module = "银行账户", operation = "查询账户列表")
    public ResponseEntity<ApiResponse<List<BankAccountVO>>> listAccounts(
            @RequestParam(required = false) String accountType) {
        return ResponseEntity.ok(ApiResponse.success(bankAccountService.listAccounts(accountType)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:bank-account')")
    @OpLog(module = "银行账户", operation = "创建账户")
    public ResponseEntity<ApiResponse<BankAccountVO>> createAccount(
            @RequestBody @Valid BankAccountCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(bankAccountService.createAccount(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:bank-account')")
    @OpLog(module = "银行账户", operation = "更新账户")
    public ResponseEntity<ApiResponse<BankAccountVO>> updateAccount(
            @PathVariable Long id,
            @RequestBody BankAccountCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(bankAccountService.updateAccount(id, req)));
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasAuthority('finance:bank-account')")
    @OpLog(module = "银行账户", operation = "查询余额")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bankAccountService.getBalance(id)));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAuthority('finance:bank-account')")
    @OpLog(module = "银行账户", operation = "查询交易记录")
    public ResponseEntity<ApiResponse<PageResult<BankTransactionVO>>> listTransactions(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                bankAccountService.listTransactions(id, startDate, endDate, page, size)));
    }

    @PostMapping("/{id}/transactions")
    @PreAuthorize("hasAuthority('finance:bank-account')")
    @OpLog(module = "银行账户", operation = "创建交易记录")
    public ResponseEntity<ApiResponse<BankTransactionVO>> createTransaction(
            @PathVariable Long id,
            @RequestBody @Valid BankTransactionCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                bankAccountService.createTransaction(id, req)));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('finance:bank-account')")
    @OpLog(module = "银行账户", operation = "查询仪表盘")
    public ResponseEntity<ApiResponse<BankDashboardVO>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(bankAccountService.getDashboard()));
    }
}
