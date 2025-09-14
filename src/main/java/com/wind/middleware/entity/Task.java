package com.wind.middleware.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.wind.middleware.entity.enums.TaskStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_task")
public class Task {

    /**
     * 任务ID
     */
    @TableId(value = "task_id", type = IdType.INPUT)
    private String taskId;

    /**
     * 提交人
     */
    @TableField("submitter")
    private String submitter;

    /**
     * 任务描述
     */
    @TableField("description")
    private String description;

    /**
     * 服务端IP列表
     */
    @TableField(value = "server_ips", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> serverIps;

    /**
     * 服务端端口
     */
    @TableField("port")
    private Integer port;

    /**
     * 查询开始日期
     */
    @TableField("start_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 查询结束日期
     */
    @TableField("end_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 任务状态
     */
    @TableField("status")
    private TaskStatus status;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}