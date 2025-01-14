package com.cat.simple.service.flowable.impl;

import com.cat.simple.service.flowable.FlowableService;
import jakarta.annotation.Resource;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlowableServiceImpl implements FlowableService {

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private ProcessEngine processEngine;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;


    @Override
    public List<Deployment> getAllDeployments() {
        return repositoryService.createDeploymentQuery().list();
    }

    @Override
    public List<ProcessDefinition> getAllProcessDefinitions() {
        return repositoryService.createProcessDefinitionQuery().list();
    }
}
