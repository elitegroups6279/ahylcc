package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.finance.VoucherCreateRequest;
import com.hfnew.dto.finance.VoucherVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:voucher')")
    public ResponseEntity<ApiResponse<PageResult<VoucherVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String voucherMonth,
            @RequestParam(required = false) String voucherType
    ) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.list(page, pageSize, voucherMonth, voucherType)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:voucher')")
    @OpLog(module = "凭证管理", operation = "新增凭证")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody VoucherCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.create(getCurrentUserId(), request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:voucher')")
    @OpLog(module = "凭证管理", operation = "删除凭证")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        voucherService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
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
