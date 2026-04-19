package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("t_voucher_entry")
public class VoucherEntry {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long voucherId;

    private Integer lineNo;

    private String summary;

    private Long subjectId;

    private BigDecimal debitAmount;

    private BigDecimal creditAmount;
}
