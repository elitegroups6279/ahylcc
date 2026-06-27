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
@TableName("t_wubao_allocation")
public class WubaoAllocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String allocateMonth;

    private Integer elderCount;

    private BigDecimal livingFeePerPerson;

    private BigDecimal careFeePerPerson;

    private BigDecimal totalAmount;

    private String counterparty;

    private String receiptNo;

    private Long paymentRecordId;

    private Long bankTransactionId;

    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private Long orgId;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
