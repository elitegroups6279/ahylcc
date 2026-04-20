package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import com.hfnew.dto.system.ConfigUpdateRequest;
import com.hfnew.dto.system.ConfigVO;
import com.hfnew.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @PutMapping
    public ResponseEntity<ApiResponse<Object>> batchUpdate(@RequestBody List<ConfigUpdateRequest> configs) {
        systemConfigService.batchUpdateConfigs(configs);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
