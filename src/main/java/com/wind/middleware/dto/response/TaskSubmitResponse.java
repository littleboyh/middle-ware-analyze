package com.wind.middleware.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务提交响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务提交响应")
public class TaskSubmitResponse {

    /**
     * 任务ID
     */
    @Schema(description = "任务ID", example = "task_1700123456789_abc12345")
    private String taskId;

    /**
     * 任务状态
     */
    @Schema(description = "任务状态", example = "SUBMITTED")
    private String status;

    /**
     * 预计处理时间
     */
    @Schema(description = "预计处理时间", example = "预计10-15分钟完成")
    private String estimatedTime;
}