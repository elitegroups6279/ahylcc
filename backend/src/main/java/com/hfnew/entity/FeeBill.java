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
@TableName("t_fee_bill")
public class FeeBill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long elderlyId;

    private String billMonth;

    private BigDecimal amountDue;

    private BigDecimal amountPaid;

    private BigDecimal carryOverIn;

    private BigDecimal carryOverOut;

    private Integer stayDays;

    private Integer leaveDays;

    private String billingRule;

    private BigDecimal baseFee;

    private BigDecimal longCareAmount;

    private BigDecimal couponDeduct;

    private BigDecimal subsidyAmount;

    private BigDecimal personalSubsidy;

    private BigDecimal familyPayable;

    private BigDecimal govPayable;

    private String subsidyDetail;

    private String status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
