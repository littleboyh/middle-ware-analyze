package com.wind.middleware.exception;

import com.wind.middleware.dto.response.MyApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 参数校验异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MyApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数校验失败: {}", errorMessage);
        return ResponseEntity.badRequest()
                .body(MyApiResponse.error("参数校验失败: " + errorMessage));
    }

    /**
     * 绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<MyApiResponse<Void>> handleBindException(BindException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数绑定异常: {}", errorMessage);
        return ResponseEntity.badRequest()
                .body(MyApiResponse.error("参数错误: " + errorMessage));
    }

    /**
     * 非法参数异常处理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MyApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("参数错误: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(MyApiResponse.error(ex.getMessage()));
    }

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<MyApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return ResponseEntity.status(ex.getCode())
                .body(MyApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    /**
     * 运行时异常处理
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MyApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("运行时异常", ex);

        // 特定异常消息处理
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("任务不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(MyApiResponse.notFound(message));
            }
            if (message.contains("只能删除已完成或失败的任务")) {
                return ResponseEntity.badRequest()
                        .body(MyApiResponse.error(message));
            }
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MyApiResponse.serverError("系统内部错误: " + message));
    }

    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<MyApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MyApiResponse.serverError("系统内部错误，请联系管理员"));
    }
}