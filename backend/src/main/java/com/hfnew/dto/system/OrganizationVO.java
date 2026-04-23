package com.hfnew.dto.system;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构视图对象
 */
@Data
public class OrganizationVO {

    private Long id;

    /**
     * 机构编码
     */
    private String orgCode;

    /**
     * 机构名称
     */
    private String orgName;

    /**
     * 地址
     */
    private String address;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 状态：1启用 0停用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
