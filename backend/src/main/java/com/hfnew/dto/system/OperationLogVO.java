package com.hfnew.dto.system;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志视图对象
 */
@Data
public class OperationLogVO {

    private Long id;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作内容
     */
    private String operation;

    /**
     * 请求参数
     */
    private String params;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 状态：1成功 0失败
     */
    private Integer status;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
}
