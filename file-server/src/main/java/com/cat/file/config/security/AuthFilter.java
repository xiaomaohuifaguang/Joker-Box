package com.cat.file.config.security;


import com.cat.api.auth.AuthServiceClient;
import com.cat.common.entity.*;
import com.cat.file.config.feign.AuthUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


/***
 * 登录过滤器
 * @title AuthFilter
 * @description 登录过滤器
 * @author xiaomaohuifaguang
 * @create 2024/6/20 0:48
 **/
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Resource
    private AuthServiceClient authServiceClient;
    @Resource
    private AuthUtils authUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {

        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        if( StringUtils.hasText(token) && token.startsWith("Bearer ") ){
            HttpResult<LoginUser> loginUserByToken = authServiceClient.getLoginUserByToken(CONSTANTS.TOKEN_TYPE+" "+authUtils.getToken() ,new LoginInfo().setToken(token));
            if(loginUserByToken.getCode() == HttpResultStatus.SUCCESS.code() && !ObjectUtils.isEmpty(loginUserByToken.getData())){
                UserDetailsImpl userDetails = new UserDetailsImpl(loginUserByToken.getData());
                // 保存用户信息 到SecurityContextHolder
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }

        }

        filterChain.doFilter(request, response);

    }
}
