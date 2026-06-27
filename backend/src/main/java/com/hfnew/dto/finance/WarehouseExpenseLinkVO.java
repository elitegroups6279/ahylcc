package com.hfnew.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WarehouseExpenseLinkVO {
    private Long inventoryInId;
    private String materialName;
    private String supplier;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private LocalDate inDate;
    private Long expenseRecordId;
    private Long bankAccountId;
    /** 基本户 or 一般户 */
    private String bankAccountName;
}
