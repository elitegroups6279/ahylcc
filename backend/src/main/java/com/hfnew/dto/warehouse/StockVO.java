package com.hfnew.dto.warehouse;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockVO {
    private Long stockId;
    private Long materialId;
    private String materialName;
    private String category;
    private String specification;
    private String unit;
    private Integer warningThreshold;
    private Integer quantity;
    private BigDecimal totalValue;
    private Integer warning;
}
