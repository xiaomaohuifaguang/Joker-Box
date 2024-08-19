package com.cat.demo.config.feign;

import com.cat.api.auth.AuthServiceClient;
import com.cat.common.entity.auth.LoginInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/***
 * 鉴权工具
 * @title AuthUtils
 * @description 鉴权工具
 * @author xiaomaohuifaguang
 * @create 2024/7/28 15:32
 **/
@Component
public class AuthUtils {

    @Value("${custom.username}")
    private String username;
    @Value("${custom.password}")
    private String password;

    @Resource
    private AuthServiceClient authServiceClient;

    public String getToken(){
        return authServiceClient.getToken(new LoginInfo(username,password,null)).getData();
    }


}
