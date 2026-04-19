package com.hfnew;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        String plainPassword = "Admin123!";
        
        boolean matches = encoder.matches(plainPassword, hashedPassword);
        System.out.println("Password matches: " + matches);
        
        // Also test generating a new hash
        String newHash = encoder.encode(plainPassword);
        System.out.println("New hash: " + newHash);
        System.out.println("New hash matches: " + encoder.matches(plainPassword, newHash));
    }
}
