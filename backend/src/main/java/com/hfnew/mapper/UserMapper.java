package com.hfnew.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hfnew.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * Count users by username, including logically deleted ones.
     * Used for duplicate check before insert.
     */
    Integer countByUsername(String username);

    /**
     * Physically delete user_role bindings for soft-deleted users with the given username.
     */
    int physicalDeleteUserRolesByUsername(@Param("username") String username);

    /**
     * Physically delete soft-deleted users with the given username.
     */
    int physicalDeleteByUsername(@Param("username") String username);
}
