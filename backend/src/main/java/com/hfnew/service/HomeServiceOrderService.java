package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.home.HomeServiceOrderCreateRequest;
import com.hfnew.dto.home.HomeServiceOrderUpdateRequest;
import com.hfnew.dto.home.HomeServiceOrderVO;
import com.hfnew.entity.HomeServiceOrder;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.HomeServiceOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceOrderService {

    private final HomeServiceOrderMapper orderMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<HomeServiceOrderVO> list(int page, int pageSize, String status) {
        Page<HomeServiceOrder> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<HomeServiceOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) wrapper.eq(HomeServiceOrder::getStatus, status);
        wrapper.orderByDesc(HomeServiceOrder::getCreateTime).orderByDesc(HomeServiceOrder::getId);
        IPage<HomeServiceOrder> result = orderMapper.selectPage(pageReq, wrapper);

        Map<Long, String> elderlyMap = loadNames("t_elderly", "name", result.getRecords().stream().map(HomeServiceOrder::getElderlyId).collect(Collectors.toList()));
        Map<Long, String> itemMap = loadNames("t_service_item", "name", result.getRecords().stream().map(HomeServiceOrder::getServiceItemId).collect(Collectors.toList()));
        Map<Long, String> staffMap = loadNames("t_staff", "name", result.getRecords().stream().map(HomeServiceOrder::getAssignedStaffId).collect(Collectors.toList()));

        List<HomeServiceOrderVO> list = result.getRecords().stream().map(o -> {
            HomeServiceOrderVO vo = new HomeServiceOrderVO();
            vo.setId(o.getId());
            vo.setElderlyId(o.getElderlyId());
            vo.setElderlyName(elderlyMap.get(o.getElderlyId()));
            vo.setServiceItemId(o.getServiceItemId());
            vo.setServiceItemName(itemMap.get(o.getServiceItemId()));
            vo.setExpectedTime(o.getExpectedTime());
            vo.setAddress(o.getAddress());
            vo.setSpecialNote(o.getSpecialNote());
            vo.setAssignedStaffId(o.getAssignedStaffId());
            vo.setAssignedStaffName(staffMap.get(o.getAssignedStaffId()));
            vo.setStatus(o.getStatus());
            vo.setCreateTime(o.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long create(HomeServiceOrderCreateRequest request) {
        if (request.getServiceItemId() == null) throw new BizException(400, 400, "请选择服务项目");
        Integer itemCnt = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM t_service_item WHERE deleted = 0 AND id = ? AND status = 1", Integer.class, request.getServiceItemId());
        if (itemCnt == null || itemCnt == 0) throw new BizException(404, 404, "服务项目不存在");

        if (request.getElderlyId() != null) {
            Integer elderlyCnt = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM t_elderly WHERE deleted = 0 AND id = ?", Integer.class, request.getElderlyId());
            if (elderlyCnt == null || elderlyCnt == 0) throw new BizException(404, 404, "老人不存在");
        }

        HomeServiceOrder order = new HomeServiceOrder();
        order.setElderlyId(request.getElderlyId());
        order.setServiceItemId(request.getServiceItemId());
        order.setExpectedTime(request.getExpectedTime());
        order.setAddress(request.getAddress());
        order.setSpecialNote(request.getSpecialNote());
        order.setAssignedStaffId(request.getAssignedStaffId());
        order.setStatus("PENDING");
        orderMapper.insert(order);
        return order.getId();
    }

    @Transactional
    public void update(Long id, HomeServiceOrderUpdateRequest request) {
        HomeServiceOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, 404, "订单不存在");

        if (request.getServiceItemId() != null) {
            Integer itemCnt = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM t_service_item WHERE deleted = 0 AND id = ? AND status = 1", Integer.class, request.getServiceItemId());
            if (itemCnt == null || itemCnt == 0) throw new BizException(404, 404, "服务项目不存在");
            order.setServiceItemId(request.getServiceItemId());
        }
        if (request.getExpectedTime() != null) order.setExpectedTime(request.getExpectedTime());
        if (request.getAddress() != null) order.setAddress(request.getAddress());
        if (request.getSpecialNote() != null) order.setSpecialNote(request.getSpecialNote());
        if (request.getAssignedStaffId() != null) order.setAssignedStaffId(request.getAssignedStaffId());

        if (request.getStatus() != null) {
            validateTransition(order.getStatus(), request.getStatus());
            order.setStatus(request.getStatus());
        }

        orderMapper.updateById(order);
    }

    private void validateTransition(String from, String to) {
        if (Objects.equals(from, to)) return;
        if ("PENDING".equals(from) && ("CONFIRMED".equals(to) || "CANCELLED".equals(to))) return;
        if ("CONFIRMED".equals(from) && ("IN_PROGRESS".equals(to) || "CANCELLED".equals(to))) return;
        if ("IN_PROGRESS".equals(from) && "COMPLETED".equals(to)) return;
        throw new BizException(400, 400, "状态流转不合法");
    }

    private Map<Long, String> loadNames(String table, String nameCol, List<Long> idsRaw) {
        Map<Long, String> map = new HashMap<>();
        if (idsRaw == null || idsRaw.isEmpty()) return map;
        List<Long> ids = idsRaw.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return map;
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, " + nameCol + " AS n FROM " + table + " WHERE deleted = 0 AND id IN (" + in + ")";
        jdbcTemplate.query(sql, (RowCallbackHandler) rs -> map.put(rs.getLong("id"), rs.getString("n")), ids.toArray());
        return map;
    }
}
