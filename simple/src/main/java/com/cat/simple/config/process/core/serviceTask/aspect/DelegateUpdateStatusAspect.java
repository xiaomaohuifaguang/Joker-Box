package com.cat.simple.config.process.core.serviceTask.aspect;

import com.cat.common.entity.process.enums.HandleButtonEnum;
import com.cat.simple.service.ProcessInstanceService;
import jakarta.annotation.Resource;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class DelegateUpdateStatusAspect {


    @Resource
    private ProcessInstanceService processInstanceService;
    @Resource
    private ProcessEngine processEngine;



    // 修正切入点表达式：明确指定拦截JavaDelegate.execute方法
    @After("execution(* com.cat.simple.config.process.core.serviceTask.excute.*.execute(org.flowable.engine.delegate.DelegateExecution)) && args(execution)")
    public void afterDelegateExecution(DelegateExecution execution) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(execution.getProcessDefinitionId());
        // 获取当前节点信息
        FlowElement flowElement = bpmnModel.getFlowElement(execution.getCurrentActivityId());

        // 更新自用表 流程状态
        LocalDateTime now = LocalDateTime.now();
        processInstanceService.updateStatus(execution.getProcessInstanceId(), null, now);
        processInstanceService.saveHandleInfo(
                execution.getProcessInstanceId(),
                null,
                flowElement.getName(),
                HandleButtonEnum.SYSTEM_TASK.getName(),
                HandleButtonEnum.SYSTEM_TASK.getCode(),
                HandleButtonEnum.SYSTEM_TASK.getName()+"->"+flowElement.getName(),
                now);

    }


}
