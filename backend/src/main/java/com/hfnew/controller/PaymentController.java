package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.finance.ElderlyOption;
import com.hfnew.dto.finance.PaymentCreateRequest;
import com.hfnew.dto.finance.PaymentVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/api/finance/elderly/options")
    public ResponseEntity<ApiResponse<List<ElderlyOption>>> elderlyOptions(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.listElderlyOptions(keyword)));
    }

    @GetMapping("/api/finance/payments")
    public ResponseEntity<ApiResponse<PageResult<PaymentVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long elderlyId
    ) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.list(page, pageSize, elderlyId)));
    }

    @PostMapping("/api/finance/payments")
    @OpLog(module = "收支管理", operation = "登记缴费")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody PaymentCreateRequest request) {
        Long id = paymentService.create(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(id));
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
