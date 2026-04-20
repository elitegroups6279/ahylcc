package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 用户-角色关联实体
 */
@Data
@TableName("t_user_role")
public class UserRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;
}
