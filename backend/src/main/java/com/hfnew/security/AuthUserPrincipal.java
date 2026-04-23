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
    private final Long orgId;

    public AuthUserPrincipal(Long userId, String username, List<String> permissions, List<String> roles, Long orgId) {
        this.userId = userId;
        this.username = username;
        this.permissions = permissions;
        this.roles = roles;
        this.orgId = orgId;
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

    public Long getOrgId() {
        return orgId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // permissions 作为 authority，roles 以 ROLE_ 前缀作为 authority，以支持 @PreAuthorize("hasRole('ADMIN')")
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        for (String perm : permissions) {
            authorities.add(new SimpleGrantedAuthority(perm));
        }
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
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

