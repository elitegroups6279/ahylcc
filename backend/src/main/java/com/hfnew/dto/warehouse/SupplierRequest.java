package com.hfnew.dto.warehouse;

import lombok.Data;

@Data
public class SupplierRequest {
    private String name;
    private String creditCode;
    private String contact;
    private String phone;
    private String bankName;
    private String bankAccount;
    private String status;
}
