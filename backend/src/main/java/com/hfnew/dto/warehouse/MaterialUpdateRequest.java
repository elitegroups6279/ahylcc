package com.hfnew.dto.warehouse;

import lombok.Data;

@Data
public class MaterialUpdateRequest {
    private String name;
    private String category;
    private String specification;
    private String unit;
    private Integer warningThreshold;
    private String description;
}
