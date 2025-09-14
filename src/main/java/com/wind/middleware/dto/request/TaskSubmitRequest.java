package com.wind.middleware.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 任务提交请求DTO
 */
@Data
@Schema(description = "任务提交请求")
public class TaskSubmitRequest {

    /**
     * 提交人
     */
    @Schema(description = "提交人", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "提交人不能为空")
    @Size(max = 100, message = "提交人长度不能超过100字符")
    private String submitter;

    /**
     * 任务描述
     */
    @Schema(description = "任务描述", example = "MySQL集群访问分析")
    @Size(max = 500, message = "任务描述长度不能超过500字符")
    private String description;

    /**
     * 服务端IP列表
     */
    @Schema(description = "服务端IP列表", example = "[\"10.106.60.172\", \"10.106.60.173\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "服务端IP列表不能为空")
    @Size(min = 1, max = 50, message = "服务端IP数量必须在1-50个之间")
    private List<@NotBlank(message = "IP地址不能为空") String> serverIps;

    /**
     * 服务端端口
     */
    @Schema(description = "服务端端口", example = "3306", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "端口不能为空")
    @Min(value = 1, message = "端口号不能小于1")
    @Max(value = 65535, message = "端口号不能大于65535")
    private Integer port;

    /**
     * 查询开始日期
     */
    @Schema(description = "查询开始日期", example = "2025-08-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 查询结束日期
     */
    @Schema(description = "查询结束日期", example = "2025-08-07", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}