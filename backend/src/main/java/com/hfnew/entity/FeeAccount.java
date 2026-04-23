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
@TableName("t_fee_account")
public class FeeAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long elderlyId;

    private BigDecimal balance;

    private BigDecimal totalCharged;

    private BigDecimal totalConsumed;

    private BigDecimal carryOver;

    private Integer warningStatus;

        @TableField(fill = FieldFill.INSERT)
        private Long orgId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
