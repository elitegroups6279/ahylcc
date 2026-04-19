package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_bed_transfer")
public class BedTransfer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long elderlyId;

    private Long fromBedId;

    private Long toBedId;

    private LocalDate transferDate;

    private String reason;

    private Long operatorId;

    private LocalDateTime createTime;
}
