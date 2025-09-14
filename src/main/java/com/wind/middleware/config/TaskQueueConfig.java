package com.wind.middleware.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 任务队列配置类
 */
@Slf4j
@Configuration
public class TaskQueueConfig {

    @Value("${middleware.task.queue-capacity}")
    private int queueCapacity;

    /**
     * 任务队列
     */
    @Bean
    public LinkedBlockingQueue<String> taskQueue() {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(queueCapacity);
        log.info("任务队列初始化完成，容量: {}", queueCapacity);
        return queue;
    }

    /**
     * 单线程执行器
     */
    @Bean
    public ExecutorService taskExecutor() {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("task-processor");
            thread.setDaemon(false);
            return thread;
        });
        log.info("任务处理线程池初始化完成");
        return executor;
    }
}