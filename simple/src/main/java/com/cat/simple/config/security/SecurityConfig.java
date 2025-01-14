package com.cat.simple.config.security;

import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.auth.Role;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.Customizer;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

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

    public static List<String> WHITE_LIST = Arrays.asList(
            "/doc.html",
            "/webjars/**",
            "/favicon.ico",
            "/v3/api-docs/**",
            "/info/version",
            "/auth/getToken",
            "/auth/mailCode",
            "/auth/register",
            "/auth/avatar/**"
    );

//    @Value("${spring.security.oauth2.resourceserver.jwt.public-key-location}")
//    private String publicKeyLocation;

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

    // OAuth2 资源服务器的 JWK Set URI
//    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
//    private String jwkSetUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用session
                .sessionManagement(sessionManagement-> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                            // 白名单
                            WHITE_LIST.forEach(p-> authorize.requestMatchers(p).permitAll());

//                            authorize.anyRequest().authenticated();
                            authorize.anyRequest().access(authorizationManager);
                        }
                )
                // 配置 OAuth2 资源服务器，支持基于 JWT 的认证
//                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))  // 使用新的方式配置 JWT 解码器
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

    // 配置 JWT 解码器，从 JWK Set URI 获取公钥
//    @Bean
//    public JwtDecoder jwtDecoder() {
//        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
//    }

//    @Bean
//    public JwtDecoder jwtDecoder() throws Exception {
//        // 使用 ClassPathResource 加载文件
//        ClassPathResource resource = new ClassPathResource("my-public-key.pub");
//
//        try (InputStream inputStream = resource.getInputStream()) {
//            // 读取文件内容
//            String publicKeyContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
//
//            // 去掉 PEM 文件的头尾部分
//            publicKeyContent = publicKeyContent.replace("-----BEGIN PUBLIC KEY-----", "")
//                    .replace("-----END PUBLIC KEY-----", "").trim();
//
//            // Base64 解码公钥
//            byte[] decoded = Base64.getDecoder().decode(publicKeyContent);
//
//            // 使用 X509 编码规范生成公钥
//            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
//            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
//
//            // 返回 JwtDecoder，使用本地公钥进行验证
//            return NimbusJwtDecoder.withPublicKey(publicKey).build();
//        }
//    }


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

//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//





//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.addAllowedOrigin("*");
//        configuration.addAllowedOrigin("http://127.0.0.1");
//        configuration.addAllowedOrigin("http://192.168.3.10 ");
//        configuration.addAllowedOrigin("http://localhost:5173");
//        configuration.addAllowedMethod("*");
//        configuration.addAllowedHeader("*");
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }


}
