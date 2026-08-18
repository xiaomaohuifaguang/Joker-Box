package com.cat.simple.task;

import com.cat.simple.process.mapper.ProcessDefinitionMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ProcessTask {

    @Resource private RuntimeService runtimeService;
    @Resource private HistoryService historyService;
    @Resource private RepositoryService repositoryService;
    @Resource private ProcessDefinitionMapper processDefinitionMapper;



//    @Scheduled(initialDelay = 10, fixedDelay = 365 * 24 * 60 * 60, timeUnit = TimeUnit.SECONDS)
//    @SchedulerLock(name = "ProcessTask.init", lockAtMostFor = "30m")
    public void init(){

        processDefinitionMapper.deleteInstanceForm();
        processDefinitionMapper.deleteNodeFieldPermission();
        processDefinitionMapper.deleteHandleInfo();
        processDefinitionMapper.deleteGatewayConditionNode();
        processDefinitionMapper.deleteGatewayCondition();
        processDefinitionMapper.deleteDefinitionForm();
        processDefinitionMapper.deleteDefinitionBytearray();
        processDefinitionMapper.deleteDefinition();
        processDefinitionMapper.deleteInstance();



        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
        for (ProcessInstance instance : processInstances) {
            runtimeService.deleteProcessInstance(instance.getId(), "清空测试数据");
        }
        List<HistoricProcessInstance> historicInstances = historyService.createHistoricProcessInstanceQuery().list();
        for (HistoricProcessInstance historicInstance : historicInstances) {
            historyService.deleteHistoricProcessInstance(historicInstance.getId());
        }
        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            // true 表示级联删除，同时清理相关的流程实例和历史记录
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

}
