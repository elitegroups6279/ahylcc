package com.hfnew.dto.system;

import lombok.Data;

/**
 * 更新菜单请求
 */
@Data
public class MenuUpdateRequest {

    /**
     * 父菜单ID
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
     * 是否可见
     */
    private Integer visible;
}
