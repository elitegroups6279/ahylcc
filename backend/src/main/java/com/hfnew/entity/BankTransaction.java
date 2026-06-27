package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_bank_transaction")
public class BankTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bankAccountId;

    private String transactionType;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private String counterparty;

    private LocalDate transactionDate;

    private String bizType;

    private Long bizId;

    private String description;

    private String receiptNo;

    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private Long orgId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
