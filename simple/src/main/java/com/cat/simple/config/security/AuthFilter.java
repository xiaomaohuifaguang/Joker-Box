package com.cat.simple.config.security;

import com.cat.simple.service.UserService;
import com.cat.common.entity.auth.LoginUser;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
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
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/20 0:48
 **/
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Resource
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {

        String token = request.getHeader(HttpHeaders.AUTHORIZATION);


        if( StringUtils.hasText(token) && token.startsWith("Bearer ") ){
            LoginUser loginUser = userService.getLoginUserByToken(token);
            if(!ObjectUtils.isEmpty(loginUser)){
                UserDetailsImpl userDetails = new UserDetailsImpl(loginUser);
                // 保存用户信息 到SecurityContextHolder
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }

        }


        filterChain.doFilter(request, response);

    }
}
