package com.cat.simple.config.security;

import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.auth.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;


/***
 * <TODO description class purpose>
 * @title UserDetailsImpl
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/20 0:36
 **/
public record UserDetailsImpl(LoginUser loginUser) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
//        if(loginUser.getUsername().equals(CONSTANTS.ANONYMOUS_ROLE_NAME)) return List.of(new SimpleGrantedAuthority(CONSTANTS.ANONYMOUS_ROLE_NAME));
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (Role role : loginUser.getRoles()) {
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(String.valueOf(role.getId()));
            authorities.add(authority);
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());
    }

    @Override
    public String getUsername() {
        return loginUser.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
