package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.staff.StaffCreateRequest;
import com.hfnew.dto.staff.StaffOption;
import com.hfnew.dto.staff.StaffUpdateRequest;
import com.hfnew.dto.staff.StaffVO;
import com.hfnew.entity.Staff;
import com.hfnew.entity.StaffAssignment;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.StaffAssignmentMapper;
import com.hfnew.mapper.StaffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffMapper staffMapper;
    private final StaffAssignmentMapper staffAssignmentMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<StaffVO> list(int page, int pageSize, String keyword, String status) {
        Page<Staff> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<Staff> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Staff::getName, keyword).or().like(Staff::getPhone, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Staff::getStatus, status);
        }
        wrapper.orderByDesc(Staff::getCreateTime).orderByDesc(Staff::getId);
        IPage<Staff> result = staffMapper.selectPage(pageReq, wrapper);

        Map<Long, Integer> countMap = loadActiveElderlyCount(result.getRecords().stream().map(Staff::getId).collect(Collectors.toList()));
        List<StaffVO> list = result.getRecords().stream().map(s -> toVO(s, countMap.getOrDefault(s.getId(), 0))).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public List<StaffOption> options(String keyword) {
        LambdaQueryWrapper<Staff> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Staff::getStatus, "ACTIVE");
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Staff::getName, keyword);
        }
        wrapper.orderByDesc(Staff::getId);
        return staffMapper.selectList(wrapper).stream().limit(50).map(s -> {
            StaffOption opt = new StaffOption();
            opt.setId(s.getId());
            opt.setName(s.getName());
            return opt;
        }).collect(Collectors.toList());
    }

    public List<StaffOption> listByIds(List<Long> ids) {
        if (ids == null) return List.of();
        List<Long> staffIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (staffIds.isEmpty()) return List.of();
        List<Staff> list = staffMapper.selectBatchIds(staffIds);
        return list.stream().map(s -> {
            StaffOption opt = new StaffOption();
            opt.setId(s.getId());
            opt.setName(s.getName());
            return opt;
        }).collect(Collectors.toList());
    }

    public StaffVO getById(Long id) {
        Staff s = staffMapper.selectById(id);
        if (s == null) throw new BizException(404, 404, "护工不存在");
        int count = loadActiveElderlyCount(List.of(id)).getOrDefault(id, 0);
        return toVO(s, count);
    }

    @Transactional
    public Long create(StaffCreateRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException(400, 400, "姓名不能为空");
        }
        Staff s = new Staff();
        s.setName(request.getName());
        s.setPhone(request.getPhone());
        s.setGender(request.getGender());
        s.setBirthDate(request.getBirthDate());
        s.setAge(request.getAge());
        s.setHireDate(request.getHireDate());
        s.setJobType(request.getJobType());
        s.setBaseSalary(request.getBaseSalary());
        s.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "ACTIVE");
        s.setProbationStatus(StringUtils.hasText(request.getProbationStatus()) ? request.getProbationStatus() : "FORMAL");
        s.setProbationMonths(request.getProbationMonths());
        s.setHasCaregiverCert(request.getHasCaregiverCert());
        s.setHasHealthCert(request.getHasHealthCert());
        // 自动计算实习到期日期
        if ("INTERN".equals(s.getProbationStatus()) && s.getProbationMonths() != null && s.getHireDate() != null) {
            s.setProbationEndDate(s.getHireDate().plus(s.getProbationMonths(), ChronoUnit.MONTHS));
        }
        staffMapper.insert(s);
        return s.getId();
    }

    @Transactional
    public void update(Long id, StaffUpdateRequest request) {
        Staff s = staffMapper.selectById(id);
        if (s == null) throw new BizException(404, 404, "护工不存在");
        if (request.getName() != null) s.setName(request.getName());
        if (request.getPhone() != null) s.setPhone(request.getPhone());
        if (request.getGender() != null) s.setGender(request.getGender());
        if (request.getBirthDate() != null) s.setBirthDate(request.getBirthDate());
        if (request.getAge() != null) s.setAge(request.getAge());
        if (request.getHireDate() != null) s.setHireDate(request.getHireDate());
        if (request.getJobType() != null) s.setJobType(request.getJobType());
        if (request.getBaseSalary() != null) s.setBaseSalary(request.getBaseSalary());
        if (request.getStatus() != null) s.setStatus(request.getStatus());
        if (request.getResignDate() != null) s.setResignDate(request.getResignDate());
        if (request.getResignReason() != null) s.setResignReason(request.getResignReason());
        if (request.getProbationStatus() != null) s.setProbationStatus(request.getProbationStatus());
        if (request.getProbationMonths() != null) s.setProbationMonths(request.getProbationMonths());
        if (request.getHasCaregiverCert() != null) s.setHasCaregiverCert(request.getHasCaregiverCert());
        if (request.getHasHealthCert() != null) s.setHasHealthCert(request.getHasHealthCert());
        // 自动计算实习到期日期
        if ("INTERN".equals(s.getProbationStatus()) && s.getProbationMonths() != null && s.getHireDate() != null) {
            s.setProbationEndDate(s.getHireDate().plus(s.getProbationMonths(), ChronoUnit.MONTHS));
        } else if (!"INTERN".equals(s.getProbationStatus())) {
            s.setProbationEndDate(null);
        }
        staffMapper.updateById(s);
        if ("RESIGNED".equals(s.getStatus())) {
            endAssignmentsByStaffId(id);
        }
    }

    @Transactional
    public void delete(Long id) {
        Staff s = staffMapper.selectById(id);
        if (s == null) return;
        staffMapper.deleteById(id);
        endAssignmentsByStaffId(id);
    }

    private void endAssignmentsByStaffId(Long staffId) {
        LambdaQueryWrapper<StaffAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StaffAssignment::getStaffId, staffId)
                .eq(StaffAssignment::getStatus, "ACTIVE");
        List<StaffAssignment> list = staffAssignmentMapper.selectList(wrapper);
        LocalDateTime now = LocalDateTime.now();
        for (StaffAssignment a : list) {
            a.setStatus("INACTIVE");
            a.setEndTime(now);
            staffAssignmentMapper.updateById(a);
        }
    }

    private Map<Long, Integer> loadActiveElderlyCount(List<Long> staffIds) {
        Map<Long, Integer> map = new HashMap<>();
        if (staffIds == null || staffIds.isEmpty()) return map;
        List<Long> ids = staffIds.stream().distinct().filter(x -> x != null).collect(Collectors.toList());
        if (ids.isEmpty()) return map;
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT staff_id, COUNT(1) AS cnt FROM t_staff_assignment WHERE deleted = 0 AND status = 'ACTIVE' AND staff_id IN (" + in + ") GROUP BY staff_id";
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getLong("staff_id"), rs.getInt("cnt"));
        }, ids.toArray());
        return map;
    }

    private StaffVO toVO(Staff s, Integer elderlyCount) {
        StaffVO vo = new StaffVO();
        vo.setId(s.getId());
        vo.setName(s.getName());
        vo.setPhone(s.getPhone());
        vo.setGender(s.getGender());
        vo.setBirthDate(s.getBirthDate());
        // 动态计算年龄：根据birthDate计算
        if (s.getBirthDate() != null) {
            vo.setAge(Period.between(s.getBirthDate(), LocalDate.now()).getYears());
        } else {
            vo.setAge(s.getAge());
        }
        vo.setHireDate(s.getHireDate());
        vo.setResignDate(s.getResignDate());
        vo.setResignReason(s.getResignReason());
        vo.setJobType(s.getJobType());
        vo.setBaseSalary(s.getBaseSalary());
        vo.setStatus(s.getStatus());
        vo.setProbationStatus(s.getProbationStatus());
        vo.setProbationMonths(s.getProbationMonths());
        vo.setProbationEndDate(s.getProbationEndDate());
        vo.setHasCaregiverCert(s.getHasCaregiverCert());
        vo.setHasHealthCert(s.getHasHealthCert());
        vo.setElderlyCount(elderlyCount == null ? 0 : elderlyCount);
        vo.setCreateTime(s.getCreateTime());
        return vo;
    }
}
