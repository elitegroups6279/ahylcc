package com.hfnew.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hfnew.common.ApiResponse;
import com.hfnew.config.OpLog;
import com.hfnew.dto.warehouse.OutboundRequestCreateDTO;
import com.hfnew.entity.OutboundRequest;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.OutboundRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/outbound-request")
@RequiredArgsConstructor
public class OutboundRequestController {

    private final OutboundRequestService outboundRequestService;

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse:out')")
    public ResponseEntity<ApiResponse<IPage<OutboundRequest>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String supplyCategory
    ) {
        return ResponseEntity.ok(ApiResponse.success(outboundRequestService.list(page, size, status, supplyCategory)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse:out')")
    @OpLog(module = "仓库管理", operation = "提交物资申领单")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody OutboundRequestCreateDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(outboundRequestService.create(getCurrentUserId(), dto)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('warehouse:out:approve')")
    @OpLog(module = "仓库管理", operation = "审批通过申领单")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String remark = body != null ? body.get("remark") : null;
        outboundRequestService.approve(id, getCurrentUserId(), remark);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('warehouse:out:approve')")
    @OpLog(module = "仓库管理", operation = "驳回申领单")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String remark = body != null ? body.get("remark") : null;
        outboundRequestService.reject(id, getCurrentUserId(), remark);
        return ResponseEntity.ok(ApiResponse.success(null));
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
