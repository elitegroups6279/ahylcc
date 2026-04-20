package com.hfnew.dto.system;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账户视图对象
 */
@Data
public class AccountVO {

    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态：1启用 0停用
     */
    private Integer status;

    /**
     * 角色列表
     */
    private List<RoleVO> roles;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
