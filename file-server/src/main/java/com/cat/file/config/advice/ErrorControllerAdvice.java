package com.cat.file.config.advice;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.common.utils.ServletUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/***
 * 接口异常处理类
 * @title ErrorControllerAdvice
 * @description 接口异常处理类
 * @author xiaomaohuifaguang
 * @create 2024/6/19 23:50
 **/
@RestControllerAdvice
public class ErrorControllerAdvice {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public HttpResult<?> error(Exception e){
        HttpServletResponse httpServletResponse = ServletUtils.getHttpServletResponse();
        e.printStackTrace();
        HttpResultStatus error = HttpResultStatus.ERROR;
        if(e instanceof org.springframework.web.servlet.NoHandlerFoundException){
            error = HttpResultStatus.NOT_FOUND;

        }else {
            error.setMsg(e.getMessage());
        }
        httpServletResponse.setStatus(error.code() == -1 ? 500 : (int) error.code());
        return HttpResult.back(error);
    }
}
