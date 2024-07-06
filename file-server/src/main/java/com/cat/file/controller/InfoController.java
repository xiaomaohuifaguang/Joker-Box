package com.cat.file.controller;

import com.cat.api.auth.AuthServiceClient;
import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.common.entity.LoginInfo;
import com.cat.common.entity.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/***
 * <TODO description class purpose>
 * @title InfoController
 * @description 系统信息接口
 * @author xiaomaohuifaguang
 * @create 2024/6/19 22:34
 **/
@RestController
@RequestMapping("/info")
@Tag(name = "服务信息")
public class InfoController {

    @Value("${custom.info.version}")
    private String version;

    @Resource
    private AuthServiceClient authServiceClient;

    @Operation(summary = "版本")
    @RequestMapping(value = "/version", method = RequestMethod.GET)
    public HttpResult<String> version() {
        HttpResult<LoginUser> loginUserByToken = authServiceClient.getLoginUserByToken(new LoginInfo().setToken("Bearer eyJjdXN0b20taGVhZGVyIjoiYzU0ODliZmEtMWVlMS00MjZiLThiMTAtM2VlMTA2ZGEzYTBkIiwiYWxnIjoiSFMyNTYifQ.eyJqdGkiOiJiODc5YTE4YS05NzMxLTQyNWUtYWU2ZS01ZDdkOGIzYzFhMjgiLCJpYXQiOjE3MTk0MDc1ODksImV4cCI6MTcyMDYxNzE4OSwicGFzc3dvcmQiOiIwMWFjOWQwY2IyNWVlODg5M2RiZWZkMzcyZDAwMThhYWI4MWZlMzIxNjMxYzdjZmMzMTExZTQ5OGVkNWEzOTM5IiwidXNlcklkIjoiMDAwMDAwMDAwMSIsInVzZXJuYW1lIjoiYWRtaW4ifQ.PgEOBq_txxqDP7ry-f7QNGUdYnATuMQsPldlLWg8uXo"));
        return HttpResult.back(HttpResultStatus.SUCCESS, version);
    }


}
