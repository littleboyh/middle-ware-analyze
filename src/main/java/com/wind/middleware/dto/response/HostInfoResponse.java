package com.wind.middleware.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 主机信息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostInfoResponse {

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 服务端IP
     */
    private String serverIp;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 主机名
     */
    private String hostname;

    /**
     * Linux路径
     */
    private String linuxPath;

    /**
     * 访问该服务的应用列表
     */
    private List<ApplicationInfo> applications;

    /**
     * 应用信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationInfo {
        /**
         * 应用名称
         */
        private String appName;

        /**
         * 应用负责人
         */
        private String appOwner;

        /**
         * 负责人域账号
         */
        private String appOwnerAccount;

        /**
         * 部门
         */
        private String department;
    }
}