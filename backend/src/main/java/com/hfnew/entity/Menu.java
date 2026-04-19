package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单实体
 */
@Data
@TableName("t_menu")
public class Menu {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 父菜单ID，0表示顶级菜单
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 菜单类型：0目录 1菜单 2按钮
     */
    private Integer menuType;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 权限标识
     */
    private String permission;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 是否可见：1是 0否
     */
    private Integer visible;

    /**
     * 状态：1启用 0停用
     */
    private Integer status;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
