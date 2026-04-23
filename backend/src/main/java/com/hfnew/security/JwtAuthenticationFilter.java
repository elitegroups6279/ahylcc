package com.hfnew.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.hfnew.config.OrgContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length());
            try {
                Claims claims = jwtService.parseAccessToken(token);

                Long userId = Long.valueOf(claims.getSubject());
                String username = (String) claims.get("username");

                @SuppressWarnings("unchecked")
                List<String> perms = (List<String>) claims.get("perms");
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles");

                if (perms == null) perms = new ArrayList<>();
                if (roles == null) roles = new ArrayList<>();

                Long orgId = null;
                Object orgIdClaim = claims.get("orgId");
                if (orgIdClaim != null) {
                    orgId = Long.valueOf(orgIdClaim.toString());
                }

                AuthUserPrincipal principal = new AuthUserPrincipal(userId, username, perms, roles, orgId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                // Set org_id into ThreadLocal for MyBatis-Plus tenant interceptor
                OrgContextHolder.setOrgId(principal.getOrgId());
            } catch (Exception ignored) {
                // 无效/过期 token：不设置 Authentication，让后续 Security 处理为 401
                SecurityContextHolder.clearContext();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clear ThreadLocal to prevent memory leaks and cross-request contamination
            OrgContextHolder.clear();
        }
    }
}

