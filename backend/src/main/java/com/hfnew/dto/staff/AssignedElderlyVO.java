package com.hfnew.dto.staff;

import lombok.Data;

@Data
public class AssignedElderlyVO {
    private Long id;
    private String uniqueNo;
    private String name;
    private String bedNumber;
    private String disabilityLevel;
    private String category;
    private String assignType;
}
