package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 角色-菜单关联实体
 */
@Data
@TableName("t_role_menu")
public class RoleMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 菜单ID
     */
    private Long menuId;
}
