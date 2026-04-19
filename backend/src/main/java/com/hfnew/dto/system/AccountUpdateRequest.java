package com.hfnew.dto.system;

import lombok.Data;

import java.util.List;

/**
 * 更新账户请求
 */
@Data
public class AccountUpdateRequest {

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
     * 角色ID列表
     */
    private List<Long> roleIds;
}
