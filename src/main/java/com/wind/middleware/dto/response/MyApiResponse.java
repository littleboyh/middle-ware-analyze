package com.wind.middleware.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyApiResponse<T> {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     */
    public static <T> MyApiResponse<T> success(T data) {
        return new MyApiResponse<>(200, "成功", data);
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> MyApiResponse<T> success() {
        return new MyApiResponse<>(200, "成功", null);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> MyApiResponse<T> success(String message, T data) {
        return new MyApiResponse<>(200, message, data);
    }

    /**
     * 客户端错误响应
     */
    public static <T> MyApiResponse<T> error(String message) {
        return new MyApiResponse<>(400, message, null);
    }

    /**
     * 客户端错误响应（自定义错误码）
     */
    public static <T> MyApiResponse<T> error(Integer code, String message) {
        return new MyApiResponse<>(code, message, null);
    }

    /**
     * 资源不存在
     */
    public static <T> MyApiResponse<T> notFound(String message) {
        return new MyApiResponse<>(404, message, null);
    }

    /**
     * 服务器错误响应
     */
    public static <T> MyApiResponse<T> serverError(String message) {
        return new MyApiResponse<>(500, message, null);
    }
}