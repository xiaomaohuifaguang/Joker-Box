package com.cat.simple.config.security;

import com.cat.common.entity.HttpResult;
import com.cat.common.utils.ServletUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${custom.page-server}")
    private String customPageServer;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

//        ServletUtils.back(HttpResult.back("").setMsg("登录失败"), response);
        response.sendRedirect(customPageServer+"/login?msg=failture");  // 登录失败后重定向到登录页
    }
}
