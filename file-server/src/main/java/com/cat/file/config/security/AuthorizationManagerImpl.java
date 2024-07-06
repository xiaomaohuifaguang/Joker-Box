package com.cat.file.config.security;

import com.cat.common.entity.CONSTANTS;
import com.cat.common.entity.HttpResultStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.function.Supplier;

/***
 * 授权处理器
 * @title AuthorizationManagerImpl
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/20 1:31
 **/
@Component
public class AuthorizationManagerImpl implements AuthorizationManager<RequestAuthorizationContext> {
    @Override
    public void verify(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        AuthorizationManager.super.verify(authentication, object);
    }
    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        HttpServletRequest request = object.getRequest();
        String path = request.getServletPath();
        Collection<? extends GrantedAuthority> authorities = authentication.get().getAuthorities();
        if(authorities.contains(new SimpleGrantedAuthority(CONSTANTS.ANONYMOUS_ROLE))){
            throw new AccessDeniedException(HttpResultStatus.UNAUTHORIZED.msg());
        }
        // 这里要对 path 和当前用户的角色做匹配 true 通过 false 不通过
        return new AuthorizationDecision(true);
    }
}
