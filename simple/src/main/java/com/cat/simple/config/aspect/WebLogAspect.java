package com.cat.simple.config.aspect;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.cat.common.entity.WebLog;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.utils.JSONUtils;
import com.cat.common.utils.ServletUtils;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.service.WebLogService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
@Order(1)
public class WebLogAspect {

    private static final ThreadLocal<WebLog> logThreadLocal = new ThreadLocal<>();


    @Resource
    private WebLogService webLogService;



    // 定义切点，匹配controller包下的所有方法
    @Pointcut("execution(public * com.cat.simple.controller.*.*(..))")
    public void webLog() {}

    // 前置通知：记录请求信息
    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) {
        HttpServletRequest request = ServletUtils.getHttpServletRequest();

        WebLog webLog = new WebLog();
        webLog.setStartTimestamp(System.currentTimeMillis());
        webLog.setReqPath(request.getServletPath());
        webLog.setReqMethod(request.getMethod());
        // 自定义序列化逻辑
        Object[] args = joinPoint.getArgs();
        Map<String, Object> argsMap = Arrays.stream(args)
                .collect(Collectors.toMap(
                        arg -> arg instanceof MultipartFile ? ((MultipartFile) arg).getName() : arg.toString(),
                        arg -> arg instanceof MultipartFile ?
                                Map.of("name", ((MultipartFile) arg).getOriginalFilename(), "size", ((MultipartFile) arg).getSize()) : arg
                ));
        String argsJson = serializeLargeObject(argsMap);
        webLog.setReqArgs(argsJson);

        webLog.setClassName(joinPoint.getSignature().getDeclaringTypeName());
        webLog.setMethodName(joinPoint.getSignature().getName());
        webLog.setRemoteAddr(request.getRemoteAddr());
        webLog.setCreateTime(LocalDateTime.now());


        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if(!ObjectUtils.isEmpty(loginUser)){
                webLog.setUserId(loginUser.getUserId());
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }


        logThreadLocal.set(webLog);


    }

    // 后置通知：记录响应信息
    @AfterReturning(value = "webLog()", returning = "ret")
    public void doAfterReturning(Object ret) {

        WebLog webLog = logThreadLocal.get();
        webLog.setEndTimestamp(System.currentTimeMillis());
        webLog.setResTime(webLog.getEndTimestamp() - webLog.getStartTimestamp());
        webLog.setRepArgs(JSONUtils.toJSONString(ret));
        webLog.setEndTime(LocalDateTime.now());
        webLogService.add(webLog);
        logThreadLocal.remove();
    }

    public static String serializeLargeObject(Map<String, Object> argsMap) {
        try {
            return JSON.toJSONString(argsMap,
                    JSONWriter.Feature.WriteMapNullValue,
                    JSONWriter.Feature.LargeObject);
        } catch (OutOfMemoryError e) {
            // 如果仍然内存不足，尝试分批处理
            return serializeInBatches(argsMap);
        }
    }

    private static String serializeInBatches(Map<String, Object> argsMap) {
        // 实现分批处理的逻辑
        // 例如每次处理1000个条目
        int batchSize = 1000;
        StringBuilder sb = new StringBuilder("{");
        int count = 0;

        for (Map.Entry<String, Object> entry : argsMap.entrySet()) {
            if (count > 0) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":")
                    .append(JSON.toJSONString(entry.getValue()));

            if (++count % batchSize == 0) {
                // 定期清理内存
                System.gc();
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
