package com.cat.api.auth;

import com.cat.common.entity.HttpResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/***
 * <TODO description class purpose>
 * @title RoleServiceClient
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/18 9:53
 **/
@FeignClient(value = "auth-server", path = "/auth-server", contextId = "RoleServiceClient")
@Component
public interface RoleServiceClient {

    @RequestMapping(value = "/role/allow", method = RequestMethod.POST)
    HttpResult<?> allow(@RequestBody List<String> userRoleIds,@RequestParam("server") String server, @RequestParam("path") String path);


}
