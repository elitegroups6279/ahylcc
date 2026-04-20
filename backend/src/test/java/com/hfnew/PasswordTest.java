package com.hfnew;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordTest {

    @Test
    void verifyExistingHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String existingHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        boolean matches = encoder.matches("Admin123!", existingHash);
        System.out.println("========================================");
        System.out.println("EXISTING HASH MATCHES Admin123!: " + matches);
        System.out.println("========================================");
    }

    @Test
    void generateNewHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String newHash = encoder.encode("Admin123!");
        System.out.println("========================================");
        System.out.println("GENERATED NEW HASH: " + newHash);
        System.out.println("NEW HASH MATCHES Admin123!: " + encoder.matches("Admin123!", newHash));
        System.out.println("========================================");
        assertTrue(encoder.matches("Admin123!", newHash));
    }
}
