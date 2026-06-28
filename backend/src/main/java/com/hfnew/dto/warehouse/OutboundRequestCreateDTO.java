package com.hfnew.dto.warehouse;

import lombok.Data;

@Data
public class OutboundRequestCreateDTO {
    private Long materialId;
    private Integer quantity;
    private String supplyCategory;
    private String department;
    private String purpose;
    private Long recipientStaffId;
    private String recipientName;
    private String remark;
}
