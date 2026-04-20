package com.hfnew.dto.pharmacy;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DispenseOrderVO {
    private Long id;
    private Long elderlyId;
    private String elderlyName;
    private LocalDate orderDate;
    private String status;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}
