package com.wind.middleware.dto.external;

import lombok.Data;

/**
 * 机器信息DTO
 */
@Data
public class MachineInfo {

    /**
     * 机器名字
     */
    private String machineName;

    /**
     * 内网IP
     */
    private String internalIp;

    /**
     * 外网IP
     */
    private String externalIp;
}