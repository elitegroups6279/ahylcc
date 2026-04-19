package com.hfnew.dto.system;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色视图对象
 */
@Data
public class RoleVO {

    private Long id;

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
     * 状态：1启用 0停用
     */
    private Integer status;

    /**
     * 菜单ID列表
     */
    private List<Long> menuIds;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
