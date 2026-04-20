package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.dto.system.*;
import com.hfnew.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理 Controller
 */
@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<MenuVO>>> tree() {
        List<MenuVO> tree = menuService.getMenuTree();
        return ResponseEntity.ok(ApiResponse.success(tree));
    }

    /**
     * 创建菜单
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody MenuCreateRequest request) {
        Long id = menuService.createMenu(request);
        return ResponseEntity.ok(ApiResponse.success(id));
    }

    /**
     * 更新菜单
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable Long id,
                                                       @RequestBody MenuUpdateRequest request) {
        menuService.updateMenu(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
