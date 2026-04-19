package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.finance.VoucherHeaderCreateRequest;
import com.hfnew.dto.finance.VoucherHeaderVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.VoucherHeaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherHeaderService voucherHeaderService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:voucher')")
    public ResponseEntity<ApiResponse<PageResult<VoucherHeaderVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String voucherWord
    ) {
        return ResponseEntity.ok(ApiResponse.success(voucherHeaderService.list(page, pageSize, month, status, voucherWord)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:voucher')")
    public ResponseEntity<ApiResponse<VoucherHeaderVO>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(voucherHeaderService.getDetail(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:voucher')")
    @OpLog(module = "凭证管理", operation = "新增凭证")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody VoucherHeaderCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(voucherHeaderService.create(getCurrentUserId(), request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:voucher')")
    @OpLog(module = "凭证管理", operation = "编辑凭证")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable Long id, @RequestBody VoucherHeaderCreateRequest request) {
        voucherHeaderService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:voucher')")
    @OpLog(module = "凭证管理", operation = "删除凭证")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        voucherHeaderService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('finance:voucher')")
    @OpLog(module = "凭证管理", operation = "提交审核")
    public ResponseEntity<ApiResponse<Object>> submit(@PathVariable Long id) {
        voucherHeaderService.submit(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('finance:voucher')")
    @OpLog(module = "凭证管理", operation = "审核通过")
    public ResponseEntity<ApiResponse<Object>> approve(@PathVariable Long id) {
        voucherHeaderService.approve(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('finance:voucher')")
    @OpLog(module = "凭证管理", operation = "审核驳回")
    public ResponseEntity<ApiResponse<Object>> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        voucherHeaderService.reject(id, getCurrentUserId(), reason);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/next-no")
    @PreAuthorize("hasAuthority('finance:voucher')")
    public ResponseEntity<ApiResponse<String>> getNextNo(
            @RequestParam(required = false) String voucherWord,
            @RequestParam(required = false) LocalDate date
    ) {
        return ResponseEntity.ok(ApiResponse.success(voucherHeaderService.getNextVoucherNo(voucherWord, date)));
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
