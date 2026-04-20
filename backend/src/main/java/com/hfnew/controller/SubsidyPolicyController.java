package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.subsidy.SubsidyPolicyRequest;
import com.hfnew.dto.subsidy.SubsidyPolicyVO;
import com.hfnew.service.SubsidyPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subsidy-policies")
@RequiredArgsConstructor
public class SubsidyPolicyController {

    private final SubsidyPolicyService subsidyPolicyService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:config')")
    public ResponseEntity<ApiResponse<PageResult<SubsidyPolicyVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(ApiResponse.success(subsidyPolicyService.list(page, pageSize, category)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config')")
    public ResponseEntity<ApiResponse<SubsidyPolicyVO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(subsidyPolicyService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:config')")
    @OpLog(module = "补贴政策", operation = "新增补贴策略")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody SubsidyPolicyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(subsidyPolicyService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config')")
    @OpLog(module = "补贴政策", operation = "更新补贴策略")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable Long id, @RequestBody SubsidyPolicyRequest request) {
        subsidyPolicyService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config')")
    @OpLog(module = "补贴政策", operation = "删除补贴策略")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        subsidyPolicyService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('system:config')")
    @OpLog(module = "补贴政策", operation = "启用/禁用补贴策略")
    public ResponseEntity<ApiResponse<Object>> toggleEnabled(@PathVariable Long id) {
        subsidyPolicyService.toggleEnabled(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
