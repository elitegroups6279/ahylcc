package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.dto.warehouse.SupplierRequest;
import com.hfnew.entity.Supplier;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierMapper supplierMapper;

    public Page<Supplier> list(int page, int size, String keyword) {
        Page<Supplier> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Supplier::getName, keyword).or().like(Supplier::getCreditCode, keyword);
        }
        wrapper.orderByDesc(Supplier::getId);
        return supplierMapper.selectPage(pageReq, wrapper);
    }

    @Transactional
    public Long create(SupplierRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException(400, 400, "供应商名称不能为空");
        }
        Supplier supplier = new Supplier();
        supplier.setName(request.getName());
        supplier.setCreditCode(request.getCreditCode());
        supplier.setContact(request.getContact());
        supplier.setPhone(request.getPhone());
        supplier.setBankName(request.getBankName());
        supplier.setBankAccount(request.getBankAccount());
        supplier.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "ACTIVE");
        supplierMapper.insert(supplier);
        return supplier.getId();
    }

    @Transactional
    public void update(Long id, SupplierRequest request) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BizException(404, 404, "供应商不存在");
        }
        if (StringUtils.hasText(request.getName())) {
            supplier.setName(request.getName());
        }
        if (request.getCreditCode() != null) {
            supplier.setCreditCode(request.getCreditCode());
        }
        if (request.getContact() != null) {
            supplier.setContact(request.getContact());
        }
        if (request.getPhone() != null) {
            supplier.setPhone(request.getPhone());
        }
        if (request.getBankName() != null) {
            supplier.setBankName(request.getBankName());
        }
        if (request.getBankAccount() != null) {
            supplier.setBankAccount(request.getBankAccount());
        }
        if (StringUtils.hasText(request.getStatus())) {
            supplier.setStatus(request.getStatus());
        }
        supplierMapper.updateById(supplier);
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            return;
        }
        supplierMapper.deleteById(id);
    }

    public List<Supplier> options() {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getStatus, "ACTIVE");
        wrapper.orderByAsc(Supplier::getName);
        return supplierMapper.selectList(wrapper);
    }
}
