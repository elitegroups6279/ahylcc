package com.hfnew.dto.pharmacy;

import lombok.Data;

@Data
public class DrugCreateRequest {
    private String name;
    private String genericName;
    private String specification;
    private String dosageForm;
    private String manufacturer;
    private String approvalNumber;
    private String storageCondition;
    private Integer isPrescription;
    private Integer warningDays;
    private String description;
}
