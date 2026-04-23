package com.hfnew.service;

import com.hfnew.security.JwtService;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RefreshTokenStore {
    private static class RefreshRecord {
        private final Long userId;
        private final String username;
        private final List<String> permissions;
        private final List<String> roles;
        private final Long orgId;
        private final Date expiresAt;

        private RefreshRecord(Long userId, String username, List<String> permissions, List<String> roles, Long orgId, Date expiresAt) {
            this.userId = userId;
            this.username = username;
            this.permissions = permissions;
            this.roles = roles;
            this.orgId = orgId;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, RefreshRecord> store = new ConcurrentHashMap<>();

    public void put(String refreshToken, Long userId, String username, List<String> permissions, List<String> roles, Long orgId, long ttlMillis) {
        Date expiresAt = new Date(System.currentTimeMillis() + ttlMillis);
        store.put(refreshToken, new RefreshRecord(userId, username, permissions, roles, orgId, expiresAt));
    }

    public RefreshTokenInfo getValid(String refreshToken) {
        RefreshRecord rec = store.get(refreshToken);
        if (rec == null) return null;
        if (rec.expiresAt.before(new Date())) {
            store.remove(refreshToken);
            return null;
        }
        return new RefreshTokenInfo(rec.userId, rec.username, rec.permissions, rec.roles, rec.orgId);
    }

    public void remove(String refreshToken) {
        if (refreshToken == null) return;
        store.remove(refreshToken);
    }

    public void removeByUser(Long userId) {
        if (userId == null) return;
        store.entrySet().removeIf(e -> e.getValue().userId.equals(userId));
    }

    public record RefreshTokenInfo(Long userId, String username, List<String> permissions, List<String> roles, Long orgId) {
    }
}

