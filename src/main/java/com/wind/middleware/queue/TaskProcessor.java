package com.wind.middleware.queue;

import com.wind.middleware.dto.external.HostDetailInfo;
import com.wind.middleware.dto.external.MachineInfo;
import com.wind.middleware.dto.external.ServiceInfo;
import com.wind.middleware.entity.HostInfo;
import com.wind.middleware.entity.Task;
import com.wind.middleware.entity.TaskResult;
import com.wind.middleware.entity.enums.TaskStatus;
import com.wind.middleware.service.ElasticsearchService;
import com.wind.middleware.service.ExternalAPIService;
import com.wind.middleware.service.TaskDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务处理器
 */
@Slf4j
@Component
public class TaskProcessor {

    @Autowired
    private TaskDataService taskDataService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private ExternalAPIService externalAPIService;

    /**
     * 处理任务
     */
    public void processTask(String taskId) {
        try {
            log.info("开始处理任务: {}", taskId);

            // 1. 获取任务详情
            Task task = taskDataService.getTaskById(taskId);
            if (task == null) {
                log.error("任务不存在: {}", taskId);
                return;
            }

            // 2. 执行ES查询
            Map<String, Long> clientIpAccess = executeESQuery(task);

            // 3. 更新任务状态
            taskDataService.updateTaskStatus(taskId, TaskStatus.ES_COMPLETED, null);

            // 4. 保存ES查询结果
            saveTaskResults(taskId, clientIpAccess, task);

            // 5. 调用外部API获取详细信息
            taskDataService.updateTaskStatus(taskId, TaskStatus.API_CALLING, null);
            processHostInfo(taskId, clientIpAccess, task);

            // 6. 任务完成
            taskDataService.updateTaskStatus(taskId, TaskStatus.COMPLETED, null);
            log.info("任务处理完成: {}", taskId);

        } catch (Exception e) {
            log.error("任务处理失败: {}", taskId, e);
            taskDataService.updateTaskStatus(taskId, TaskStatus.FAILED, e.getMessage());
        }
    }

    /**
     * 执行ES查询
     */
    private Map<String, Long> executeESQuery(Task task) {
        try {
            log.info("开始ES查询，任务ID: {}, 服务端IP: {}, 端口: {}, 时间范围: {} - {}",
                    task.getTaskId(), task.getServerIps(), task.getPort(),
                    task.getStartDate(), task.getEndDate());

            Map<String, Long> result = elasticsearchService.queryClientIpAccess(
                    task.getServerIps(),
                    task.getPort(),
                    task.getStartDate(),
                    task.getEndDate()
            );

            log.info("ES查询完成，任务ID: {}, 获得客户端IP数量: {}", task.getTaskId(), result.size());
            return result;
        } catch (Exception e) {
            log.error("ES查询失败，任务ID: {}", task.getTaskId(), e);
            throw new RuntimeException("ES查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存ES查询结果
     */
    private void saveTaskResults(String taskId, Map<String, Long> clientIpAccess, Task task) {
        try {
            List<TaskResult> taskResults = new ArrayList<>();

            for (Map.Entry<String, Long> entry : clientIpAccess.entrySet()) {
                String key = entry.getKey(); // 格式为"clientIp:serverIp"
                Long accessCount = entry.getValue();

                // 解析clientIp和serverIp
                String[] parts = key.split(":");
                if (parts.length != 2) {
                    log.warn("无效的key格式: {}, 跳过", key);
                    continue;
                }

                String clientIp = parts[0];
                String serverIp = parts[1];

                TaskResult taskResult = new TaskResult();
                taskResult.setTaskId(taskId);
                taskResult.setClientIp(clientIp);
                taskResult.setServerIp(serverIp);
                taskResult.setPort(task.getPort());
                taskResult.setAccessCount(accessCount);
                taskResult.setCreateTime(LocalDateTime.now());

                taskResults.add(taskResult);
            }

            taskDataService.saveTaskResults(taskResults);
            log.info("ES查询结果保存成功，任务ID: {}, 记录数量: {}", taskId, taskResults.size());
        } catch (Exception e) {
            log.error("保存ES查询结果失败，任务ID: {}", taskId, e);
            throw new RuntimeException("保存查询结果失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理主机信息
     */
    private void processHostInfo(String taskId, Map<String, Long> clientIpAccess, Task task) {
        List<HostInfo> hostInfoList = new ArrayList<>();
        int processedCount = 0;
        int totalCount = clientIpAccess.size();

        log.info("开始处理主机信息，任务ID: {}, 总数: {}", taskId, totalCount);

        for (Map.Entry<String, Long> entry : clientIpAccess.entrySet()) {
            String key = entry.getKey(); // 格式为"clientIp:serverIp"

            // 解析clientIp和serverIp
            String[] parts = key.split(":");
            if (parts.length != 2) {
                log.warn("无效的key格式: {}, 跳过", key);
                continue;
            }

            String clientIp = parts[0];
            String serverIp = parts[1];

            try {
                processedCount++;

                // 获取机器信息
                Optional<MachineInfo> machineInfo = externalAPIService.getMachineByIp(clientIp);
                if (machineInfo.isEmpty()) {
                    log.debug("未找到客户端IP对应的机器信息: {}", clientIp);
                    continue;
                }

                // 获取主机详细信息
                Optional<HostDetailInfo> hostDetail = externalAPIService.getHostDetail(
                        clientIp, serverIp, task.getPort());

                if (hostDetail.isEmpty()) {
                    log.debug("未找到主机详细信息: clientIp={}, serverIp={}, port={}",
                            clientIp, serverIp, task.getPort());
                    continue;
                }

                // 处理应用信息
                HostDetailInfo detail = hostDetail.get();
                if (detail.getApplications() != null) {
                    for (String appName : detail.getApplications()) {
                        Optional<ServiceInfo> serviceInfo = externalAPIService.getServiceInfo(appName);

                        HostInfo hostInfo = new HostInfo();
                        hostInfo.setTaskId(taskId);
                        hostInfo.setClientIp(clientIp);
                        hostInfo.setServerIp(serverIp);
                        hostInfo.setPort(task.getPort());
                        hostInfo.setHostname(machineInfo.get().getMachineName());
                        hostInfo.setLinuxPath(detail.getLinuxPath());
                        hostInfo.setAppName(appName);

                        if (serviceInfo.isPresent()) {
                            ServiceInfo service = serviceInfo.get();
                            hostInfo.setAppOwner(service.getOwnerName());
                            hostInfo.setAppOwnerAccount(service.getOwnerAccount());
                            hostInfo.setDepartment(service.getDepartment());
                        }

                        hostInfo.setCreateTime(LocalDateTime.now());
                        hostInfoList.add(hostInfo);
                    }
                }

                // 定期记录进度
                if (processedCount % 10 == 0) {
                    log.info("任务ID: {} 主机信息处理进度: {}/{} ({}%)",
                            taskId, processedCount, totalCount,
                            (processedCount * 100 / totalCount));
                }

            } catch (Exception e) {
                log.warn("处理主机信息失败，跳过: clientIp={}, serverIp={}, 错误: {}",
                        clientIp, serverIp, e.getMessage());
                // 继续处理其他IP，不中断整个流程
            }
        }

        // 保存主机信息
        if (!hostInfoList.isEmpty()) {
            try {
                taskDataService.saveHostInfo(hostInfoList);
                log.info("主机信息保存成功，任务ID: {}, 记录数量: {}", taskId, hostInfoList.size());
            } catch (Exception e) {
                log.error("保存主机信息失败，任务ID: {}", taskId, e);
                throw new RuntimeException("保存主机信息失败: " + e.getMessage(), e);
            }
        } else {
            log.warn("未获取到任何主机信息，任务ID: {}", taskId);
        }
    }
}