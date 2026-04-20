package com.hfnew.dto.elderly;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ElderlyTransferRequest {
    private Long toBedId;
    private String customBedNumber;
    private LocalDate transferDate;
    private String reason;
}
