package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.system.OperationLogVO;
import com.hfnew.entity.OperationLog;
import com.hfnew.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    /**
     * 分页查询日志
     */
    public PageResult<OperationLogVO> listLogs(int page, int pageSize, String username, 
                                                String module, LocalDateTime startTime, 
                                                LocalDateTime endTime) {
        Page<OperationLog> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getOperationTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getOperationTime, endTime);
        }
        
        wrapper.orderByDesc(OperationLog::getOperationTime);
        IPage<OperationLog> result = operationLogMapper.selectPage(pageReq, wrapper);

        List<OperationLogVO> voList = result.getRecords().stream()
                .map(this::toOperationLogVO)
                .collect(Collectors.toList());

        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }

    /**
     * 异步保存日志
     */
    @Async
    public void saveLogAsync(OperationLog logEntity) {
        try {
            operationLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    /**
     * 同步保存日志
     */
    public void saveLog(OperationLog logEntity) {
        operationLogMapper.insert(logEntity);
    }

    /**
     * 转换为 VO
     */
    private OperationLogVO toOperationLogVO(OperationLog entity) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setModule(entity.getModule());
        vo.setOperation(entity.getOperation());
        vo.setParams(entity.getParams());
        vo.setIp(entity.getIp());
        vo.setStatus(entity.getStatus());
        vo.setOperationTime(entity.getOperationTime());
        return vo;
    }
}
