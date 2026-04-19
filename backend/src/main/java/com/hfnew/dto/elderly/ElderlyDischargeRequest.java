package com.hfnew.dto.elderly;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ElderlyDischargeRequest {
    private LocalDate dischargeDate;
    private String dischargeReason;
}
