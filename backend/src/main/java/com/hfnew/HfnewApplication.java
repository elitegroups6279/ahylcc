package com.hfnew;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
@MapperScan("com.hfnew.mapper")
@EnableAsync
@EnableScheduling
public class HfnewApplication {
    public static void main(String[] args) {
        SpringApplication.run(HfnewApplication.class, args);
    }
}

