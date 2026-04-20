package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hfnew.dto.elderly.BedOption;
import com.hfnew.entity.Bed;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.BedMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BedService {

    private final BedMapper bedMapper;

    public List<BedOption> listAvailable() {
        LambdaQueryWrapper<Bed> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bed::getStatus, 0)
                .orderByAsc(Bed::getBuilding)
                .orderByAsc(Bed::getFloor)
                .orderByAsc(Bed::getRoomNumber)
                .orderByAsc(Bed::getBedNumber);
        return bedMapper.selectList(wrapper).stream().map(this::toOption).collect(Collectors.toList());
    }

    public Bed getById(Long id) {
        Bed bed = bedMapper.selectById(id);
        if (bed == null) {
            throw new BizException(404, 404, "床位不存在");
        }
        return bed;
    }

    @Transactional
    public void occupy(Long bedId) {
        Bed bed = getById(bedId);
        if (bed.getStatus() != null && bed.getStatus() != 0) {
            throw new BizException(400, 400, "床位不可用");
        }
        bed.setStatus(1);
        bedMapper.updateById(bed);
    }

    @Transactional
    public void release(Long bedId) {
        if (bedId == null) return;
        Bed bed = getById(bedId);
        bed.setStatus(0);
        bedMapper.updateById(bed);
    }

    private BedOption toOption(Bed bed) {
        BedOption opt = new BedOption();
        opt.setId(bed.getId());
        opt.setBuilding(bed.getBuilding());
        opt.setFloor(bed.getFloor());
        opt.setRoomNumber(bed.getRoomNumber());
        opt.setBedNumber(bed.getBedNumber());
        return opt;
    }
}
