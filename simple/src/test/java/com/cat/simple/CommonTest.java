package com.cat.simple;

import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.auth.UserInfo;
import com.cat.simple.config.security.UserDetailsImpl;
import com.cat.simple.system.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;

@Slf4j
@SpringBootTest
@Transactional
public class CommonTest {


    @Resource
    private UserService userService;


    @AfterEach
    void clearSecurityContext() {
        // 测试结束后务必清理，防止影响其他测试用例
        SecurityContextHolder.clearContext();
    }


    @Test
    public void test(){

        LoginUser loginUser = userService.getLoginUser("admin");

        UserDetailsImpl userDetails = new UserDetailsImpl(loginUser);
        // 保存用户信息 到SecurityContextHolder
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticationToken);
        SecurityContextHolder.setContext(context);


        UserInfo userInfo = userService.getUserInfo();

        log.info(userInfo.toString());

    }

    @Test
    public void cmd() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "pandoc", "test1.docx", "-o", "test1.md", "--extract-media=./media"
        );

        pb.directory(new File("C:\\Users\\six6\\todo\\tmp\\文件转换测试"));

        pb.redirectErrorStream(true);

        Process process = pb.start();
        int exitCode = process.waitFor();
        System.out.println("退出码: " + exitCode);
    }


}
