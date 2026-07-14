package com.hfnew.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@TableName("t_medication_plan")
public class MedicationPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long elderlyId;

    private Long drugId;

    private String drugName;

    private String dosage;

    private String dosageUnit;

    private String frequencyType;

    private Integer intervalDays;

    private String timeSlots;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal totalQuantity;

    private String dosagePerTime;

    private Integer timesPerDay;

    private LocalDate depletionDate;

    private String instructions;

    private String prescriberName;

    private String status;

    private Long operatorId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long orgId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    public List<String> getTimeSlotsAsList() {
        if (timeSlots == null || timeSlots.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(timeSlots, STRING_LIST_TYPE);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
