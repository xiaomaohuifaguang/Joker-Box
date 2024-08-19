package com.cat.demo.config.security;

import com.cat.common.entity.CONSTANTS;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.auth.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

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

    public static boolean isNormal(){
        LoginUser loginUser = getLoginUser();
        List<Role> roles = loginUser.getRoles();
        return roles.size()==1 && roles.get(0).getId().equals(CONSTANTS.ROLE_EVERYONE_CODE);
    }

    public static String getUserId(){
        LoginUser loginUser = getLoginUser();
        return loginUser.getUserId();
    }

}
