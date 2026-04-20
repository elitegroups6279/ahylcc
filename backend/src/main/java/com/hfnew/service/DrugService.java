package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.pharmacy.DrugCreateRequest;
import com.hfnew.dto.pharmacy.DrugUpdateRequest;
import com.hfnew.dto.pharmacy.DrugVO;
import com.hfnew.entity.Drug;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.DrugMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DrugService {

    private final DrugMapper drugMapper;

    public PageResult<DrugVO> list(int page, int pageSize, String keyword) {
        Page<Drug> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<Drug> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Drug::getName, keyword).or().like(Drug::getGenericName, keyword);
        }
        wrapper.orderByDesc(Drug::getId);
        IPage<Drug> result = drugMapper.selectPage(pageReq, wrapper);
        List<DrugVO> list = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long create(DrugCreateRequest request) {
        if (!StringUtils.hasText(request.getName())) throw new BizException(400, 400, "药品名称不能为空");
        Drug d = new Drug();
        d.setName(request.getName());
        d.setGenericName(request.getGenericName());
        d.setSpecification(request.getSpecification());
        d.setDosageForm(request.getDosageForm());
        d.setManufacturer(request.getManufacturer());
        d.setApprovalNumber(request.getApprovalNumber());
        d.setStorageCondition(request.getStorageCondition());
        d.setIsPrescription(request.getIsPrescription() == null ? 0 : request.getIsPrescription());
        d.setWarningDays(request.getWarningDays() == null ? 30 : request.getWarningDays());
        d.setDescription(request.getDescription());
        drugMapper.insert(d);
        return d.getId();
    }

    @Transactional
    public void update(Long id, DrugUpdateRequest request) {
        Drug d = drugMapper.selectById(id);
        if (d == null) throw new BizException(404, 404, "药品不存在");
        if (request.getName() != null) d.setName(request.getName());
        if (request.getGenericName() != null) d.setGenericName(request.getGenericName());
        if (request.getSpecification() != null) d.setSpecification(request.getSpecification());
        if (request.getDosageForm() != null) d.setDosageForm(request.getDosageForm());
        if (request.getManufacturer() != null) d.setManufacturer(request.getManufacturer());
        if (request.getApprovalNumber() != null) d.setApprovalNumber(request.getApprovalNumber());
        if (request.getStorageCondition() != null) d.setStorageCondition(request.getStorageCondition());
        if (request.getIsPrescription() != null) d.setIsPrescription(request.getIsPrescription());
        if (request.getWarningDays() != null) d.setWarningDays(request.getWarningDays());
        if (request.getDescription() != null) d.setDescription(request.getDescription());
        drugMapper.updateById(d);
    }

    @Transactional
    public void delete(Long id) {
        Drug d = drugMapper.selectById(id);
        if (d == null) return;
        drugMapper.deleteById(id);
    }

    private DrugVO toVO(Drug d) {
        DrugVO vo = new DrugVO();
        vo.setId(d.getId());
        vo.setName(d.getName());
        vo.setGenericName(d.getGenericName());
        vo.setSpecification(d.getSpecification());
        vo.setDosageForm(d.getDosageForm());
        vo.setManufacturer(d.getManufacturer());
        vo.setApprovalNumber(d.getApprovalNumber());
        vo.setStorageCondition(d.getStorageCondition());
        vo.setIsPrescription(d.getIsPrescription());
        vo.setWarningDays(d.getWarningDays());
        vo.setDescription(d.getDescription());
        vo.setCreateTime(d.getCreateTime());
        return vo;
    }
}
