package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.staff.AttendanceUpsertRequest;
import com.hfnew.dto.staff.AttendanceVO;
import com.hfnew.entity.Attendance;
import com.hfnew.entity.Staff;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.AttendanceMapper;
import com.hfnew.mapper.StaffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceMapper attendanceMapper;
    private final StaffMapper staffMapper;

    public PageResult<AttendanceVO> list(int page, int pageSize, Long staffId, LocalDate startDate, LocalDate endDate) {
        Page<Attendance> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        if (staffId != null) wrapper.eq(Attendance::getStaffId, staffId);
        if (startDate != null) wrapper.ge(Attendance::getAttendanceDate, startDate);
        if (endDate != null) wrapper.le(Attendance::getAttendanceDate, endDate);
        wrapper.orderByDesc(Attendance::getAttendanceDate).orderByDesc(Attendance::getId);
        IPage<Attendance> result = attendanceMapper.selectPage(pageReq, wrapper);

        Map<Long, String> nameMap = loadStaffNames(result.getRecords().stream().map(Attendance::getStaffId).collect(Collectors.toList()));
        List<AttendanceVO> list = result.getRecords().stream().map(a -> toVO(a, nameMap.get(a.getStaffId()))).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long upsert(AttendanceUpsertRequest request) {
        if (request.getStaffId() == null) throw new BizException(400, 400, "请选择护工");
        if (request.getAttendanceDate() == null) throw new BizException(400, 400, "请选择日期");
        Staff s = staffMapper.selectById(request.getStaffId());
        if (s == null) throw new BizException(404, 404, "护工不存在");

        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attendance::getStaffId, request.getStaffId()).eq(Attendance::getAttendanceDate, request.getAttendanceDate());
        Attendance existing = attendanceMapper.selectOne(wrapper);

        String status = request.getStatus() == null ? "NORMAL" : request.getStatus();

        if (existing == null) {
            Attendance a = new Attendance();
            a.setStaffId(request.getStaffId());
            a.setAttendanceDate(request.getAttendanceDate());
            a.setClockInTime(request.getClockInTime());
            a.setClockOutTime(request.getClockOutTime());
            a.setStatus(status);
            a.setRemark(request.getRemark());
            attendanceMapper.insert(a);
            return a.getId();
        }

        existing.setClockInTime(request.getClockInTime());
        existing.setClockOutTime(request.getClockOutTime());
        existing.setStatus(status);
        existing.setRemark(request.getRemark());
        attendanceMapper.updateById(existing);
        return existing.getId();
    }

    private Map<Long, String> loadStaffNames(List<Long> staffIds) {
        Map<Long, String> map = new HashMap<>();
        if (staffIds == null || staffIds.isEmpty()) return map;
        List<Long> ids = staffIds.stream().distinct().filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) return map;
        List<Staff> list = staffMapper.selectBatchIds(ids);
        for (Staff s : list) {
            map.put(s.getId(), s.getName());
        }
        return map;
    }

    private AttendanceVO toVO(Attendance a, String staffName) {
        AttendanceVO vo = new AttendanceVO();
        vo.setId(a.getId());
        vo.setStaffId(a.getStaffId());
        vo.setStaffName(staffName);
        vo.setAttendanceDate(a.getAttendanceDate());
        vo.setClockInTime(a.getClockInTime());
        vo.setClockOutTime(a.getClockOutTime());
        vo.setStatus(a.getStatus());
        vo.setRemark(a.getRemark());
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }
}
