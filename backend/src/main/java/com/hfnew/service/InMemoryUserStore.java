package com.hfnew.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 内存用户存储（已废弃，现使用数据库存储）
 * @deprecated 已迁移到数据库，使用 UserMapper + AuthService 替代
 */
@Deprecated
@Component
public class InMemoryUserStore {

    public record UserRecord(Long userId, String username, String passwordHash, List<String> roles, List<String> permissions) {
    }

    private final Map<String, UserRecord> users;

    public InMemoryUserStore(PasswordEncoder passwordEncoder) {
        // P0 默认账号（你可以在前端 Login 页面里提示一下）
        // admin / Admin123!
        // staff / Staff123!
        users = Map.of(
                "admin", new UserRecord(
                        1L,
                        "admin",
                        passwordEncoder.encode("Admin123!"),
                        List.of("ADMIN"),
                        List.of("demo:secure")
                ),
                "staff", new UserRecord(
                        2L,
                        "staff",
                        passwordEncoder.encode("Staff123!"),
                        List.of("STAFF"),
                        List.of()
                )
        );
    }

    public UserRecord getByUsername(String username) {
        return users.get(username);
    }
}

