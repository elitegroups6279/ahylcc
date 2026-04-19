package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hfnew.dto.home.ServiceItemOption;
import com.hfnew.entity.ServiceItem;
import com.hfnew.mapper.ServiceItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceItemService {

    private final ServiceItemMapper serviceItemMapper;

    public List<ServiceItemOption> options() {
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceItem::getStatus, 1).orderByAsc(ServiceItem::getId);
        return serviceItemMapper.selectList(wrapper).stream().map(i -> {
            ServiceItemOption opt = new ServiceItemOption();
            opt.setId(i.getId());
            opt.setName(i.getName());
            opt.setCategory(i.getCategory());
            return opt;
        }).collect(Collectors.toList());
    }
}
