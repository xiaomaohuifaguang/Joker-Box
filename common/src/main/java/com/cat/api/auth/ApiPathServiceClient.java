package com.cat.api.auth;

import com.cat.common.entity.auth.ApiPath;
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
 * @title ApiPathServiceClient
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/12 1:44
 **/
@FeignClient(value = "auth-server", path = "/auth-server", contextId = "ApiPathServiceClient")
@Component
public interface ApiPathServiceClient {

    @RequestMapping(value = "/apiPath/saveBatch", method = RequestMethod.POST)
    HttpResult<?> saveBatch(@RequestParam("server") String server, @RequestBody List<ApiPath> apiPaths);

}
