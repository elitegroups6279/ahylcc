package com.hfnew.dto.finance;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class VoucherHeaderCreateRequest {

    private String voucherWord;

    private LocalDate voucherDate;

    private Integer attachmentCount;

    private String description;

    private List<VoucherEntryRequest> entries;
}
