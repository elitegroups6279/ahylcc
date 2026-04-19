package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.pharmacy.DrugBatchCreateRequest;
import com.hfnew.dto.pharmacy.DrugBatchVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.DrugBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pharmacy/batches")
@RequiredArgsConstructor
public class PharmacyBatchController {

    private final DrugBatchService drugBatchService;

    @GetMapping
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PageResult<DrugBatchVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long drugId
    ) {
        return ResponseEntity.ok(ApiResponse.success(drugBatchService.list(page, pageSize, drugId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    @OpLog(module = "药物管理", operation = "药品批次入库")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody DrugBatchCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(drugBatchService.create(getCurrentUserId(), request)));
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
