package com.hfnew.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.ApiResponse;
import com.hfnew.common.PageResult;
import com.hfnew.entity.InventoryIn;
import com.hfnew.entity.InventoryOut;
import com.hfnew.mapper.InventoryInMapper;
import com.hfnew.mapper.InventoryOutMapper;
import com.hfnew.mapper.MaterialMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class FundUsageReportController {

    private final InventoryOutMapper inventoryOutMapper;
    private final InventoryInMapper inventoryInMapper;
    private final MaterialMapper materialMapper;

    @Data
    public static class FundUsageItem {
        private Long outId;
        private String materialName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private String recipientName;
        private LocalDate outDate;
        private String supplyCategory;
    }

    @GetMapping("/fund-usage")
    @PreAuthorize("hasAuthority('warehouse:stock')")
    public ResponseEntity<ApiResponse<PageResult<FundUsageItem>>> listFundUsage(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "CENTRALIZED") String supplyCategory,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        LambdaQueryWrapper<InventoryOut> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(InventoryOut::getOutDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(InventoryOut::getOutDate, LocalDate.parse(endDate));
        }
        if (supplyCategory != null && !supplyCategory.isBlank()) {
            wrapper.eq(InventoryOut::getSupplyCategory, supplyCategory);
        }
        wrapper.orderByDesc(InventoryOut::getOutDate).orderByDesc(InventoryOut::getId);

        Page<InventoryOut> pageReq = new Page<>(page, size);
        IPage<InventoryOut> result = inventoryOutMapper.selectPage(pageReq, wrapper);

        // Load material names
        Set<Long> materialIds = result.getRecords().stream()
                .map(InventoryOut::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> materialNames = new HashMap<>();
        if (!materialIds.isEmpty()) {
            materialMapper.selectBatchIds(materialIds)
                    .forEach(m -> materialNames.put(m.getId(), m.getName()));
        }

        // Load latest unit prices from inventory-in
        Map<Long, BigDecimal> materialPrices = new HashMap<>();
        for (Long matId : materialIds) {
            LambdaQueryWrapper<InventoryIn> priceWrapper = new LambdaQueryWrapper<>();
            priceWrapper.eq(InventoryIn::getMaterialId, matId)
                        .isNotNull(InventoryIn::getUnitPrice)
                        .orderByDesc(InventoryIn::getInDate)
                        .last("LIMIT 1");
            InventoryIn latest = inventoryInMapper.selectOne(priceWrapper);
            if (latest != null && latest.getUnitPrice() != null) {
                materialPrices.put(matId, latest.getUnitPrice());
            }
        }

        List<FundUsageItem> items = result.getRecords().stream().map(out -> {
            FundUsageItem item = new FundUsageItem();
            item.setOutId(out.getId());
            item.setMaterialName(materialNames.getOrDefault(out.getMaterialId(), "未知物资"));
            item.setQuantity(out.getQuantity() != null ? out.getQuantity() : 0);
            BigDecimal price = materialPrices.getOrDefault(out.getMaterialId(), BigDecimal.ZERO);
            item.setUnitPrice(price);
            item.setAmount(price.multiply(BigDecimal.valueOf(item.getQuantity())));
            item.setRecipientName(out.getRecipientName());
            item.setOutDate(out.getOutDate());
            item.setSupplyCategory(out.getSupplyCategory());
            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), items)));
    }
}
