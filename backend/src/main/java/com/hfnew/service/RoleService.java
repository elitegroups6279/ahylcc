package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.system.*;
import com.hfnew.entity.Role;
import com.hfnew.entity.RoleMenu;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.RoleMapper;
import com.hfnew.mapper.RoleMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;

    /**
     * 分页查询角色
     */
    public PageResult<RoleVO> listRoles(int page, int pageSize, String keyword) {
        Page<Role> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Role::getRoleName, keyword)
                    .or()
                    .like(Role::getRoleCode, keyword);
        }
        wrapper.orderByAsc(Role::getId);
        IPage<Role> result = roleMapper.selectPage(pageReq, wrapper);

        List<RoleVO> voList = result.getRecords().stream()
                .map(this::toRoleVO)
                .collect(Collectors.toList());

        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }

    /**
     * 获取所有角色（下拉用）
     */
    public List<RoleVO> getAllRoles() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getStatus, 1)
                .orderByAsc(Role::getId);
        List<Role> roles = roleMapper.selectList(wrapper);

        return roles.stream()
                .map(this::toRoleVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取角色详情
     */
    public RoleVO getRoleById(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(404, 404, "角色不存在");
        }
        return toRoleVO(role);
    }

    /**
     * 创建角色
     */
    @Transactional
    public Long createRole(RoleCreateRequest request) {
        // 检查角色编码是否已存在
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getRoleCode, request.getRoleCode());
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BizException(400, 400, "角色编码已存在");
        }

        Role role = new Role();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        role.setStatus(1);
        roleMapper.insert(role);

        // 绑定菜单
        if (request.getMenuIds() != null && !request.getMenuIds().isEmpty()) {
            for (Long menuId : request.getMenuIds()) {
                RoleMenu roleMenu = new RoleMenu();
                roleMenu.setRoleId(role.getId());
                roleMenu.setMenuId(menuId);
                roleMenuMapper.insert(roleMenu);
            }
        }

        log.info("创建角色成功: {}", role.getRoleCode());
        return role.getId();
    }

    /**
     * 更新角色
     */
    @Transactional
    public void updateRole(Long id, RoleUpdateRequest request) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(404, 404, "角色不存在");
        }

        if (request.getRoleName() != null) {
            role.setRoleName(request.getRoleName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
        roleMapper.updateById(role);

        // 重新绑定菜单
        if (request.getMenuIds() != null) {
            // 删除旧的菜单关联
            LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RoleMenu::getRoleId, id);
            roleMenuMapper.delete(wrapper);

            // 绑定新菜单
            for (Long menuId : request.getMenuIds()) {
                RoleMenu roleMenu = new RoleMenu();
                roleMenu.setRoleId(id);
                roleMenu.setMenuId(menuId);
                roleMenuMapper.insert(roleMenu);
            }
        }

        log.info("更新角色成功: {}", role.getRoleCode());
    }

    /**
     * 删除角色
     */
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(404, 404, "角色不存在");
        }

        // 不能删除超级管理员角色
        if ("SUPER_ADMIN".equals(role.getRoleCode())) {
            throw new BizException(400, 400, "不能删除超级管理员角色");
        }

        roleMapper.deleteById(id);

        // 删除菜单关联
        LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenu::getRoleId, id);
        roleMenuMapper.delete(wrapper);

        log.info("删除角色成功: {}", role.getRoleCode());
    }

    /**
     * 获取角色的菜单ID列表
     */
    public List<Long> getRoleMenuIds(Long roleId) {
        LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenu::getRoleId, roleId);
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(wrapper);

        return roleMenus.stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    /**
     * 转换为 RoleVO
     */
    private RoleVO toRoleVO(Role role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        vo.setMenuIds(getRoleMenuIds(role.getId()));
        return vo;
    }
}
