package com.cat.file.config.mapping;


import com.cat.api.auth.ApiPathServiceClient;
import com.cat.api.auth.AuthServiceClient;
import com.cat.common.entity.auth.ApiPath;
import com.cat.file.config.security.SecurityConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/***
 * 读取全部控制层接口
 * @title EndpointMappingConfiguration
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/11 0:17
 **/

@Configuration
@Slf4j
public class EndpointMappingConfiguration {

    @Value("${spring.application.name}")
    private String server;

    @Resource
    private ApiPathServiceClient apiPathServiceClient;
    @Resource
    private AuthServiceClient authServiceClient;

    @Bean
    public CommandLineRunner commandLineRunner(RequestMappingHandlerMapping handlerMapping) {
        return args -> {
            long startTime = System.currentTimeMillis();
            log.info("apiPath录入开始...");
            // 打印所有的URL映射
            List<ApiPath> apiPaths = new ArrayList<>();
            handlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
                // 过滤非com.cat包下的
                if (handler.getBeanType().getName().startsWith("com.cat")) {
                    Class<?> beanType = handler.getBeanType();
                    // 与 knife4j联合使用 获取控制层描述
                    Tag tag = beanType.getAnnotation(Tag.class);
                    // 获取方法
                    Method method = handler.getMethod();
                    // 与 knife4j联合使用 获取方法描述
                    Operation annotation = method.getAnnotation(Operation.class);
                    if (mapping.getPathPatternsCondition() != null) {
                        String groupName = tag != null ? tag.name() : null;
                        String name = annotation != null ? annotation.summary() : null;
                        Set<PathPattern> patterns = mapping.getPathPatternsCondition().getPatterns();
                        patterns.forEach(p -> {
                            ApiPath apiPath = new ApiPath();
                            apiPath.setPath(p.toString());
                            apiPath.setServer(server);
                            apiPath.setWhiteList(SecurityConfig.WHITE_LIST.contains(p.toString()) ? "1" : "0");
                            apiPath.setGroupName(groupName);
                            apiPath.setName(name);
                            apiPaths.add(apiPath);
                        });
                    }
                }
            });
            // 全量处理
            apiPathServiceClient.saveBatch(server, apiPaths);
            log.info("apiPath录入结束-耗时"+(System.currentTimeMillis()-startTime)+"ms");
        };
    }
}
