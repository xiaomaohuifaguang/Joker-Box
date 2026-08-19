package com.cat.simple.config.advice;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;
import java.util.stream.Collectors;

/***
 * 接口异常处理类
 * @title ErrorControllerAdvice
 * @description 统一捕获控制器异常，按类型返回标准响应；原始堆栈只进日志，不回前端
 * @author xiaomaohuifaguang
 * @create 2024/6/19 23:50
 **/
@RestControllerAdvice
@Slf4j
public class ErrorControllerAdvice {

    /** 参数校验失败（@Valid / @Validated） */
    @ExceptionHandler(BindException.class)
    public HttpResult<?> handleBind(BindException e, HttpServletRequest request) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(ErrorControllerAdvice::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("[{} {}] 参数校验失败: {}", request.getMethod(), request.getRequestURI(), detail);
        return back(HttpResultStatus.ERROR, "参数校验失败: " + detail);
    }

    /** 缺少必填请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public HttpResult<?> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("[{} {}] 缺少参数: {}", request.getMethod(), request.getRequestURI(), e.getParameterName());
        return back(HttpResultStatus.ERROR, "缺少必填参数: " + e.getParameterName());
    }

    /** 参数类型不匹配（如应传数字却传了字符串） */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public HttpResult<?> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("[{} {}] 参数类型不匹配: {}", request.getMethod(), request.getRequestURI(), e.getName());
        return back(HttpResultStatus.ERROR, "参数类型不匹配: " + e.getName());
    }

    /** 请求体不可读（JSON 格式错误等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public HttpResult<?> handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("[{} {}] 请求体解析失败: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return back(HttpResultStatus.ERROR, "请求体格式错误");
    }

    /** 404：无匹配处理器 / 静态资源不存在 */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public HttpResult<?> handleNotFound(Exception e, HttpServletRequest request, HttpServletResponse response) {
        log.warn("[{} {}] 资源不存在", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return HttpResult.back(HttpResultStatus.NOT_FOUND);
    }

    /** 405：请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public HttpResult<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request, HttpServletResponse response) {
        log.warn("[{} {}] 方法不支持: {}", request.getMethod(), request.getRequestURI(), e.getMethod());
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        return back(HttpResultStatus.ERROR, "请求方法不支持: " + e.getMethod());
    }

    /** 401：未认证 */
    @ExceptionHandler(AuthenticationException.class)
    public HttpResult<?> handleAuthentication(AuthenticationException e, HttpServletRequest request, HttpServletResponse response) {
        log.warn("[{} {}] 鉴权失败: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return HttpResult.back(HttpResultStatus.UNAUTHORIZED);
    }

    /** 403：无权限 */
    @ExceptionHandler(AccessDeniedException.class)
    public HttpResult<?> handleAccessDenied(AccessDeniedException e, HttpServletRequest request, HttpServletResponse response) {
        log.warn("[{} {}] 权限不足: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return HttpResult.back(HttpResultStatus.FORBIDDEN);
    }

    /** 兜底：其余所有异常。不回原始 message，避免泄露内部信息 */
    @ExceptionHandler(Exception.class)
    public HttpResult<?> handleException(Exception e, HttpServletRequest request) {
        log.error("[{} {}] 系统异常", request.getMethod(), request.getRequestURI(), e);
        if(Objects.nonNull(e.getCause()) && StringUtils.hasText(e.getCause().getMessage())){
            return back(HttpResultStatus.ERROR, e.getCause().getMessage());
        }
        return back(HttpResultStatus.ERROR, "系统异常，请联系管理员");
    }

    private static String formatFieldError(FieldError fe) {
        return fe.getField() + " " + (fe.getDefaultMessage() == null ? "不合法" : fe.getDefaultMessage());
    }

    /** 用独立 msg 构造响应，不改动枚举单例（枚举 msg 只读） */
    private static HttpResult<?> back(HttpResultStatus status, String msg) {
        return HttpResult.back(status.code(), null, msg);
    }
}
