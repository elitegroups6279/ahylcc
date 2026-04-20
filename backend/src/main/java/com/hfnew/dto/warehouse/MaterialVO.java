package com.hfnew.dto.warehouse;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialVO {
    private Long id;
    private String name;
    private String category;
    private String specification;
    private String unit;
    private Integer warningThreshold;
    private String description;
    private LocalDateTime createTime;
}
