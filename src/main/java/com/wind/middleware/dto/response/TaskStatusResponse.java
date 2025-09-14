package com.wind.middleware.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务状态响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusResponse {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务状态码
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 进度信息
     */
    private ProgressInfo progress;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 进度信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressInfo {
        /**
         * 当前步骤描述
         */
        private String currentStep;

        /**
         * 进度百分比
         */
        private Double percentage;

        /**
         * 预计剩余时间
         */
        private String estimatedRemaining;
    }
}