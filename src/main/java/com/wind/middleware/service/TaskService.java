package com.wind.middleware.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wind.middleware.entity.HostInfo;
import com.wind.middleware.entity.Task;
import com.wind.middleware.entity.TaskResult;
import com.wind.middleware.entity.enums.TaskStatus;
import com.wind.middleware.mapper.HostInfoMapper;
import com.wind.middleware.mapper.TaskMapper;
import com.wind.middleware.mapper.TaskResultMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.wind.middleware.event.TaskSubmittedEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * 任务管理服务
 */
@Slf4j
@Service
public class TaskService implements TaskDataService {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskResultMapper taskResultMapper;

    @Autowired
    private HostInfoMapper hostInfoMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${middleware.task.max-query-days}")
    private int maxQueryDays;

    @Value("${middleware.task.default-page-size}")
    private int defaultPageSize;

    @Value("${middleware.task.max-page-size}")
    private int maxPageSize;

    /**
     * 提交分析任务
     */
    @Transactional
    public String submitTask(String submitter, String description, List<String> serverIps,
                           Integer port, LocalDate startDate, LocalDate endDate) {
        // 参数校验
        validateTaskParams(submitter, serverIps, port, startDate, endDate);

        // 生成任务ID
        String taskId = generateTaskId();

        // 创建任务实体
        Task task = new Task();
        task.setTaskId(taskId);
        task.setSubmitter(submitter);
        task.setDescription(description);
        task.setServerIps(serverIps);
        task.setPort(port);
        task.setStartDate(startDate);
        task.setEndDate(endDate);
        task.setStatus(TaskStatus.SUBMITTED);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        // 保存任务
        taskMapper.insert(task);
        log.info("任务创建成功: {}", taskId);

        // 发布任务提交事件
        eventPublisher.publishEvent(new TaskSubmittedEvent(taskId));
        log.info("任务提交事件发布成功: {}", taskId);

        return taskId;
    }

    /**
     * 分页查询任务列表（支持条件筛选）
     */
    public Page<Task> queryTasks(int pageNum, int pageSize, String sort,
                               String submitter, String description) {
        // 参数校验和默认值处理
        pageSize = Math.min(pageSize > 0 ? pageSize : defaultPageSize, maxPageSize);
        pageNum = Math.max(pageNum, 0);

        Page<Task> page = new Page<>(pageNum + 1, pageSize);

        // 设置排序
        if ("updateTime".equals(sort)) {
            page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("update_time"));
        } else {
            page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("create_time"));
        }

        return taskMapper.selectTaskPage(page, submitter, description);
    }

    /**
     * 查询全部任务（不进行条件筛选）
     */
    public Page<Task> queryAllTasks(int pageNum, int pageSize, String sort) {
        // 参数校验和默认值处理
        pageSize = Math.min(pageSize > 0 ? pageSize : 20, maxPageSize);
        pageNum = Math.max(pageNum, 0);

        Page<Task> page = new Page<>(pageNum + 1, pageSize);

        // 设置排序
        if ("updateTime".equals(sort)) {
            page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("update_time"));
        } else {
            page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("create_time"));
        }

        return taskMapper.selectAllTaskPage(page);
    }

    /**
     * 查询任务状态
     */
    public Task getTaskById(String taskId) {
        return taskMapper.selectById(taskId);
    }

    /**
     * 查询客户端IP访问结果
     */
    public List<TaskResult> getTaskResults(String taskId) {
        return taskResultMapper.selectByTaskId(taskId);
    }

    /**
     * 查询完整主机信息
     */
    public List<HostInfo> getHostInfo(String taskId) {
        return hostInfoMapper.selectByTaskId(taskId);
    }

    /**
     * 删除任务
     */
    @Transactional
    public boolean deleteTask(String taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 只能删除已完成或失败的任务
        if (task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.FAILED) {
            throw new RuntimeException("只能删除已完成或失败的任务");
        }

        // 逻辑删除任务和相关数据
        taskMapper.deleteById(taskId);
        taskResultMapper.deleteByTaskId(taskId);
        hostInfoMapper.deleteByTaskId(taskId);

        log.info("任务删除成功: {}", taskId);
        return true;
    }

    /**
     * 更新任务状态
     */
    @Transactional
    public boolean updateTaskStatus(String taskId, TaskStatus status, String errorMessage) {
        Task task = new Task();
        task.setTaskId(taskId);
        task.setStatus(status);
        task.setErrorMessage(errorMessage);
        task.setUpdateTime(LocalDateTime.now());

        int updated = taskMapper.updateById(task);
        if (updated > 0) {
            log.info("任务状态更新成功: {}, 状态: {}", taskId, status);
            return true;
        } else {
            log.warn("任务状态更新失败: {}", taskId);
            return false;
        }
    }

    /**
     * 保存任务结果
     */
    @Transactional
    public void saveTaskResults(List<TaskResult> taskResults) {
        if (taskResults != null && !taskResults.isEmpty()) {
            taskResultMapper.batchInsert(taskResults);
            log.info("任务结果保存成功，数量: {}", taskResults.size());
        }
    }

    /**
     * 保存主机信息
     */
    @Transactional
    public void saveHostInfo(List<HostInfo> hostInfoList) {
        if (hostInfoList != null && !hostInfoList.isEmpty()) {
            hostInfoMapper.batchInsert(hostInfoList);
            log.info("主机信息保存成功，数量: {}", hostInfoList.size());
        }
    }

    /**
     * 参数校验
     */
    private void validateTaskParams(String submitter, List<String> serverIps,
                                  Integer port, LocalDate startDate, LocalDate endDate) {
        if (!StringUtils.hasText(submitter)) {
            throw new IllegalArgumentException("提交人不能为空");
        }
        if (submitter.length() > 100) {
            throw new IllegalArgumentException("提交人长度不能超过100字符");
        }
        if (serverIps == null || serverIps.isEmpty()) {
            throw new IllegalArgumentException("服务端IP列表不能为空");
        }
        if (serverIps.size() > 50) {
            throw new IllegalArgumentException("服务端IP数量不能超过50个");
        }
        if (port == null || port < 1 || port > 65535) {
            throw new IllegalArgumentException("端口号必须在1-65535范围内");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("开始日期和结束日期不能为空");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }

        // 检查查询时间范围
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (daysBetween > maxQueryDays) {
            throw new IllegalArgumentException(
                    String.format("查询时间范围不能超过%d天，当前: %d天", maxQueryDays, daysBetween));
        }
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "task_" + System.currentTimeMillis() + "_" +
               UUID.randomUUID().toString().substring(0, 8);
    }

}