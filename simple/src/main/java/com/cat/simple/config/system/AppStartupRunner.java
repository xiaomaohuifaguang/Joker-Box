package com.cat.simple.config.system;

import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppStartupRunner implements ApplicationRunner {

    @Resource
    private OpenBrowser openBrowser;

    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 应用启动完成后执行的逻辑
//        openBrowser.open();
    }
}