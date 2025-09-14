package com.wind.middleware.service;

import com.wind.middleware.entity.HostInfo;
import com.wind.middleware.entity.Task;
import com.wind.middleware.entity.TaskResult;
import com.wind.middleware.entity.enums.TaskStatus;

import java.util.List;

/**
 * 任务数据服务接口 - 用于解除TaskProcessor对TaskService的直接依赖
 */
public interface TaskDataService {

    /**
     * 根据ID获取任务
     */
    Task getTaskById(String taskId);

    /**
     * 更新任务状态
     */
    boolean updateTaskStatus(String taskId, TaskStatus status, String errorMessage);

    /**
     * 保存任务结果
     */
    void saveTaskResults(List<TaskResult> taskResults);

    /**
     * 保存主机信息
     */
    void saveHostInfo(List<HostInfo> hostInfoList);
}