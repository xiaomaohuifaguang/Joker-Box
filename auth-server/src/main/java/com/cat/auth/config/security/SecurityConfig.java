package com.cat.auth.config.security;

import com.cat.common.entity.LoginUser;
import com.cat.common.entity.Role;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/***
 * SpringSecurity配置
 * @title SecurityConfig
 * @description SpringSecurity配置
 * @author xiaomaohuifaguang
 * @create 2024/6/19 23:14
 **/
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 认证过滤器
     */
    @Resource
    private AuthFilter authFilter;

    /**
     * 授权处理器
     */
    @Resource
    private AuthorizationManagerImpl authorizationManager;

    /**
     * 认证失败 异常处理
     */
    @Resource
    private AuthenticationEntryPointImpl authenticationEntryPoint;
    /**
     * 授权失败 异常处理
     */
    @Resource
    private AccessDeniedHandlerImpl accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用session
                .sessionManagement(sessionManagement->{
                    sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .authorizeHttpRequests(authorize -> {
                            // 白名单
                            authorize.requestMatchers("/doc.html").permitAll();
                            authorize.requestMatchers("/webjars/**").permitAll();
                            authorize.requestMatchers("/favicon.ico").permitAll();
                            authorize.requestMatchers("/v3/api-docs/**").permitAll();
                            authorize.requestMatchers("/info/version").permitAll();
                            authorize.requestMatchers("/auth/getToken").permitAll();
                            authorize.requestMatchers("/auth/test1").permitAll();
//                            authorize.anyRequest().authenticated();
                            authorize.anyRequest().access(authorizationManager);
                        }
                )
                .exceptionHandling(exceptionHandling -> {
                    // 认证失败
                    exceptionHandling.authenticationEntryPoint(authenticationEntryPoint);
                    // 授权失败
                    exceptionHandling.accessDeniedHandler(accessDeniedHandler);
                })
                .httpBasic(AbstractHttpConfigurer::disable)
                // 屏蔽原有登录
                .formLogin(AbstractHttpConfigurer::disable)
                // 屏蔽原有登出
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(UUID.randomUUID().toString());
        loginUser.setUsername(UUID.randomUUID().toString());
        loginUser.setNickname(UUID.randomUUID().toString());
        loginUser.setRoles(List.of(new Role().setId(new Random().nextInt(9999))));
        UserDetails userDetails = User.withUserDetails(new UserDetailsImpl(loginUser)).build();
        return new InMemoryUserDetailsManager(userDetails);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.addAllowedOrigin("*");
        configuration.addAllowedOrigin("http://127.0.0.1");
        configuration.addAllowedOrigin("http://192.168.3.10 ");
        configuration.addAllowedOrigin("http://localhost:5173");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


}
