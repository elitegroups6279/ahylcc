package com.hfnew.dto.pharmacy;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DispenseOrderCreateRequest {
    private Long elderlyId;
    private LocalDate orderDate;
    private String remark;
}
