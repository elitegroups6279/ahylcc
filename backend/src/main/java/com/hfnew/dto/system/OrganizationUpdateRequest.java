package com.hfnew.dto.system;

import lombok.Data;

/**
 * 更新机构请求
 */
@Data
public class OrganizationUpdateRequest {

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
}
