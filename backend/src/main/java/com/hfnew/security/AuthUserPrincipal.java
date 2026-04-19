package com.hfnew.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AuthUserPrincipal implements UserDetails {
    private final Long userId;
    private final String username;
    private final List<String> permissions;
    private final List<String> roles;

    public AuthUserPrincipal(Long userId, String username, List<String> permissions, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.permissions = permissions;
        this.roles = roles;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 以 permissions 字符串作为 Spring Security 的 authority
        return permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return null; // 仅用于认证态，不用于登录校验
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

