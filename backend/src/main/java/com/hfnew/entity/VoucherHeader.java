package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_voucher_header")
public class VoucherHeader {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String voucherWord;

    private String voucherNo;

    private LocalDate voucherDate;

    private Integer attachmentCount;

    private String description;

    private String status;

    private String relatedBizType;

    private Long relatedBizId;

    private Long creatorId;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    private String rejectReason;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
