package com.wind.middleware;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 中间件分析系统主启动类
 */
@SpringBootApplication
@MapperScan("com.wind.middleware.mapper")
public class MiddlewareAnalyzeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiddlewareAnalyzeApplication.class, args);
    }
}