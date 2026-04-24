package com.hfnew.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hfnew.entity.ElderlyLeave;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ElderlyLeaveMapper extends BaseMapper<ElderlyLeave> {
    
    @Select("SELECT * FROM t_elderly_leave WHERE deleted = 0 AND status IN ('ON_LEAVE', 'RETURNED') AND start_date <= #{monthEnd} AND (end_date IS NULL OR end_date >= #{monthStart} OR (status = 'RETURNED' AND return_date IS NOT NULL AND return_date >= #{monthStart}))")
    List<ElderlyLeave> selectLeavesInMonth(@Param("monthStart") LocalDate monthStart, @Param("monthEnd") LocalDate monthEnd);
}
