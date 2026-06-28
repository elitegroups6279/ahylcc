package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.entity.InventoryBatch;
import com.hfnew.mapper.InventoryBatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryBatchService {

    private final InventoryBatchMapper inventoryBatchMapper;

    public PageResult<InventoryBatch> list(int page, int size, Long materialId, String supplyCategory) {
        Page<InventoryBatch> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<InventoryBatch> wrapper = new LambdaQueryWrapper<>();
        if (materialId != null) {
            wrapper.eq(InventoryBatch::getMaterialId, materialId);
        }
        if (supplyCategory != null && !supplyCategory.isBlank()) {
            wrapper.eq(InventoryBatch::getSupplyCategory, supplyCategory);
        }
        wrapper.orderByAsc(InventoryBatch::getExpiryDate);
        IPage<InventoryBatch> result = inventoryBatchMapper.selectPage(pageReq, wrapper);
        return PageResult.from(result);
    }

    @Transactional
    public Long create(InventoryBatch batch) {
        if (batch.getRemainingQuantity() == null) {
            batch.setRemainingQuantity(batch.getQuantity());
        }
        inventoryBatchMapper.insert(batch);
        return batch.getId();
    }

    public List<InventoryBatch> getNearExpiry(int days) {
        LocalDate now = LocalDate.now();
        LocalDate deadline = now.plusDays(days);
        LambdaQueryWrapper<InventoryBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(InventoryBatch::getExpiryDate, now)
               .le(InventoryBatch::getExpiryDate, deadline)
               .gt(InventoryBatch::getRemainingQuantity, 0)
               .orderByAsc(InventoryBatch::getExpiryDate);
        return inventoryBatchMapper.selectList(wrapper);
    }
}
