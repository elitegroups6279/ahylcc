package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_outbound_request")
public class OutboundRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicantId;

    private String applicantName;

    private Long materialId;

    private String materialName;

    private Integer quantity;

    private String supplyCategory;

    private String department;

    private String purpose;

    private Long recipientStaffId;

    private String recipientName;

    private String approvalStatus;

    private Long approverId;

    private String approverName;

    private LocalDateTime approveTime;

    private String approveRemark;

    private Long inventoryOutId;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
