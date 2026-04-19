package com.hfnew.dto.system;

import lombok.Data;

/**
 * 更新配置请求
 */
@Data
public class ConfigUpdateRequest {

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;
}
