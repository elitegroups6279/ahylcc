package com.hfnew.dto.system;

import lombok.Data;

import java.util.List;

/**
 * 创建角色请求
 */
@Data
public class RoleCreateRequest {

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 描述
     */
    private String description;

    /**
     * 菜单ID列表
     */
    private List<Long> menuIds;
}
