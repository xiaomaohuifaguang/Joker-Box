package com.cat.simple.process.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 1. 注册为 Spring Bean，名称必须为 demoService
@Component("delegateDemoService")
public class DemoServiceTask implements JavaDelegate {



    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("测试任务");
    }
}