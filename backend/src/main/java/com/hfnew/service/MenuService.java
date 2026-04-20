package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hfnew.dto.system.*;
import com.hfnew.entity.Menu;
import com.hfnew.entity.RoleMenu;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.MenuMapper;
import com.hfnew.mapper.RoleMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    /**
     * 获取完整菜单树
     */
    public List<MenuVO> getMenuTree() {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getStatus, 1)
                .orderByAsc(Menu::getSortOrder)
                .orderByAsc(Menu::getId);
        List<Menu> menus = menuMapper.selectList(wrapper);

        return buildMenuTree(menus, 0L);
    }

    /**
     * 根据角色ID列表获取菜单树
     */
    public List<MenuVO> getMenusByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询角色关联的菜单ID
        LambdaQueryWrapper<RoleMenu> rmWrapper = new LambdaQueryWrapper<>();
        rmWrapper.in(RoleMenu::getRoleId, roleIds);
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(rmWrapper);

        if (roleMenus.isEmpty()) {
            return new ArrayList<>();
        }

        // 去重菜单ID
        Set<Long> menuIds = roleMenus.stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toSet());

        // 查询菜单
        LambdaQueryWrapper<Menu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(Menu::getId, menuIds)
                .eq(Menu::getStatus, 1)
                .orderByAsc(Menu::getSortOrder)
                .orderByAsc(Menu::getId);
        List<Menu> menus = menuMapper.selectList(menuWrapper);

        return buildMenuTree(menus, 0L);
    }

    /**
     * 根据角色ID列表获取权限标识列表
     */
    public List<String> getPermissionsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询角色关联的菜单ID
        LambdaQueryWrapper<RoleMenu> rmWrapper = new LambdaQueryWrapper<>();
        rmWrapper.in(RoleMenu::getRoleId, roleIds);
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(rmWrapper);

        if (roleMenus.isEmpty()) {
            return new ArrayList<>();
        }

        // 去重菜单ID
        Set<Long> menuIds = roleMenus.stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toSet());

        // 查询菜单
        LambdaQueryWrapper<Menu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(Menu::getId, menuIds)
                .eq(Menu::getStatus, 1)
                .isNotNull(Menu::getPermission);
        List<Menu> menus = menuMapper.selectList(menuWrapper);

        return menus.stream()
                .map(Menu::getPermission)
                .filter(Objects::nonNull)
                .filter(p -> !p.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 创建菜单
     */
    public Long createMenu(MenuCreateRequest request) {
        Menu menu = new Menu();
        menu.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        menu.setMenuName(request.getMenuName());
        menu.setMenuType(request.getMenuType());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setPermission(request.getPermission());
        menu.setIcon(request.getIcon());
        menu.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        menu.setVisible(1);
        menu.setStatus(1);
        menuMapper.insert(menu);

        log.info("创建菜单成功: {}", menu.getMenuName());
        return menu.getId();
    }

    /**
     * 更新菜单
     */
    public void updateMenu(Long id, MenuUpdateRequest request) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BizException(404, 404, "菜单不存在");
        }

        if (request.getParentId() != null) {
            menu.setParentId(request.getParentId());
        }
        if (request.getMenuName() != null) {
            menu.setMenuName(request.getMenuName());
        }
        if (request.getMenuType() != null) {
            menu.setMenuType(request.getMenuType());
        }
        if (request.getPath() != null) {
            menu.setPath(request.getPath());
        }
        if (request.getComponent() != null) {
            menu.setComponent(request.getComponent());
        }
        if (request.getPermission() != null) {
            menu.setPermission(request.getPermission());
        }
        if (request.getIcon() != null) {
            menu.setIcon(request.getIcon());
        }
        if (request.getSortOrder() != null) {
            menu.setSortOrder(request.getSortOrder());
        }
        if (request.getVisible() != null) {
            menu.setVisible(request.getVisible());
        }
        menuMapper.updateById(menu);

        log.info("更新菜单成功: {}", menu.getMenuName());
    }

    /**
     * 删除菜单及其子菜单
     */
    @Transactional
    public void deleteMenu(Long id) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BizException(404, 404, "菜单不存在");
        }

        // 删除子菜单
        deleteChildMenus(id);

        // 删除当前菜单
        menuMapper.deleteById(id);

        // 删除角色-菜单关联
        LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenu::getMenuId, id);
        roleMenuMapper.delete(wrapper);

        log.info("删除菜单成功: {}", menu.getMenuName());
    }

    /**
     * 递归删除子菜单
     */
    private void deleteChildMenus(Long parentId) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getParentId, parentId);
        List<Menu> children = menuMapper.selectList(wrapper);

        for (Menu child : children) {
            deleteChildMenus(child.getId());
            menuMapper.deleteById(child.getId());

            // 删除角色-菜单关联
            LambdaQueryWrapper<RoleMenu> rmWrapper = new LambdaQueryWrapper<>();
            rmWrapper.eq(RoleMenu::getMenuId, child.getId());
            roleMenuMapper.delete(rmWrapper);
        }
    }

    /**
     * 构建菜单树
     */
    private List<MenuVO> buildMenuTree(List<Menu> menus, Long parentId) {
        List<MenuVO> tree = new ArrayList<>();

        for (Menu menu : menus) {
            if (Objects.equals(menu.getParentId(), parentId)) {
                MenuVO vo = toMenuVO(menu);
                vo.setChildren(buildMenuTree(menus, menu.getId()));
                tree.add(vo);
            }
        }

        return tree;
    }

    /**
     * 转换为 MenuVO
     */
    private MenuVO toMenuVO(Menu menu) {
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setPermission(menu.getPermission());
        vo.setIcon(menu.getIcon());
        vo.setSortOrder(menu.getSortOrder());
        vo.setVisible(menu.getVisible());
        return vo;
    }
}
