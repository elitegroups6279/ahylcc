package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_elderly_change_log")
public class ElderlyChangeLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderlyId;
    private String fieldName;
    private String fieldLabel;
    private String oldValue;
    private String newValue;
    private String operator;
    private LocalDateTime createTime;
}
