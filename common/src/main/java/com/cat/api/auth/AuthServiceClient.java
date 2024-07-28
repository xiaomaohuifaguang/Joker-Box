package com.cat.api.auth;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.LoginInfo;
import com.cat.common.entity.LoginUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/***
 * <TODO description class purpose>
 * @title AuthServiceClient
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/26 21:04
 **/
@FeignClient(value = "auth-server", path = "/auth-server", contextId = "AuthServiceClient")
@Component
public interface AuthServiceClient {

    @PostMapping("/auth/getToken")
    HttpResult<String> getToken(@RequestBody LoginInfo loginInfo);

    @PostMapping("/auth/getLoginUserByToken")
    HttpResult<LoginUser> getLoginUserByToken(@RequestHeader("Authorization") String authorization, @RequestBody LoginInfo loginInfo);

}
