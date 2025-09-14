package com.wind.middleware.entity.enums;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    /**
     * 已提交
     */
    SUBMITTED("已提交"),

    /**
     * ES查询中
     */
    ES_QUERYING("ES查询中"),

    /**
     * ES查询完成
     */
    ES_COMPLETED("ES查询完成"),

    /**
     * API调用中
     */
    API_CALLING("API调用中"),

    /**
     * 已完成
     */
    COMPLETED("已完成"),

    /**
     * 执行失败
     */
    FAILED("执行失败");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}