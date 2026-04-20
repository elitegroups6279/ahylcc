package com.hfnew.dto.system;

import lombok.Data;

import java.util.List;

/**
 * 更新角色请求
 */
@Data
public class RoleUpdateRequest {

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：1启用 0停用
     */
    private Integer status;

    /**
     * 菜单ID列表
     */
    private List<Long> menuIds;
}
