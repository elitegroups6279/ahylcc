package com.hfnew.dto.finance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BankAccountCreateRequest {
    @NotBlank(message = "账户名称不能为空")
    private String accountName;

    @NotBlank(message = "账户类型不能为空")
    private String accountType;

    private String bankName;
    private String accountNumber;
    private BigDecimal initialBalance;
}
