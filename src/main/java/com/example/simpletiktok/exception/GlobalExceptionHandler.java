package com.example.simpletiktok.exception;

import com.example.simpletiktok.util.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * 统一兜底接口异常，避免直接返回默认错误页或不一致的错误结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：优先返回业务层定义的 code 和 msg。
     */
    @ExceptionHandler(BizException.class)
    public R<?> handleBizException(BizException e) {
        return R.error().code(e.getCode()).message(e.getMsg());
    }

    /**
     * 参数校验异常（@RequestBody + @Valid）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return R.error().code(400).message(msg.isEmpty() ? "请求参数不合法" : msg);
    }

    /**
     * 参数绑定异常（表单/查询参数绑定失败）。
     */
    @ExceptionHandler(BindException.class)
    public R<?> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return R.error().code(400).message(msg.isEmpty() ? "请求参数不合法" : msg);
    }

    /**
     * 约束校验异常（如 @Validated 场景）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<?> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining(", "));
        return R.error().code(400).message(msg.isEmpty() ? "请求参数不合法" : msg);
    }

    /**
     * 参数类型不匹配（如 Long 参数收到 NaN/undefined）。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String name = e.getName();
        Object value = e.getValue();
        return R.error().code(400).message("参数类型错误: " + name + "=" + value);
    }

    /**
     * 缺少必填请求参数。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return R.error().code(400).message("缺少必填参数: " + e.getParameterName());
    }

    /**
     * 请求体不可读（JSON 语法错误、字段类型错误等）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return R.error().code(400).message("请求体格式错误");
    }

    /**
     * 参数非法异常。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return R.error().code(400).message(e.getMessage() == null ? "参数非法" : e.getMessage());
    }

    /**
     * 未捕获异常兜底。
     */
    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return R.error().code(500).message("系统异常，请稍后重试");
    }
}
