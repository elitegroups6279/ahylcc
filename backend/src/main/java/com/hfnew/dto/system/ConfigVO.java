package com.hfnew.dto.system;

import lombok.Data;

/**
 * 系统配置视图对象
 */
@Data
public class ConfigVO {

    private Long id;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置类型
     */
    private String configType;

    /**
     * 描述
     */
    private String description;
}
