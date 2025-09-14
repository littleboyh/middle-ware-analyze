package com.wind.middleware.event;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 任务提交事件
 */
@Data
@AllArgsConstructor
public class TaskSubmittedEvent {

    private String taskId;
}