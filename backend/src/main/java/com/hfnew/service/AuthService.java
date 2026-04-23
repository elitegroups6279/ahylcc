package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hfnew.dto.auth.LoginResponse;
import com.hfnew.dto.auth.MeResponse;
import com.hfnew.entity.Role;
import com.hfnew.entity.User;
import com.hfnew.entity.UserRole;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.RoleMapper;
import com.hfnew.mapper.UserMapper;
import com.hfnew.mapper.UserRoleMapper;
import com.hfnew.security.JwtService;
import com.hfnew.service.RefreshTokenStore.RefreshTokenInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final MenuService menuService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;

    public AuthService(UserMapper userMapper,
                       RoleMapper roleMapper,
                       UserRoleMapper userRoleMapper,
                       MenuService menuService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenStore refreshTokenStore) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.menuService = menuService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
    }

    public LoginResponse login(String username, String password, String ip) {
        // 从数据库查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BizException(401, 401, "invalid username or password");
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new BizException(401, 401, "user is disabled");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(401, 401, "invalid username or password");
        }

        // 获取用户角色
        List<String> roles = getRoleCodesByUserId(user.getId());

        // 获取用户权限
        List<Long> roleIds = getRoleIdsByUserId(user.getId());
        List<String> permissions = menuService.getPermissionsByRoleIds(roleIds);

        // 生成 JWT
        String accessToken = jwtService.createAccessToken(user.getId(), user.getUsername(), permissions, roles, user.getOrgId());
        String refreshToken = jwtService.createRefreshToken(user.getId(), user.getUsername(), permissions, roles, user.getOrgId());

        // 保存 RefreshToken
        refreshTokenStore.put(
                refreshToken,
                user.getId(),
                user.getUsername(),
                permissions,
                roles,
                user.getOrgId(),
                jwtService.getRefreshTtlSeconds() * 1000
        );

        // 更新最后登录时间和IP
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userMapper.updateById(user);

        // 构建响应
        var resp = new LoginResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setExpiresInSeconds(jwtService.getAccessTtlSeconds());

        MeResponse me = new MeResponse();
        me.setUserId(user.getId());
        me.setUsername(user.getUsername());
        me.setRoles(roles);
        me.setPermissions(permissions);
        me.setOrgId(user.getOrgId());
        resp.setUser(me);

        log.info("用户登录成功: {}", username);
        return resp;
    }

    /**
     * 兼容旧的 login 方法（不传 IP）
     */
    public LoginResponse login(String username, String password) {
        return login(username, password, null);
    }

    public String refresh(String refreshToken) {
        RefreshTokenInfo info = refreshTokenStore.getValid(refreshToken);
        if (info == null) {
            throw new BizException(401, 401, "refresh token invalid or expired");
        }

        // 从数据库重新获取最新权限，避免 JWT 中权限过期
        List<Long> roleIds = getRoleIdsByUserId(info.userId());
        List<String> permissions = menuService.getPermissionsByRoleIds(roleIds);
        List<String> roles = getRoleCodesByUserId(info.userId());

        // 更新 RefreshTokenStore 中的权限信息
        refreshTokenStore.put(
                refreshToken,
                info.userId(),
                info.username(),
                permissions,
                roles,
                info.orgId(),
                jwtService.getRefreshTtlSeconds() * 1000
        );

        return jwtService.createAccessToken(
                info.userId(),
                info.username(),
                permissions,
                roles,
                info.orgId()
        );
    }

    public void logout(String refreshToken) {
        refreshTokenStore.remove(refreshToken);
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

    /**
     * 根据用户ID获取角色编码列表
     */
    private List<String> getRoleCodesByUserId(Long userId) {
        List<Long> roleIds = getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toList());
    }
}
