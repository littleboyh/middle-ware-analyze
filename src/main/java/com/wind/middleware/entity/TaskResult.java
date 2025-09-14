package com.wind.middleware.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务结果实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_task_result")
public class TaskResult {

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
     * 访问次数
     */
    @TableField("access_count")
    private Long accessCount;

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