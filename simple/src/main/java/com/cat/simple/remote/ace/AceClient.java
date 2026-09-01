package com.cat.simple.remote.ace;

import com.cat.common.entity.HttpResult;
import com.cat.simple.config.feign.AceClientFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "joker-box-ace", path = "", configuration = AceClientFeignConfig.class)
public interface AceClient {

    @GetMapping("/alive")    // 每个方法都写全
    HttpResult<?> alive();


}
