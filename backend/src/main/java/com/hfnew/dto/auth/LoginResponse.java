package com.hfnew.dto.auth;

public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresInSeconds;
    private MeResponse user;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public MeResponse getUser() {
        return user;
    }

    public void setUser(MeResponse user) {
        this.user = user;
    }
}

