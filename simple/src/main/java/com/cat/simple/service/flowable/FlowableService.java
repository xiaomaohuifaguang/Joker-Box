package com.cat.simple.service.flowable;

import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;

import java.util.List;

public interface FlowableService {

    /**
     * 获取全部部署
     * @return 部署列表
     */
    List<Deployment> getAllDeployments();


    /**
     * 获取全部流程
     * @return 流程列表
     */
    List<ProcessDefinition> getAllProcessDefinitions();




}
