package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.dto.system.*;
import com.hfnew.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理 Controller
 */
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 分页查询角色
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<RoleVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        PageResult<RoleVO> result = roleService.listRoles(page, pageSize, keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取所有角色（下拉用）
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<RoleVO>>> all() {
        List<RoleVO> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleVO>> getById(@PathVariable Long id) {
        RoleVO vo = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success(vo));
    }

    /**
     * 创建角色
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody RoleCreateRequest request) {
        Long id = roleService.createRole(request);
        return ResponseEntity.ok(ApiResponse.success(id));
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable Long id,
                                                       @RequestBody RoleUpdateRequest request) {
        roleService.updateRole(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
