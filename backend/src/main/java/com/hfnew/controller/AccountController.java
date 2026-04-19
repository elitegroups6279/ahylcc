package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.config.OpLog;
import com.hfnew.dto.system.*;
import com.hfnew.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 账户管理 Controller
 */
@RestController
@RequestMapping("/api/system/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;

    /**
     * 分页查询用户列表
     */
    @GetMapping
    @OpLog(module = "账户管理", operation = "查询用户列表")
    public ResponseEntity<ApiResponse<PageResult<AccountVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        PageResult<AccountVO> result = userService.listUsers(page, pageSize, keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountVO>> getById(@PathVariable Long id) {
        AccountVO vo = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(vo));
    }

    /**
     * 创建用户
     */
    @PostMapping
    @OpLog(module = "账户管理", operation = "创建用户")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody AccountCreateRequest request) {
        Long id = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success(id));
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @OpLog(module = "账户管理", operation = "更新用户")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable Long id,
                                                       @RequestBody AccountUpdateRequest request) {
        userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 重置密码
     */
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@PathVariable Long id,
                                                              @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 启用/停用
     */
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Object>> toggleStatus(@PathVariable Long id) {
        userService.toggleStatus(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @OpLog(module = "账户管理", operation = "删除用户")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
