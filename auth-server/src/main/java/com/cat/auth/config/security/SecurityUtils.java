package com.cat.auth.config.security;

import com.cat.common.entity.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/***
 * <TODO description class purpose>
 * @title SecurityUtils
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/27 0:10
 **/
public class SecurityUtils {

    public static LoginUser getLoginUser(){
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        return ((UserDetailsImpl) authentication.getPrincipal()).loginUser();
    }

}
