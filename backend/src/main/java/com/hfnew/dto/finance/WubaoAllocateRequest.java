package com.hfnew.dto.finance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WubaoAllocateRequest {

    @NotBlank
    private String allocateMonth; // YYYY-MM format

    private String counterparty; // 对方户名, default "民政局"

    private String receiptNo; // 银行回单号

    private String remark;
}
