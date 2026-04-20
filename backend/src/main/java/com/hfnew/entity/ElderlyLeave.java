package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_elderly_leave")
public class ElderlyLeave {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderlyId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String status; // ON_LEAVE, RETURNED, CANCELLED
    private LocalDate returnDate;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
