package com.cat.simple.config.security;
import com.alibaba.fastjson2.JSONArray;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.utils.JSONUtils;
import com.cat.simple.config.redis.RedisService;
import com.cat.simple.system.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.cat.common.entity.CONSTANTS.*;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${custom.page-server}")
    private String customPageServer;

    @Resource
    private UserService userService;

    @Resource
    private RedisService redisService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 获取 OAuth2 登录来源
        String clientName = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        String clientNameDown = clientName.toLowerCase();

        // 获取 GitHub 用户信息
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // id
        Integer id = oAuth2User.getAttribute("id");


        LoginUser loginUser = userService.getLoginUser(clientNameDown , id);
        String token = userService.makeToken(loginUser);

        redisService.set(REDIS_SSO+clientNameDown+":"+id, token, 60*60*24);


        List<String> tokens;
        String  tokensStr = redisService.get(REDIS_PARENT_TOKEN + loginUser.getUsername(), String.class);
        if(StringUtils.hasText(tokensStr)){
            tokens = JSONArray.parseArray(tokensStr, String.class);
        }else {
            tokens = new ArrayList<>();
        }

        if(tokens.size() == MAX_LOGIN){
            tokens.remove(0);
        }

        tokens.add(token);
        redisService.set(REDIS_PARENT_TOKEN + loginUser.getUsername(), JSONUtils.toJSONString(tokens), tokenExpire);


        // 保存用户信息到数据库
        // 或者更新当前用户的会话信息

        response.sendRedirect(customPageServer+"/login?msg=success&clientName="+clientNameDown+"&id="+id);  // 登录成功后重定向到首页
//        ServletUtils.back(HttpResult.back(oAuth2User).setMsg("登录成功"), response);
    }
}

