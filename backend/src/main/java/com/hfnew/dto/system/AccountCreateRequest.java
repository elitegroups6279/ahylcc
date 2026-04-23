package com.hfnew.dto.system;

import lombok.Data;

import java.util.List;

/**
 * 创建账户请求
 */
@Data
public class AccountCreateRequest {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

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
     * 角色ID列表
     */
    private List<Long> roleIds;

    /**
     * 所属机构ID
     */
    private Long orgId;
}
