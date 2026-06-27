package com.hfnew.dto.warehouse;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InventoryInCreateRequest {
    private Long materialId;
    private String supplier;
    private String purchaseOrderNo;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private LocalDate inDate;
    private String remark;
    private String supplyCategory;

    /** 是否同步生成支出记录 */
    private Boolean syncExpense;

    /** 出账银行账户 (syncExpense=true时使用) */
    private Long bankAccountId;
}
