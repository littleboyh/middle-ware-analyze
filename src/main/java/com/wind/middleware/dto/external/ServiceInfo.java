package com.wind.middleware.dto.external;

import lombok.Data;

/**
 * 服务信息DTO
 */
@Data
public class ServiceInfo {

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 应用负责人中文名
     */
    private String ownerName;

    /**
     * 应用负责人域账号
     */
    private String ownerAccount;

    /**
     * 部门
     */
    private String department;
}