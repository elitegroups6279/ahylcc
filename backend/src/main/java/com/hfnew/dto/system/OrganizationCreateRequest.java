package com.hfnew.dto.system;

import lombok.Data;

/**
 * 创建机构请求
 */
@Data
public class OrganizationCreateRequest {

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
}
