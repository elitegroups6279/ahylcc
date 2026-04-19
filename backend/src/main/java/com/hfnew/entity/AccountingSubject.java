package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_accounting_subject")
public class AccountingSubject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String subjectType;

    private String direction;

    private Integer enabled;

    private Integer sortOrder;
}
