package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.home.HomeServiceRecordCreateRequest;
import com.hfnew.dto.home.HomeServiceRecordVO;
import com.hfnew.entity.HomeServiceOrder;
import com.hfnew.entity.HomeServiceRecord;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.HomeServiceOrderMapper;
import com.hfnew.mapper.HomeServiceRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceRecordService {

    private final HomeServiceRecordMapper recordMapper;
    private final HomeServiceOrderMapper orderMapper;

    public PageResult<HomeServiceRecordVO> list(int page, int pageSize, Long orderId) {
        Page<HomeServiceRecord> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<HomeServiceRecord> wrapper = new LambdaQueryWrapper<>();
        if (orderId != null) wrapper.eq(HomeServiceRecord::getOrderId, orderId);
        wrapper.orderByDesc(HomeServiceRecord::getCreateTime).orderByDesc(HomeServiceRecord::getId);
        IPage<HomeServiceRecord> result = recordMapper.selectPage(pageReq, wrapper);
        List<HomeServiceRecordVO> list = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long create(HomeServiceRecordCreateRequest request) {
        if (request.getOrderId() == null) throw new BizException(400, 400, "请选择订单");
        HomeServiceOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) throw new BizException(404, 404, "订单不存在");
        if (Objects.equals(order.getStatus(), "CANCELLED")) throw new BizException(400, 400, "已取消订单不可记录");

        HomeServiceRecord r = new HomeServiceRecord();
        r.setOrderId(request.getOrderId());
        r.setActualStartTime(request.getActualStartTime());
        r.setActualEndTime(request.getActualEndTime());
        r.setServiceContent(request.getServiceContent());
        r.setSignatureUrl(request.getSignatureUrl());
        r.setRating(request.getRating());
        r.setAmount(request.getAmount());
        r.setPaymentStatus(request.getPaymentStatus() == null ? "UNPAID" : request.getPaymentStatus());
        r.setRemark(request.getRemark());
        recordMapper.insert(r);
        return r.getId();
    }

    private HomeServiceRecordVO toVO(HomeServiceRecord r) {
        HomeServiceRecordVO vo = new HomeServiceRecordVO();
        vo.setId(r.getId());
        vo.setOrderId(r.getOrderId());
        vo.setActualStartTime(r.getActualStartTime());
        vo.setActualEndTime(r.getActualEndTime());
        vo.setServiceContent(r.getServiceContent());
        vo.setSignatureUrl(r.getSignatureUrl());
        vo.setRating(r.getRating());
        vo.setAmount(r.getAmount());
        vo.setPaymentStatus(r.getPaymentStatus());
        vo.setRemark(r.getRemark());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}
