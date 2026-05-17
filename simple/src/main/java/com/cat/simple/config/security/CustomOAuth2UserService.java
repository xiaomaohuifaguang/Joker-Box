package com.cat.simple.config.security;

import com.cat.common.entity.CONSTANTS;
import com.cat.common.entity.DTO;
import com.cat.common.entity.auth.RegisterUserInfo;
import com.cat.common.entity.auth.User;
import com.cat.common.utils.who.RandomUserNameUtils;
import com.cat.simple.system.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {


    // 使用默认的 OAuth2UserService 代理加载用户信息
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();


    @Resource
    private UserService userService;


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 判断 OAuth2 登录的来源
        String clientName = userRequest.getClientRegistration().getClientName();

        String clientNameDown = clientName.toLowerCase();

        // 账号
//        String username = oAuth2User.getAttribute("login");
        // id
        Integer id = oAuth2User.getAttribute("id");
        // 昵称
        String name = oAuth2User.getAttribute("name");

        String avatarUrl = oAuth2User.getAttribute("avatar_url");


        // 这里注意 会出问题 暂时没想好怎么设计 即正常注册的用户也可以使用 aaa_123 的 形式
        User userByClient = userService.getUserByClient(clientNameDown, id);

        if(ObjectUtils.isEmpty(userByClient)){
            RegisterUserInfo registerUserInfo = new RegisterUserInfo();

            String username;
            do{
                username = RandomUserNameUtils.make();
            }while (userService.exist(username));
            registerUserInfo.setUsername(username);

            registerUserInfo.setPassword(CONSTANTS.DEFAULT_PASSWORD);
            registerUserInfo.setNickname(name);
            registerUserInfo.setClientName(clientNameDown);
            registerUserInfo.setClientId(id);
            userService.register(registerUserInfo, false);
            User userByUsername = userService.getUserByUsername(registerUserInfo.getUsername());

            String finalUsername = username;
            new Thread(()->{
                try {
                    userService.avatarUpload(avatarUrl, String.valueOf(userByUsername.getIdStr()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();

        }

        return oAuth2User;
    }
}

