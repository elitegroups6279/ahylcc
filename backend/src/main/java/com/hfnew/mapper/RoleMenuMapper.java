package com.hfnew.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hfnew.entity.RoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-菜单关联 Mapper
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {
}
