package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hfnew.dto.elderly.ElderlyLeaveRequest;
import com.hfnew.dto.elderly.ElderlyReturnRequest;
import com.hfnew.entity.Elderly;
import com.hfnew.entity.ElderlyLeave;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.ElderlyLeaveMapper;
import com.hfnew.mapper.ElderlyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ElderlyLeaveService {

    private final ElderlyLeaveMapper elderlyLeaveMapper;
    private final ElderlyMapper elderlyMapper;

    /**
     * 申请请假
     */
    @Transactional
    public ElderlyLeave applyLeave(Long elderlyId, ElderlyLeaveRequest req) {
        // 验证老人存在且status=ACTIVE
        Elderly elderly = elderlyMapper.selectById(elderlyId);
        if (elderly == null) {
            throw new BizException(404, 404, "老人不存在");
        }
        if (!"ACTIVE".equals(elderly.getStatus())) {
            throw new BizException(400, 400, "当前状态不可请假");
        }

        // 验证该老人没有正在进行的请假（status=ON_LEAVE）
        LambdaQueryWrapper<ElderlyLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyLeave::getElderlyId, elderlyId)
               .eq(ElderlyLeave::getStatus, "ON_LEAVE")
               .eq(ElderlyLeave::getDeleted, 0);
        Long activeLeaveCount = elderlyLeaveMapper.selectCount(wrapper);
        if (activeLeaveCount > 0) {
            throw new BizException(400, 400, "该老人已有正在进行的请假记录");
        }

        // 创建请假记录
        ElderlyLeave leave = new ElderlyLeave();
        leave.setElderlyId(elderlyId);
        leave.setStartDate(req.getStartDate());
        leave.setEndDate(req.getEndDate());
        leave.setReason(req.getReason());
        leave.setStatus("ON_LEAVE");
        elderlyLeaveMapper.insert(leave);

        // 更新老人status为ON_LEAVE（仅更新status字段，避免覆盖其他字段）
        LambdaUpdateWrapper<Elderly> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Elderly::getId, elderlyId)
                     .set(Elderly::getStatus, "ON_LEAVE");
        elderlyMapper.update(null, updateWrapper);

        return leave;
    }

    /**
     * 销假（返回）
     */
    @Transactional
    public ElderlyLeave returnFromLeave(Long elderlyId, ElderlyReturnRequest request) {
        // 查找该老人最新的ON_LEAVE记录
        LambdaQueryWrapper<ElderlyLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyLeave::getElderlyId, elderlyId)
               .eq(ElderlyLeave::getStatus, "ON_LEAVE")
               .eq(ElderlyLeave::getDeleted, 0)
               .orderByDesc(ElderlyLeave::getCreateTime)
               .last("LIMIT 1");
        ElderlyLeave leave = elderlyLeaveMapper.selectOne(wrapper);
        
        if (leave == null) {
            throw new BizException(404, 404, "未找到该老人的请假记录");
        }

        // 更新请假记录：status=RETURNED, returnDate=提供的日期或今天
        leave.setStatus("RETURNED");
        LocalDate returnDate = (request != null && request.getReturnDate() != null)
                ? request.getReturnDate() : LocalDate.now();
        leave.setReturnDate(returnDate);
        elderlyLeaveMapper.updateById(leave);

        // 更新老人status为ACTIVE（仅更新status字段，避免覆盖其他字段如disabilityLevel）
        LambdaUpdateWrapper<Elderly> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Elderly::getId, elderlyId)
                     .set(Elderly::getStatus, "ACTIVE");
        elderlyMapper.update(null, updateWrapper);

        return leave;
    }

    /**
     * 查询该老人当前ON_LEAVE的请假记录
     */
    public ElderlyLeave getActiveLeave(Long elderlyId) {
        LambdaQueryWrapper<ElderlyLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyLeave::getElderlyId, elderlyId)
               .eq(ElderlyLeave::getStatus, "ON_LEAVE")
               .eq(ElderlyLeave::getDeleted, 0)
               .orderByDesc(ElderlyLeave::getCreateTime)
               .last("LIMIT 1");
        return elderlyLeaveMapper.selectOne(wrapper);
    }

    /**
     * 查询该老人所有请假记录历史
     */
    public List<ElderlyLeave> getLeaveHistory(Long elderlyId) {
        LambdaQueryWrapper<ElderlyLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyLeave::getElderlyId, elderlyId)
               .eq(ElderlyLeave::getDeleted, 0)
               .orderByDesc(ElderlyLeave::getCreateTime);
        return elderlyLeaveMapper.selectList(wrapper);
    }
}
