package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.home.ServiceAssessmentCreateRequest;
import com.hfnew.dto.home.ServiceAssessmentVO;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.ServiceAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/home-service/assessments")
@RequiredArgsConstructor
public class ServiceAssessmentController {

    private final ServiceAssessmentService assessmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('home-service:assessment')")
    public ResponseEntity<ApiResponse<PageResult<ServiceAssessmentVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String grade
    ) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.list(page, pageSize, month, status, grade)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('home-service:assessment')")
    public ResponseEntity<ApiResponse<ServiceAssessmentVO>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.getDetail(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('home-service:assessment')")
    @OpLog(module = "服务评估", operation = "创建评估")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody ServiceAssessmentCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.create(getCurrentUserId(), request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('home-service:assessment')")
    @OpLog(module = "服务评估", operation = "编辑评估")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable Long id, @RequestBody ServiceAssessmentCreateRequest request) {
        assessmentService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('home-service:assessment')")
    @OpLog(module = "服务评估", operation = "删除评估")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        assessmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('home-service:assessment')")
    @OpLog(module = "服务评估", operation = "提交评估")
    public ResponseEntity<ApiResponse<Object>> submit(@PathVariable Long id) {
        assessmentService.submit(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('home-service:assessment')")
    @OpLog(module = "服务评估", operation = "确认评估")
    public ResponseEntity<ApiResponse<Object>> confirm(@PathVariable Long id) {
        assessmentService.confirm(id);
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
