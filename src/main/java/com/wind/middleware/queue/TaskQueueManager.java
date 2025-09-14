package com.wind.middleware.queue;

import com.wind.middleware.entity.enums.TaskStatus;
import com.wind.middleware.service.TaskDataService;
import com.wind.middleware.event.TaskSubmittedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 任务队列管理器
 */
@Slf4j
@Component
public class TaskQueueManager {

    @Autowired
    private LinkedBlockingQueue<String> taskQueue;

    @Autowired
    private ExecutorService taskExecutor;

    @Autowired
    private TaskProcessor taskProcessor;

    @Autowired
    private TaskDataService taskDataService;

    private volatile boolean isRunning = true;

    @PostConstruct
    public void initialize() {
        log.info("任务队列管理器启动");
        // 启动任务消费线程
        taskExecutor.submit(this::consumeTasks);
    }

    /**
     * 监听任务提交事件
     */
    @EventListener
    public void handleTaskSubmittedEvent(TaskSubmittedEvent event) {
        submitTask(event.getTaskId());
    }

    /**
     * 提交任务到队列
     */
    private boolean submitTask(String taskId) {
        if (!isRunning) {
            log.warn("任务队列管理器已停止，无法提交任务: {}", taskId);
            return false;
        }

        try {
            taskQueue.put(taskId);
            log.info("任务提交成功: {}, 当前队列长度: {}", taskId, taskQueue.size());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务提交被中断: {}", taskId, e);
            return false;
        }
    }

    /**
     * 消费任务
     */
    private void consumeTasks() {
        log.info("任务消费线程启动");

        while (isRunning) {
            try {
                // 阻塞获取任务
                String taskId = taskQueue.take();
                log.info("开始处理任务: {}, 剩余队列长度: {}", taskId, taskQueue.size());

                // 更新任务状态为处理中
                taskDataService.updateTaskStatus(taskId, TaskStatus.ES_QUERYING, null);

                // 处理任务
                taskProcessor.processTask(taskId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("任务消费线程被中断");
                break;
            } catch (Exception e) {
                log.error("任务处理过程中发生异常", e);
                // 继续处理下一个任务，不中断整个消费循环
            }
        }

        log.info("任务消费线程结束");
    }

    /**
     * 获取队列状态
     */
    public QueueStatus getQueueStatus() {
        return QueueStatus.builder()
                .queueSize(taskQueue.size())
                .isRunning(isRunning)
                .capacity(taskQueue.size() + taskQueue.remainingCapacity())
                .build();
    }

    @PreDestroy
    public void shutdown() {
        log.info("任务队列管理器开始停止");
        isRunning = false;

        // 中断任务执行器
        if (taskExecutor != null && !taskExecutor.isShutdown()) {
            taskExecutor.shutdown();
            log.info("任务执行器已关闭");
        }

        log.info("任务队列管理器停止完成");
    }

    /**
     * 队列状态信息
     */
    @lombok.Data
    @lombok.Builder
    public static class QueueStatus {
        private int queueSize;      // 当前队列长度
        private boolean isRunning;  // 是否运行中
        private int capacity;       // 队列容量
    }
}