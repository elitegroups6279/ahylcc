package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.entity.AccountingSubject;
import com.hfnew.service.AccountingSubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finance/accounting-subjects")
@RequiredArgsConstructor
public class AccountingSubjectController {

    private final AccountingSubjectService accountingSubjectService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:voucher')")
    public ResponseEntity<ApiResponse<List<AccountingSubject>>> list() {
        return ResponseEntity.ok(ApiResponse.success(accountingSubjectService.listEnabled()));
    }
}
