package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.staff.ScheduleUpsertRequest;
import com.hfnew.dto.staff.ScheduleVO;
import com.hfnew.entity.Schedule;
import com.hfnew.entity.Staff;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.ScheduleMapper;
import com.hfnew.mapper.StaffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final Set<String> SHIFT_TYPES = Set.of("MORNING", "AFTERNOON", "NIGHT");

    private final ScheduleMapper scheduleMapper;
    private final StaffMapper staffMapper;

    public PageResult<ScheduleVO> list(int page, int pageSize, Long staffId, LocalDate startDate, LocalDate endDate, String shiftType) {
        Page<Schedule> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        if (staffId != null) wrapper.eq(Schedule::getStaffId, staffId);
        if (startDate != null) wrapper.ge(Schedule::getScheduleDate, startDate);
        if (endDate != null) wrapper.le(Schedule::getScheduleDate, endDate);
        if (StringUtils.hasText(shiftType)) wrapper.eq(Schedule::getShiftType, shiftType);
        wrapper.orderByDesc(Schedule::getScheduleDate).orderByDesc(Schedule::getId);
        IPage<Schedule> result = scheduleMapper.selectPage(pageReq, wrapper);

        Map<Long, String> nameMap = loadStaffNames(result.getRecords().stream().map(Schedule::getStaffId).collect(Collectors.toList()));
        List<ScheduleVO> list = result.getRecords().stream().map(s -> toVO(s, nameMap.get(s.getStaffId()))).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long upsert(ScheduleUpsertRequest request) {
        if (request == null) throw new BizException(400, 400, "参数错误");
        if (request.getStaffId() == null) throw new BizException(400, 400, "请选择护工");
        if (request.getScheduleDate() == null) throw new BizException(400, 400, "请选择日期");
        if (!StringUtils.hasText(request.getShiftType()) || !SHIFT_TYPES.contains(request.getShiftType())) {
            throw new BizException(400, 400, "班次类型错误");
        }
        Staff staff = staffMapper.selectById(request.getStaffId());
        if (staff == null) throw new BizException(404, 404, "护工不存在");

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getStaffId, request.getStaffId()).eq(Schedule::getScheduleDate, request.getScheduleDate());
        Schedule existing = scheduleMapper.selectOne(wrapper);

        if (existing == null) {
            Schedule s = new Schedule();
            s.setStaffId(request.getStaffId());
            s.setScheduleDate(request.getScheduleDate());
            s.setShiftType(request.getShiftType());
            s.setRemark(request.getRemark());
            scheduleMapper.insert(s);
            return s.getId();
        }

        existing.setShiftType(request.getShiftType());
        existing.setRemark(request.getRemark());
        scheduleMapper.updateById(existing);
        return existing.getId();
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new BizException(400, 400, "参数错误");
        Schedule s = scheduleMapper.selectById(id);
        if (s == null) throw new BizException(404, 404, "排班不存在");
        scheduleMapper.deleteById(id);
    }

    /**
     * 获取周视图数据
     * 按护工分组，返回每个护工在日期范围内的排班情况
     */
    public List<Map<String, Object>> getWeekView(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BizException(400, 400, "日期参数错误");
        }
        if (startDate.isAfter(endDate)) {
            throw new BizException(400, 400, "开始日期不能晚于结束日期");
        }

        // 查询日期范围内所有排班
        List<Schedule> schedules = scheduleMapper.selectList(
            new LambdaQueryWrapper<Schedule>()
                .ge(Schedule::getScheduleDate, startDate)
                .le(Schedule::getScheduleDate, endDate)
                .orderByAsc(Schedule::getScheduleDate)
        );

        // 查询所有在职护工
        List<Staff> staffList = staffMapper.selectList(
            new LambdaQueryWrapper<Staff>().eq(Staff::getStatus, "ACTIVE"));

        // 按护工ID分组排班数据
        Map<Long, Map<String, String>> staffScheduleMap = new HashMap<>();
        for (Schedule s : schedules) {
            staffScheduleMap.computeIfAbsent(s.getStaffId(), k -> new HashMap<>())
                .put(s.getScheduleDate().toString(), s.getShiftType());
        }

        // 构建返回结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Staff staff : staffList) {
            Map<String, Object> row = new HashMap<>();
            row.put("staffId", staff.getId());
            row.put("staffName", staff.getName());
            row.put("days", staffScheduleMap.getOrDefault(staff.getId(), new HashMap<>()));
            result.add(row);
        }

        return result;
    }

    /**
     * 批量创建排班
     * 遍历日期范围，对每天进行排班（有冲突则覆盖）
     */
    @Transactional
    public int batchCreate(Long staffId, LocalDate startDate, LocalDate endDate, String shiftType) {
        // 参数验证
        if (staffId == null) throw new BizException(400, 400, "请选择护工");
        if (startDate == null || endDate == null) throw new BizException(400, 400, "请选择日期范围");
        if (startDate.isAfter(endDate)) throw new BizException(400, 400, "开始日期不能晚于结束日期");
        if (!StringUtils.hasText(shiftType) || !SHIFT_TYPES.contains(shiftType)) {
            throw new BizException(400, 400, "班次类型错误");
        }

        // 验证护工存在
        Staff staff = staffMapper.selectById(staffId);
        if (staff == null) throw new BizException(404, 404, "护工不存在");

        int count = 0;
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        for (int i = 0; i < daysBetween; i++) {
            LocalDate currentDate = startDate.plusDays(i);

            // 检查是否已有排班
            LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Schedule::getStaffId, staffId).eq(Schedule::getScheduleDate, currentDate);
            Schedule existing = scheduleMapper.selectOne(wrapper);

            if (existing == null) {
                // 新建排班
                Schedule s = new Schedule();
                s.setStaffId(staffId);
                s.setScheduleDate(currentDate);
                s.setShiftType(shiftType);
                scheduleMapper.insert(s);
            } else {
                // 更新排班
                existing.setShiftType(shiftType);
                scheduleMapper.updateById(existing);
            }
            count++;
        }

        return count;
    }

    /**
     * 获取月度统计
     * 统计每个护工在指定月份的各班次数量
     */
    public List<Map<String, Object>> getMonthStats(String yearMonth) {
        if (!StringUtils.hasText(yearMonth)) {
            throw new BizException(400, 400, "月份参数错误");
        }

        YearMonth ym;
        try {
            ym = YearMonth.parse(yearMonth);
        } catch (Exception e) {
            throw new BizException(400, 400, "月份格式错误，应为 yyyy-MM");
        }

        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 查询该月所有排班
        List<Schedule> schedules = scheduleMapper.selectList(
            new LambdaQueryWrapper<Schedule>()
                .ge(Schedule::getScheduleDate, start)
                .le(Schedule::getScheduleDate, end)
        );

        // 查询所有护工（包括已离职的，因为要统计历史数据）
        List<Staff> staffList = staffMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, String> staffNameMap = staffList.stream()
            .collect(Collectors.toMap(Staff::getId, Staff::getName, (a, b) -> a));

        // 按护工ID分组统计
        Map<Long, Map<String, Integer>> statsMap = new HashMap<>();
        for (Schedule s : schedules) {
            statsMap.computeIfAbsent(s.getStaffId(), k -> {
                Map<String, Integer> map = new HashMap<>();
                map.put("morningCount", 0);
                map.put("afternoonCount", 0);
                map.put("nightCount", 0);
                return map;
            });

            Map<String, Integer> staffStats = statsMap.get(s.getStaffId());
            switch (s.getShiftType()) {
                case "MORNING":
                    staffStats.put("morningCount", staffStats.get("morningCount") + 1);
                    break;
                case "AFTERNOON":
                    staffStats.put("afternoonCount", staffStats.get("afternoonCount") + 1);
                    break;
                case "NIGHT":
                    staffStats.put("nightCount", staffStats.get("nightCount") + 1);
                    break;
            }
        }

        // 构建返回结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Map<String, Integer>> entry : statsMap.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            Long sid = entry.getKey();
            Map<String, Integer> counts = entry.getValue();

            row.put("staffId", sid);
            row.put("staffName", staffNameMap.getOrDefault(sid, "未知"));
            row.put("morningCount", counts.get("morningCount"));
            row.put("afternoonCount", counts.get("afternoonCount"));
            row.put("nightCount", counts.get("nightCount"));
            row.put("totalShifts", counts.get("morningCount") + counts.get("afternoonCount") + counts.get("nightCount"));
            result.add(row);
        }

        // 按总班次降序排序
        result.sort((a, b) -> ((Integer) b.get("totalShifts")).compareTo((Integer) a.get("totalShifts")));

        return result;
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

    private ScheduleVO toVO(Schedule s, String staffName) {
        ScheduleVO vo = new ScheduleVO();
        vo.setId(s.getId());
        vo.setStaffId(s.getStaffId());
        vo.setStaffName(staffName);
        vo.setScheduleDate(s.getScheduleDate());
        vo.setShiftType(s.getShiftType());
        vo.setRemark(s.getRemark());
        vo.setCreateTime(s.getCreateTime());
        return vo;
    }
}

