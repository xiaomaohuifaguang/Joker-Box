package com.cat.simple;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.LocalMultipartFile;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.auth.UserInfo;
import com.cat.common.entity.minerU.ParseRequest;
import com.cat.common.entity.minerU.ParseResponse;
import com.cat.common.entity.minerU.TaskResponse;
import com.cat.common.entity.minerU.TaskResultResponse;
import com.cat.common.utils.JSONUtils;
import com.cat.simple.config.security.UserDetailsImpl;
import com.cat.simple.file.service.FileService;
import com.cat.simple.remote.ace.AceClient;
import com.cat.simple.remote.mineru.MinerUClient;
import com.cat.simple.system.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@SpringBootTest
@Transactional
public class CommonTest {


    @Resource
    private UserService userService;

    @Resource
    private AceClient aceClient;

    @Resource
    private MinerUClient minerUClient;

    @Resource
    private FileService fileService;


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

        pb.directory(new File("文件转换测试"));

        pb.redirectErrorStream(true);

        Process process = pb.start();
        int exitCode = process.waitFor();
        System.out.println("退出码: " + exitCode);
    }

    @Test
    public void aceTest(){
        HttpResult<?> alive = aceClient.alive();
        log.info(alive.toString());
    }


    @Test
    public void minerUTest() throws IOException {

        File file = new File("C:\\Users\\six6\\Desktop\\图片1.png");

        // 自动探测 ContentType
        String contentType = Files.probeContentType(file.toPath());

        // 如果探测不到（某些系统可能返回 null），给个默认值
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // "application/octet-stream"
        }

        MockMultipartFile multipart = new MockMultipartFile(
                "files",                       // name，必须和 @RequestPart 一致
                "我是图片.png",                // 原始文件名
                contentType,     // Content-Type
                new FileInputStream(file)      // 文件流
        );


//        ParseRequest parseRequest = new ParseRequest();
//        parseRequest.setFiles(new MultipartFile[]{multipart});
//        TaskResponse taskResponse = minerUClient.tasks(parseRequest);
//
//        log.info(JSONUtils.toJSONString(taskResponse));
//
//        log.info(minerUClient.taskStatus(taskResponse.getTaskId()).getStatus());

        TaskResultResponse taskResultResponse = minerUClient.taskResult("cb2ba521-d151-4fa0-b1a9-22490e47f0cc");
        log.info(taskResultResponse.getResults().toString());


    }


    @Test
    public void saveLocal() throws IOException {
        String savePath = fileService.saveLocalAgentFileById("fddb445276b84f4a9e36e3009ad9a1ff");
        System.out.println(savePath);

//        String savePath = fileService.saveLocalAgentFileById(fileId);

        File file = new File(savePath);
        String contentType = Files.probeContentType(file.toPath());
        // 如果探测不到（某些系统可能返回 null），给个默认值
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // "application/octet-stream"
        }

        LocalMultipartFile files = new LocalMultipartFile(file, "files", contentType);


        ParseRequest parseRequest = new ParseRequest();
        parseRequest.setFiles(new MultipartFile[]{files});
//        parseRequest.setBackend("pipeline");
        ParseResponse parseResponse = minerUClient.fileParse(parseRequest);
        String fileName = file.getName();
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        System.out.println(parseResponse.getResults().get(nameWithoutExt).getMd_content());

        boolean delete = file.delete();

    }


}
