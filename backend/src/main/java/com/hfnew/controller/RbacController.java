package com.hfnew.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hfnew.common.ApiResponse;
import com.hfnew.dto.rbac.PermissionsResponse;
import com.hfnew.dto.system.MenuVO;
import com.hfnew.entity.UserRole;
import com.hfnew.mapper.UserRoleMapper;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rbac")
@RequiredArgsConstructor
public class RbacController {

    private final MenuService menuService;
    private final UserRoleMapper userRoleMapper;

    /**
     * 获取当前用户的权限列表
     */
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<PermissionsResponse>> permissions() {
        AuthUserPrincipal principal = getCurrentUser();

        // 从数据库获取最新权限（而不是从 JWT 中获取）
        List<Long> roleIds = getRoleIdsByUserId(principal.getUserId());
        List<String> permissions = menuService.getPermissionsByRoleIds(roleIds);

        PermissionsResponse resp = new PermissionsResponse();
        resp.setPermissions(permissions);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    /**
     * 获取当前用户的菜单树
     */
    @GetMapping("/menus")
    public ResponseEntity<ApiResponse<List<MenuVO>>> menus() {
        AuthUserPrincipal principal = getCurrentUser();

        // 获取用户角色ID列表
        List<Long> roleIds = getRoleIdsByUserId(principal.getUserId());

        // 根据角色获取菜单树
        List<MenuVO> menuTree = menuService.getMenusByRoleIds(roleIds);
        return ResponseEntity.ok(ApiResponse.success(menuTree));
    }

    /**
     * 获取当前登录用户
     */
    private AuthUserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (AuthUserPrincipal) auth.getPrincipal();
    }

    /**
     * 根据用户ID获取角色ID列表
     */
    private List<Long> getRoleIdsByUserId(Long userId) {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, userId);
        List<UserRole> userRoles = userRoleMapper.selectList(wrapper);

        return userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
    }
}
