package com.hfnew.dto.elderly;

import lombok.Data;

@Data
public class ElderlyContactDTO {
    private Long id;
    private String name;
    private String relationship;
    private String phone;
    private Integer isEmergency;
    private Integer sortOrder;
}
