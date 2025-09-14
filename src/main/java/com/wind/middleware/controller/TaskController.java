package com.wind.middleware.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wind.middleware.dto.request.TaskSubmitRequest;
import com.wind.middleware.dto.response.HostInfoResponse;
import com.wind.middleware.dto.response.TaskStatusResponse;
import com.wind.middleware.dto.response.TaskSubmitResponse;
import com.wind.middleware.entity.HostInfo;
import com.wind.middleware.entity.Task;
import com.wind.middleware.entity.TaskResult;
import com.wind.middleware.entity.enums.TaskStatus;
import com.wind.middleware.exception.BusinessException;
import com.wind.middleware.service.TaskService;
import com.wind.middleware.queue.TaskQueueManager;
import com.wind.middleware.dto.response.MyApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "任务管理", description = "中间件分析任务的创建、查询、状态监控和管理相关接口")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskQueueManager taskQueueManager;

    /**
     * 提交分析任务
     */
    @Operation(
        summary = "提交分析任务",
        description = "提交中间件网络流量分析任务，系统将异步处理ES查询和外部API调用"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "任务提交成功",
            content = @Content(schema = @Schema(implementation = TaskSubmitResponse.class),
                examples = @ExampleObject(value = """
                {
                    "code": 200,
                    "message": "任务提交成功",
                    "data": {
                        "taskId": "task_1700123456789_abc12345",
                        "status": "SUBMITTED",
                        "estimatedTime": "预计10-15分钟完成"
                    }
                }
                """))),
        @ApiResponse(responseCode = "400", description = "参数校验失败",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "code": 400,
                    "message": "查询时间范围不能超过30天",
                    "data": null
                }
                """))),
        @ApiResponse(responseCode = "500", description = "系统内部错误")
    })
    @PostMapping
    public MyApiResponse<TaskSubmitResponse> submitTask(
            @Parameter(description = "任务提交请求参数", required = true)
            @Valid @RequestBody TaskSubmitRequest request) {
        try {
            // 提交任务
            String taskId = taskService.submitTask(
                    request.getSubmitter(),
                    request.getDescription(),
                    request.getServerIps(),
                    request.getPort(),
                    request.getStartDate(),
                    request.getEndDate()
            );

            // 计算预计处理时间
            long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
            String estimatedTime = calculateEstimatedTime(days, request.getServerIps().size());

            TaskSubmitResponse response = TaskSubmitResponse.builder()
                    .taskId(taskId)
                    .status(TaskStatus.SUBMITTED.name())
                    .estimatedTime(estimatedTime)
                    .build();

            log.info("任务提交成功: {}", taskId);
            return MyApiResponse.success("任务提交成功", response);
        } catch (IllegalArgumentException e) {
            log.warn("任务提交参数错误: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("任务提交失败", e);
            throw new BusinessException("任务提交失败: " + e.getMessage());
        }
    }

    /**
     * 任务列表查询（支持筛选和分页）
     */
    @Operation(
        summary = "任务列表查询",
        description = "分页查询任务列表，支持按提交人和任务描述进行筛选"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(value = """
                {
                    "code": 200,
                    "message": "成功",
                    "data": {
                        "records": [
                            {
                                "taskId": "task_1700123456789_abc12345",
                                "submitter": "张三",
                                "description": "MySQL集群访问分析",
                                "serverIps": ["10.106.60.172", "10.106.60.173"],
                                "port": 3306,
                                "startDate": "2025-08-01",
                                "endDate": "2025-08-07",
                                "status": "COMPLETED",
                                "createTime": "2025-09-14 10:30:00",
                                "updateTime": "2025-09-14 10:45:00"
                            }
                        ],
                        "total": 25,
                        "size": 10,
                        "current": 1,
                        "pages": 3
                    }
                }
                """))),
        @ApiResponse(responseCode = "500", description = "系统内部错误")
    })
    @GetMapping
    public MyApiResponse<Page<Task>> queryTasks(
            @Parameter(description = "页码，从0开始", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页数量，最大100", example = "10")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "排序字段", schema = @Schema(allowableValues = {"createTime", "updateTime"}))
            @RequestParam(defaultValue = "createTime") String sort,
            @Parameter(description = "提交人筛选，支持模糊匹配", example = "张三")
            @RequestParam(required = false) String submitter,
            @Parameter(description = "任务描述筛选，支持模糊匹配", example = "MySQL")
            @RequestParam(required = false) String description) {
        try {
            Page<Task> result = taskService.queryTasks(page, size, sort, submitter, description);
            return MyApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询任务列表失败", e);
            throw new BusinessException("查询任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 全量任务查询
     */
    @Operation(
        summary = "全量任务查询",
        description = "查询所有任务，默认按创建时间倒序排列，不进行条件筛选"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "系统内部错误")
    })
    @GetMapping("/all")
    public MyApiResponse<Page<Task>> queryAllTasks(
            @Parameter(description = "页码，从0开始", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页数量，默认20，最大100", example = "20")
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "排序字段", schema = @Schema(allowableValues = {"createTime", "updateTime"}))
            @RequestParam(defaultValue = "createTime") String sort) {
        try {
            Page<Task> result = taskService.queryAllTasks(page, size, sort);
            return MyApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询全量任务列表失败", e);
            throw new BusinessException("查询任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 任务状态查询
     */
    @Operation(
        summary = "任务状态查询",
        description = "查询指定任务的执行状态和进度信息，包含实时处理进度"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = TaskStatusResponse.class),
                examples = @ExampleObject(value = """
                {
                    "code": 200,
                    "message": "查询成功",
                    "data": {
                        "taskId": "task_1700123456789_abc12345",
                        "status": "ES_QUERYING",
                        "statusDesc": "ES查询中",
                        "progress": {
                            "currentStep": "正在从Elasticsearch查询数据",
                            "percentage": 25.0,
                            "estimatedRemaining": "预计还需5-15分钟"
                        },
                        "startTime": "2025-09-14 10:30:00",
                        "updateTime": "2025-09-14 10:32:15",
                        "errorMessage": null
                    }
                }
                """))),
        @ApiResponse(responseCode = "404", description = "任务不存在"),
        @ApiResponse(responseCode = "500", description = "系统内部错误")
    })
    @GetMapping("/status")
    public MyApiResponse<TaskStatusResponse> getTaskStatus(
            @Parameter(description = "任务ID", required = true, example = "task_1700123456789_abc12345")
            @RequestParam String taskId) {
        try {
            Task task = taskService.getTaskById(taskId);
            if (task == null) {
                throw BusinessException.notFound("任务不存在");
            }

            // 构建进度信息
            TaskStatusResponse.ProgressInfo progress = buildProgressInfo(task);

            TaskStatusResponse response = TaskStatusResponse.builder()
                    .taskId(task.getTaskId())
                    .status(task.getStatus().name())
                    .statusDesc(task.getStatus().getDescription())
                    .progress(progress)
                    .startTime(task.getCreateTime())
                    .updateTime(task.getUpdateTime())
                    .errorMessage(task.getErrorMessage())
                    .build();

            return MyApiResponse.success(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询任务状态失败: {}", taskId, e);
            throw new BusinessException("查询任务状态失败: " + e.getMessage());
        }
    }

    /**
     * 客户端IP访问结果查询
     */
    @Operation(
        summary = "客户端IP访问结果查询",
        description = "查询任务的ES查询结果，即访问指定服务的客户端IP列表和访问统计"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "code": 200,
                    "message": "成功",
                    "data": [
                        {
                            "clientIp": "10.100.6.218",
                            "serverIp": "10.106.60.172",
                            "port": 3306,
                            "accessCount": 1245
                        },
                        {
                            "clientIp": "10.100.6.219",
                            "serverIp": "10.106.60.173",
                            "port": 3306,
                            "accessCount": 892
                        }
                    ]
                }
                """))),
        @ApiResponse(responseCode = "404", description = "任务不存在"),
        @ApiResponse(responseCode = "500", description = "系统内部错误")
    })
    @GetMapping("/client-ips")
    public MyApiResponse<List<TaskResult>> getClientIps(
            @Parameter(description = "任务ID", required = true, example = "task_1700123456789_abc12345")
            @RequestParam String taskId) {
        try {
            Task task = taskService.getTaskById(taskId);
            if (task == null) {
                throw BusinessException.notFound("任务不存在");
            }

            List<TaskResult> results = taskService.getTaskResults(taskId);
            return MyApiResponse.success(results);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询客户端IP结果失败: {}", taskId, e);
            throw new BusinessException("查询客户端IP结果失败: " + e.getMessage());
        }
    }

    /**
     * 完整主机信息查询
     */
    @Operation(
        summary = "完整主机信息查询",
        description = "查询任务的完整结果，包括客户端IP、主机信息、应用信息等详细数据"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "code": 200,
                    "message": "成功",
                    "data": [
                        {
                            "clientIp": "10.100.6.218",
                            "serverIp": "10.106.60.172",
                            "port": 3306,
                            "hostname": "app-server-01",
                            "linuxPath": "/opt/applications/user-service",
                            "applications": [
                                {
                                    "appName": "用户服务",
                                    "appOwner": "张三",
                                    "appOwnerAccount": "zhangsan",
                                    "department": "用户中心"
                                }
                            ]
                        }
                    ]
                }
                """))),
        @ApiResponse(responseCode = "404", description = "任务不存在"),
        @ApiResponse(responseCode = "500", description = "系统内部错误")
    })
    @GetMapping("/host-info")
    public MyApiResponse<List<HostInfoResponse>> getHostInfo(
            @Parameter(description = "任务ID", required = true, example = "task_1700123456789_abc12345")
            @RequestParam String taskId) {
        try {
            Task task = taskService.getTaskById(taskId);
            if (task == null) {
                throw BusinessException.notFound("任务不存在");
            }

            List<HostInfo> hostInfoList = taskService.getHostInfo(taskId);
            List<HostInfoResponse> responses = buildHostInfoResponse(hostInfoList);

            return MyApiResponse.success(responses);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询主机信息失败: {}", taskId, e);
            throw new BusinessException("查询主机信息失败: " + e.getMessage());
        }
    }

    /**
     * 任务删除
     */
    @Operation(
        summary = "任务删除",
        description = "删除指定任务及其相关数据（逻辑删除），只能删除已完成或失败的任务"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "任务删除成功",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "code": 200,
                    "message": "成功",
                    "data": null
                }
                """))),
        @ApiResponse(responseCode = "400", description = "业务规则限制",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "code": 400,
                    "message": "只能删除已完成或失败的任务",
                    "data": null
                }
                """))),
        @ApiResponse(responseCode = "404", description = "任务不存在"),
        @ApiResponse(responseCode = "500", description = "系统内部错误")
    })
    @DeleteMapping
    public MyApiResponse<Void> deleteTask(
            @Parameter(description = "要删除的任务ID", required = true, example = "task_1700123456789_abc12345")
            @RequestParam String taskId) {
        try {
            boolean deleted = taskService.deleteTask(taskId);
            if (deleted) {
                log.info("任务删除成功: {}", taskId);
                return MyApiResponse.success();
            } else {
                throw new BusinessException("任务删除失败");
            }
        } catch (BusinessException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除任务失败: {}", taskId, e);
            throw new BusinessException("删除任务失败: " + e.getMessage());
        }
    }

    /**
     * 计算预计处理时间
     */
    private String calculateEstimatedTime(long days, int serverIpCount) {
        // 简单的时间估算逻辑：每天每个IP大约需要10秒
        long estimatedSeconds = days * serverIpCount * 10;
        if (estimatedSeconds < 60) {
            return "预计1分钟内完成";
        } else if (estimatedSeconds < 600) {
            return "预计" + (estimatedSeconds / 60) + "-" + ((estimatedSeconds / 60) + 1) + "分钟完成";
        } else {
            return "预计10-30分钟完成";
        }
    }

    /**
     * 构建进度信息
     */
    private TaskStatusResponse.ProgressInfo buildProgressInfo(Task task) {
        String currentStep;
        Double percentage;
        String estimatedRemaining;

        switch (task.getStatus()) {
            case SUBMITTED:
                currentStep = "任务已提交，等待处理";
                percentage = 0.0;
                estimatedRemaining = "等待中";
                break;
            case ES_QUERYING:
                currentStep = "正在从Elasticsearch查询数据";
                percentage = 25.0;
                estimatedRemaining = "预计还需5-15分钟";
                break;
            case ES_COMPLETED:
                currentStep = "ES查询完成，准备调用外部API";
                percentage = 50.0;
                estimatedRemaining = "预计还需3-8分钟";
                break;
            case API_CALLING:
                currentStep = "正在调用外部API获取详细信息";
                percentage = 75.0;
                estimatedRemaining = "预计还需2-5分钟";
                break;
            case COMPLETED:
                currentStep = "任务处理完成";
                percentage = 100.0;
                estimatedRemaining = "已完成";
                break;
            case FAILED:
                currentStep = "任务处理失败";
                percentage = null;
                estimatedRemaining = "已失败";
                break;
            default:
                currentStep = "未知状态";
                percentage = null;
                estimatedRemaining = "未知";
        }

        return TaskStatusResponse.ProgressInfo.builder()
                .currentStep(currentStep)
                .percentage(percentage)
                .estimatedRemaining(estimatedRemaining)
                .build();
    }

    /**
     * 构建主机信息响应
     */
    private List<HostInfoResponse> buildHostInfoResponse(List<HostInfo> hostInfoList) {
        // 按客户端IP和服务端IP分组
        Map<String, List<HostInfo>> groupedByHost = hostInfoList.stream()
                .collect(Collectors.groupingBy(h -> h.getClientIp() + ":" + h.getServerIp() + ":" + h.getPort()));

        List<HostInfoResponse> responses = new ArrayList<>();

        for (Map.Entry<String, List<HostInfo>> entry : groupedByHost.entrySet()) {
            List<HostInfo> hostGroup = entry.getValue();
            if (hostGroup.isEmpty()) {
                continue;
            }

            HostInfo firstHost = hostGroup.get(0);

            // 构建应用信息列表
            List<HostInfoResponse.ApplicationInfo> applications = hostGroup.stream()
                    .map(h -> HostInfoResponse.ApplicationInfo.builder()
                            .appName(h.getAppName())
                            .appOwner(h.getAppOwner())
                            .appOwnerAccount(h.getAppOwnerAccount())
                            .department(h.getDepartment())
                            .build())
                    .collect(Collectors.toList());

            HostInfoResponse response = HostInfoResponse.builder()
                    .clientIp(firstHost.getClientIp())
                    .serverIp(firstHost.getServerIp())
                    .port(firstHost.getPort())
                    .hostname(firstHost.getHostname())
                    .linuxPath(firstHost.getLinuxPath())
                    .applications(applications)
                    .build();

            responses.add(response);
        }

        return responses;
    }
}