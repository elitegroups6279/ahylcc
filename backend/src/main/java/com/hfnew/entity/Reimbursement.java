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
@TableName("t_reimbursement")
public class Reimbursement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicantId;

    private BigDecimal amount;

    private String reason;

    private String attachmentUrls;

    private String status;

    private Long approverId;

    private LocalDateTime approveTime;

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
