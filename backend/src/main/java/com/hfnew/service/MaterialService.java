package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.warehouse.MaterialCreateRequest;
import com.hfnew.dto.warehouse.MaterialUpdateRequest;
import com.hfnew.dto.warehouse.MaterialVO;
import com.hfnew.entity.Material;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.MaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialMapper materialMapper;

    public PageResult<MaterialVO> list(int page, int pageSize, String keyword, String category) {
        Page<Material> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Material::getName, keyword).or().like(Material::getSpecification, keyword);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Material::getCategory, category);
        }
        wrapper.orderByDesc(Material::getId);
        IPage<Material> result = materialMapper.selectPage(pageReq, wrapper);
        List<MaterialVO> list = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public MaterialVO getById(Long id) {
        Material m = materialMapper.selectById(id);
        if (m == null) throw new BizException(404, 404, "物资不存在");
        return toVO(m);
    }

    @Transactional
    public Long create(MaterialCreateRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException(400, 400, "物资名称不能为空");
        }
        Material m = new Material();
        m.setName(request.getName());
        m.setCategory(request.getCategory());
        m.setSpecification(request.getSpecification());
        m.setUnit(request.getUnit());
        m.setWarningThreshold(request.getWarningThreshold() == null ? 10 : request.getWarningThreshold());
        m.setDescription(request.getDescription());
        materialMapper.insert(m);
        return m.getId();
    }

    @Transactional
    public void update(Long id, MaterialUpdateRequest request) {
        Material m = materialMapper.selectById(id);
        if (m == null) throw new BizException(404, 404, "物资不存在");
        if (request.getName() != null) m.setName(request.getName());
        if (request.getCategory() != null) m.setCategory(request.getCategory());
        if (request.getSpecification() != null) m.setSpecification(request.getSpecification());
        if (request.getUnit() != null) m.setUnit(request.getUnit());
        if (request.getWarningThreshold() != null) m.setWarningThreshold(request.getWarningThreshold());
        if (request.getDescription() != null) m.setDescription(request.getDescription());
        materialMapper.updateById(m);
    }

    @Transactional
    public void delete(Long id) {
        Material m = materialMapper.selectById(id);
        if (m == null) return;
        materialMapper.deleteById(id);
    }

    private MaterialVO toVO(Material m) {
        MaterialVO vo = new MaterialVO();
        vo.setId(m.getId());
        vo.setName(m.getName());
        vo.setCategory(m.getCategory());
        vo.setSpecification(m.getSpecification());
        vo.setUnit(m.getUnit());
        vo.setWarningThreshold(m.getWarningThreshold());
        vo.setDescription(m.getDescription());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }
}
