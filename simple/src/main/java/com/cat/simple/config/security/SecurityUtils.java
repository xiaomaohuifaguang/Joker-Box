package com.cat.simple.config.security;

import com.cat.common.entity.CONSTANTS;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.auth.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
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
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if(authorities.contains(new SimpleGrantedAuthority(CONSTANTS.ANONYMOUS_ROLE))){
            return null;
        }
        return ((UserDetailsImpl) authentication.getPrincipal()).loginUser();
    }



    public static boolean isAdmin(){
        LoginUser loginUser = getLoginUser();
        List<Role> roles = loginUser.getRoles();
        for (Role role : roles) {
            if(role.getId().equals(CONSTANTS.ROLE_ADMIN_CODE)) return true;
            if(role.getAdmin().equals("1")) return true;
        }
        return false;
    }



}
