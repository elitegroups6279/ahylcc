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
    private Long allocationId;

    /** 入库模式: DIRECT直接入库 / FROM_PURCHASE从采购单入库 */
    private String inMode;

    /** 关联验收记录ID (FROM_PURCHASE模式使用) */
    private Long purchaseReceiptId;

    /** 是否同步生成支出记录 */
    private Boolean syncExpense;

    /** 出账银行账户 (syncExpense=true时使用) */
    private Long bankAccountId;
}
