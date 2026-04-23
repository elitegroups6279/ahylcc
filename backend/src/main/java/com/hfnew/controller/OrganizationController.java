package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.system.OrganizationCreateRequest;
import com.hfnew.dto.system.OrganizationUpdateRequest;
import com.hfnew.dto.system.OrganizationVO;
import com.hfnew.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 机构管理 Controller
 */
@RestController
@RequestMapping("/api/system/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * 分页查询机构
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_MANAGER')")
    public ResponseEntity<ApiResponse<PageResult<OrganizationVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        PageResult<OrganizationVO> result = organizationService.listOrganizations(page, pageSize, keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取所有启用的机构（下拉用）
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_MANAGER')")
    public ResponseEntity<ApiResponse<List<OrganizationVO>>> all() {
        List<OrganizationVO> list = organizationService.listAll();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * 获取机构详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_MANAGER')")
    public ResponseEntity<ApiResponse<OrganizationVO>> getById(@PathVariable Long id) {
        OrganizationVO vo = organizationService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(vo));
    }

    /**
     * 创建机构
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_MANAGER')")
    @OpLog(module = "机构管理", operation = "新增机构")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody OrganizationCreateRequest request) {
        Long id = organizationService.create(request);
        return ResponseEntity.ok(ApiResponse.success(id));
    }

    /**
     * 更新机构
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_MANAGER')")
    @OpLog(module = "机构管理", operation = "编辑机构")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable Long id,
                                                       @RequestBody OrganizationUpdateRequest request) {
        organizationService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 删除机构
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_MANAGER')")
    @OpLog(module = "机构管理", operation = "删除机构")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        organizationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
