package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.system.*;
import com.hfnew.entity.Role;
import com.hfnew.entity.User;
import com.hfnew.entity.UserRole;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.RoleMapper;
import com.hfnew.mapper.UserMapper;
import com.hfnew.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     */
    public PageResult<AccountVO> listUsers(int page, int pageSize, String keyword) {
        Page<User> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getPhone, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> result = userMapper.selectPage(pageReq, wrapper);

        List<AccountVO> voList = result.getRecords().stream()
                .map(this::toAccountVO)
                .collect(Collectors.toList());

        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }

    /**
     * 获取用户详情
     */
    public AccountVO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, 404, "用户不存在");
        }
        return toAccountVO(user);
    }

    /**
     * 创建用户
     */
    @Transactional
    public Long createUser(AccountCreateRequest request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException(400, 400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(1);
        userMapper.insert(user);

        // 绑定角色
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            for (Long roleId : request.getRoleIds()) {
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }

        log.info("创建用户成功: {}", user.getUsername());
        return user.getId();
    }

    /**
     * 更新用户
     */
    @Transactional
    public void updateUser(Long id, AccountUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, 404, "用户不存在");
        }

        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        userMapper.updateById(user);

        // 重新绑定角色
        if (request.getRoleIds() != null) {
            // 删除旧的角色关联
            LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserRole::getUserId, id);
            userRoleMapper.delete(wrapper);

            // 绑定新角色
            for (Long roleId : request.getRoleIds()) {
                UserRole userRole = new UserRole();
                userRole.setUserId(id);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }

        log.info("更新用户成功: {}", user.getUsername());
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, 404, "用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        log.info("重置密码成功: {}", user.getUsername());
    }

    /**
     * 启用/停用用户
     */
    public void toggleStatus(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, 404, "用户不存在");
        }

        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userMapper.updateById(user);

        log.info("切换用户状态: {} -> {}", user.getUsername(), user.getStatus());
    }

    /**
     * 删除用户（逻辑删除）
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, 404, "用户不存在");
        }

        // 不能删除 admin 用户
        if ("admin".equals(user.getUsername())) {
            throw new BizException(400, 400, "不能删除超级管理员");
        }

        userMapper.deleteById(id);

        // 删除角色关联
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, id);
        userRoleMapper.delete(wrapper);

        log.info("删除用户成功: {}", user.getUsername());
    }

    /**
     * 根据用户ID获取角色列表
     */
    public List<Role> getRolesByUserId(Long userId) {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, userId);
        List<UserRole> userRoles = userRoleMapper.selectList(wrapper);

        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());

        return roleMapper.selectBatchIds(roleIds);
    }

    /**
     * 转换为 AccountVO
     */
    private AccountVO toAccountVO(User user) {
        AccountVO vo = new AccountVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setCreateTime(user.getCreateTime());

        // 获取用户角色
        List<Role> roles = getRolesByUserId(user.getId());
        List<RoleVO> roleVOs = roles.stream().map(role -> {
            RoleVO roleVO = new RoleVO();
            roleVO.setId(role.getId());
            roleVO.setRoleName(role.getRoleName());
            roleVO.setRoleCode(role.getRoleCode());
            roleVO.setDescription(role.getDescription());
            roleVO.setStatus(role.getStatus());
            return roleVO;
        }).collect(Collectors.toList());
        vo.setRoles(roleVOs);

        return vo;
    }
}
