package com.hfnew.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hfnew.entity.Organization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 机构 Mapper
 */
@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
    /**
     * Count organizations by orgCode, including logically deleted ones.
     * Used for duplicate check before insert.
     */
    Integer countByOrgCode(String orgCode);

    /**
     * Physically delete soft-deleted organizations with the given orgCode.
     */
    int physicalDeleteByOrgCode(@Param("orgCode") String orgCode);
}
