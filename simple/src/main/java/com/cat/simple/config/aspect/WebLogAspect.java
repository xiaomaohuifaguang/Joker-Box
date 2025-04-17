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
        String argsJson = JSON.toJSONString(argsMap, JSONWriter.Feature.WriteMapNullValue);
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

}
