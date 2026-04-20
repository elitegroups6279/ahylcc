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
@TableName("t_home_service_record")
public class HomeServiceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private LocalDateTime actualStartTime;

    private LocalDateTime actualEndTime;

    private String serviceContent;

    private String signatureUrl;

    private Integer rating;

    private BigDecimal amount;

    private String paymentStatus;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
