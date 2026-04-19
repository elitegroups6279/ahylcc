package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.dto.auth.*;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpRequest
    ) {
        String xff = httpRequest.getHeader("X-Forwarded-For");
        String clientIp = (xff != null && !xff.isEmpty())
                ? xff.split(",")[0].trim()
                : httpRequest.getRemoteAddr();
        return ResponseEntity.ok(ApiResponse.success(authService.login(req.getUsername(), req.getPassword(), clientIp)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refresh(@Valid @RequestBody RefreshRequest req) {
        String accessToken = authService.refresh(req.getRefreshToken());
        AccessTokenResponse resp = new AccessTokenResponse(accessToken);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(@RequestBody(required = false) LogoutRequest req) {
        String refreshToken = req == null ? null : req.getRefreshToken();
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        AuthUserPrincipal p = (AuthUserPrincipal) principal;

        MeResponse me = new MeResponse();
        me.setUserId(p.getUserId());
        me.setUsername(p.getUsername());
        me.setRoles(p.getRoles());
        me.setPermissions(p.getPermissions());
        return ResponseEntity.ok(ApiResponse.success(me));
    }

    // P0：refresh 响应体占位
    public record AccessTokenResponse(String accessToken) {
    }
}

