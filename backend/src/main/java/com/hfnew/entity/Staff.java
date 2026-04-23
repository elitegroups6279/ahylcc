package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_staff")
public class Staff {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String idCard;

    private Integer gender;

    private LocalDate birthDate;

    private Integer age;

    private String education;

    private String phone;

    private String emergencyContact;

    private String emergencyPhone;

    private LocalDate hireDate;

    private LocalDate resignDate;

    private String resignReason;

    private String jobType;

    private String qualificationUrls;

    private BigDecimal baseSalary;

    private String probationStatus;

    private Integer probationMonths;

    private LocalDate probationEndDate;

    private Integer hasCaregiverCert;

    private Integer hasHealthCert;

    private String positionType;

        @TableField(fill = FieldFill.INSERT)
        private Long orgId;

    private String status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
