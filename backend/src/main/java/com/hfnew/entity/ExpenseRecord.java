package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_expense_record")
public class ExpenseRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String expenseType;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String payee;
    private String description;
    private Long operatorId;
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
