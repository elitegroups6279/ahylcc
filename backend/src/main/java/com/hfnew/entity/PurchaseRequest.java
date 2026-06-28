package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_purchase_request")
public class PurchaseRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestNo;

    private Long applicantId;

    private String applicantName;

    private String itemsJson;

    private BigDecimal totalAmount;

    private String supplyCategory;

    private Long allocationId;

    private Long supplierId;

    private String approvalStatus;

    private Long approverId;

    private String approverName;

    private LocalDateTime approveTime;

    private String approveRemark;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
