package com.wind.middleware.dto.external;

import lombok.Data;

import java.util.List;

/**
 * 主机详细信息DTO
 */
@Data
public class HostDetailInfo {

    /**
     * Linux路径
     */
    private String linuxPath;

    /**
     * Client端主机名
     */
    private String clientHostname;

    /**
     * 访问该服务的应用列表
     */
    private List<String> applications;
}