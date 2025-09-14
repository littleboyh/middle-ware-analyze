package com.wind.middleware.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 主机信息实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_host_info")
public class HostInfo {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 客户端IP
     */
    @TableField("client_ip")
    private String clientIp;

    /**
     * 服务端IP
     */
    @TableField("server_ip")
    private String serverIp;

    /**
     * 端口
     */
    @TableField("port")
    private Integer port;

    /**
     * 主机名
     */
    @TableField("hostname")
    private String hostname;

    /**
     * Linux路径
     */
    @TableField("linux_path")
    private String linuxPath;

    /**
     * 应用名称
     */
    @TableField("app_name")
    private String appName;

    /**
     * 应用负责人
     */
    @TableField("app_owner")
    private String appOwner;

    /**
     * 负责人域账号
     */
    @TableField("app_owner_account")
    private String appOwnerAccount;

    /**
     * 部门
     */
    @TableField("department")
    private String department;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}