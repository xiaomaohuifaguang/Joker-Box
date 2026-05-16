package com.cat.simple.service;

import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.auth.Role;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.common.entity.process.ProcessInstancePageParam;
import com.cat.common.entity.Page;
import com.cat.simple.config.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProcessInstanceServiceTest {

    @Autowired
    private ProcessInstanceService processInstanceService;

    private static final String TEST_USER_ID = "1";

    @BeforeEach
    void setUp() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(TEST_USER_ID);
        loginUser.setUsername("test");
        loginUser.setNickname("testUser");
        loginUser.setPassword("123456");
        loginUser.setType("0");
        loginUser.setRoles(List.of());
        loginUser.setOrgs(List.of());

        UserDetailsImpl userDetails = new UserDetailsImpl(loginUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testQueryPage() {
        ProcessInstancePageParam pageParam = new ProcessInstancePageParam();
        pageParam.setCurrent(1);
        pageParam.setSize(10);

        Page<ProcessInstance> page = processInstanceService.queryPage(pageParam);
        assertNotNull(page);
    }


    @Test
    void testStart() {
        // 请先替换为数据库中存在的流程定义 ID
        Integer processDefinitionId = 48;
        ProcessInstance instance = processInstanceService.start(processDefinitionId, null);
        assertNotNull(instance);
    }

    @Disabled("需要有效的 taskId，请先启动流程实例并替换占位符")
    @Test
    void testGetBackConfig() {
        String taskId = "REPLACE_WITH_VALID_TASK_ID";
        var config = processInstanceService.getBackConfig(taskId);
        assertNotNull(config);
    }

    @Disabled("需要有效的 taskId，请先启动流程实例并替换占位符")
    @Test
    void testGetAvailableBackTargets() {
        String taskId = "REPLACE_WITH_VALID_TASK_ID";
        var targets = processInstanceService.getAvailableBackTargets(taskId);
        assertNotNull(targets);
    }

}
