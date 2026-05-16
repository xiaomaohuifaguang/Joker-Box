package com.cat.simple.service;

import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.process.ProcessBackParam;
import com.cat.common.entity.process.ProcessInstance;
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
public class ProcessBackServiceTest {

    @Autowired
    private ProcessBackService processBackService;

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

    @Disabled("需要有效的 taskId，请先启动流程实例并替换占位符")
    @Test
    void testGetBackConfig() {
        // 需要先启动一个流程并获取任务ID
        // 这里使用数据库中已有的流程实例和任务进行测试
        // 实际测试时需要替换为有效的 taskId
        String taskId = "REPLACE_WITH_VALID_TASK_ID";

        var config = processBackService.getBackConfig(taskId);
        assertNotNull(config);
    }

    @Disabled("需要有效的 taskId，请先启动流程实例并替换占位符")
    @Test
    void testGetAvailableBackTargets() {
        String taskId = "REPLACE_WITH_VALID_TASK_ID";

        var targets = processBackService.getAvailableBackTargets(taskId);
        assertNotNull(targets);
    }
}
