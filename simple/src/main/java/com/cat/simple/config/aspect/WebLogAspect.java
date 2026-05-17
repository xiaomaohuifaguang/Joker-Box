package com.cat.simple.config.aspect;


import com.alibaba.fastjson2.JSON;
import com.cat.common.entity.WebLog;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.utils.JSONUtils;
import com.cat.common.utils.ServletUtils;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.log.service.WebLogService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@Order(1)
public class WebLogAspect {

    /** 请求/响应参数最大序列化长度，超出截断防止大对象撑爆日志 */
    private static final int MAX_LOG_LENGTH = 2000;

    @Resource
    private WebLogService webLogService;

    @Pointcut("execution(public * com.cat.simple..controller.*.*(..))")
    public void webLog() {}

    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();
        WebLog webLog = buildWebLog(point);

        Object result;
        try {
            result = point.proceed();
            webLog.setRepArgs(truncate(JSONUtils.toJSONString(result)));
        } catch (Throwable e) {
            webLog.setRepArgs(truncate(e.getClass().getName() + ": " + e.getMessage()));
            throw e;
        } finally {
            long end = System.currentTimeMillis();
            webLog.setEndTimestamp(end);
            webLog.setResTime(end - start);
            webLog.setEndTime(LocalDateTime.now());
            try {
                webLogService.add(webLog);
            } catch (Exception ex) {
                log.error("WebLog 持久化失败: {}", ex.getMessage());
            }
        }
        return result;
    }

    private WebLog buildWebLog(ProceedingJoinPoint point) {
        HttpServletRequest request = ServletUtils.getHttpServletRequest();

        WebLog webLog = new WebLog();
        webLog.setStartTimestamp(System.currentTimeMillis());
        webLog.setReqPath(request.getServletPath());
        webLog.setReqMethod(request.getMethod());
        webLog.setReqArgs(truncate(serializeArgs(point.getArgs())));
        webLog.setClassName(point.getSignature().getDeclaringTypeName());
        webLog.setMethodName(point.getSignature().getName());
        webLog.setRemoteAddr(request.getRemoteAddr());
        webLog.setCreateTime(LocalDateTime.now());

        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (!ObjectUtils.isEmpty(loginUser)) {
                webLog.setUserId(loginUser.getUserId());
            }
        } catch (Exception e) {
            log.debug("获取当前登录用户失败: {}", e.getMessage());
        }

        return webLog;
    }

    private String serializeArgs(Object[] args) {
        Map<String, Object> argsMap = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            String key = arg == null ? "arg#" + i : arg.getClass().getSimpleName() + "#" + i;
            argsMap.put(key, resolveArgValue(arg));
        }
        return JSON.toJSONString(argsMap);
    }

    private Object resolveArgValue(Object arg) {
        if (arg == null) {
            return null;
        }
        if (arg instanceof MultipartFile file) {
            return Map.of("name", file.getOriginalFilename() != null ? file.getOriginalFilename() : "",
                    "size", file.getSize());
        }
        if (arg instanceof MultipartFile[] files) {
            return java.util.Arrays.stream(files)
                    .map(f -> Map.of("name", f.getOriginalFilename() != null ? f.getOriginalFilename() : "",
                            "size", f.getSize()))
                    .toList();
        }
        if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                || arg instanceof InputStream || arg instanceof OutputStream) {
            return arg.getClass().getSimpleName() + " [ignored]";
        }
        return arg;
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > MAX_LOG_LENGTH
                ? text.substring(0, MAX_LOG_LENGTH) + "... [truncated, total=" + text.length() + "]"
                : text;
    }
}
