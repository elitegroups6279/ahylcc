package com.hfnew.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hfnew.entity.Elderly;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ElderlyMapper extends BaseMapper<Elderly> {

    /**
     * 查询指定月份入住的老人（不受逻辑删除过滤，包含deleted=0和deleted=1）
     */
    @Select("SELECT * FROM t_elderly WHERE admission_date >= #{monthStart} AND admission_date <= #{monthEnd}")
    List<Elderly> selectAdmissionsInMonth(@Param("monthStart") LocalDate monthStart,
                                          @Param("monthEnd") LocalDate monthEnd);

    /**
     * 查询指定月份退住的老人（不受逻辑删除过滤，包含deleted=0和deleted=1）
     */
    @Select("SELECT * FROM t_elderly WHERE discharge_date IS NOT NULL AND discharge_date >= #{monthStart} AND discharge_date <= #{monthEnd}")
    List<Elderly> selectDischargesInMonth(@Param("monthStart") LocalDate monthStart,
                                           @Param("monthEnd") LocalDate monthEnd);
}
