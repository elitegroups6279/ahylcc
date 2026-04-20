package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hfnew.dto.elderly.BedOption;
import com.hfnew.entity.Bed;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.BedMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    /**
     * Find or create a bed by custom bed number string.
     * The customBedNumber can be in formats like "301-2", "A-301-2", etc.
     * If the bed doesn't exist, it will be created with the custom number as bedNumber.
     */
    @Transactional
    public Bed findOrCreateByCustomNumber(String customBedNumber) {
        if (!StringUtils.hasText(customBedNumber)) {
            throw new BizException(400, 400, "床位号不能为空");
        }
        String trimmed = customBedNumber.trim();
        
        // Try to find existing bed by bedNumber
        LambdaQueryWrapper<Bed> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bed::getBedNumber, trimmed);
        Bed existing = bedMapper.selectOne(wrapper);
        if (existing != null) {
            return existing;
        }
        
        // Create new bed with the custom number
        Bed newBed = new Bed();
        // The bed_number stores the FULL custom number as the display identifier (e.g., "201-1")
        newBed.setBedNumber(trimmed);
        // Parse the custom number to extract building/floor/room if possible
        // Format examples: "301-2" (room-bed), "A-301-2" (building-room-bed), "A-3-301-2" (building-floor-room-bed)
        String[] parts = trimmed.split("-");
        if (parts.length >= 2) {
            // Second last part is room number
            newBed.setRoomNumber(parts[parts.length - 2]);
            if (parts.length >= 3) {
                newBed.setFloor(parts[parts.length - 3]);
            }
            if (parts.length >= 4) {
                newBed.setBuilding(parts[0]);
            }
        }
        newBed.setStatus(0); // Available
        bedMapper.insert(newBed);
        return newBed;
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
