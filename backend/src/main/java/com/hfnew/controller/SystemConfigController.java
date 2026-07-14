package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.dto.system.ConfigUpdateRequest;
import com.hfnew.dto.system.ConfigVO;
import com.hfnew.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 系统配置 Controller
 */
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 获取所有配置
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConfigVO>>> list() {
        List<ConfigVO> configs = systemConfigService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * 批量更新配置
     */
    @PreAuthorize("hasAuthority('system:config')")
    @PutMapping
    public ResponseEntity<ApiResponse<Object>> batchUpdate(@RequestBody List<ConfigUpdateRequest> configs) {
        systemConfigService.batchUpdateConfigs(configs);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 获取审核人列表
     */
    @GetMapping("/reviewers")
    public ResponseEntity<ApiResponse<List<String>>> reviewers() {
        List<String> reviewers = Arrays.asList("王希", "柳吴红", "胡婵娟");
        return ResponseEntity.ok(ApiResponse.success(reviewers));
    }
}
