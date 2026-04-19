package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @GetMapping("/secure")
    @PreAuthorize("hasAuthority('demo:secure')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> secure() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true, "message", "demo secure ok")));
    }
}

