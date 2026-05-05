package com.hfnew.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hfnew.dto.staff.AssignedElderlyVO;
import com.hfnew.entity.StaffAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StaffAssignmentMapper extends BaseMapper<StaffAssignment> {

    @Select("SELECT e.id, e.name, e.unique_no, b.bed_number, e.disability_level, e.category, sa.assign_type " +
            "FROM t_staff_assignment sa " +
            "JOIN t_elderly e ON sa.elderly_id = e.id AND e.deleted = 0 " +
            "LEFT JOIN t_bed b ON e.bed_id = b.id AND b.deleted = 0 " +
            "WHERE sa.staff_id = #{staffId} AND sa.deleted = 0 AND sa.status = 'ACTIVE' " +
            "ORDER BY sa.start_time DESC")
    List<AssignedElderlyVO> selectAssignedElderlyByStaffId(@Param("staffId") Long staffId);
}
