package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("t_operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作用户ID
     */
    private Long userId;

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
     * 请求方法
     */
    private String method;

    /**
     * 请求参数
     */
    private String params;

    /**
     * 修改前值
     */
    private String oldValue;

    /**
     * 修改后值
     */
    private String newValue;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 状态：1成功 0失败
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
}
