package com.cat.simple.config.advice;
import com.cat.common.entity.HttpResult;
import com.cat.common.entity.auth.LoginUser;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.system.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

@RestControllerAdvice
public class CustomResponseBodyAdviceAdapter implements ResponseBodyAdvice<Object> {

    @Resource
    private UserService userService;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

        if(body instanceof HttpResult<?>) {
            HttpHeaders headers = request.getHeaders();
            List<String> tokens = headers.get(HttpHeaders.AUTHORIZATION);
            if (!CollectionUtils.isEmpty(tokens)) {
                String token = tokens.get(0);
                long tokenExpirationTimeLeftMillis = userService.getTokenExpirationTimeLeftMillis(token);
                if (tokenExpirationTimeLeftMillis > 0) {
//                    LoginUser loginUser = SecurityUtils.getLoginUser();
                }
            }
        }

        return body;
    }
}
