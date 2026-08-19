package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.flowable.gateway.GatewayConditionEngine;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.hook.context.StartContext;
import com.cat.simple.config.process.ProcessCodeGenerator;
import com.cat.simple.process.service.ProcessFormService;
import jakarta.annotation.Resource;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 启动流程命令，根据流程定义创建新的流程实例。
 */
public class StartProcessCommand extends ProcessCommand<ProcessInstance> {

    @Resource private RuntimeService runtimeService;
    @Resource private TaskService taskService;
    @Resource private ProcessCodeGenerator codeGenerator;
    @Resource private com.cat.simple.process.mapper.ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessFormService processFormService;
    @Resource private com.cat.simple.process.service.ProcessDefinitionService processDefinitionService;
    @Resource private RepositoryService repositoryService;
    @Resource private GatewayConditionEngine gatewayConditionEngine;

    private final ProcessHandleParam param;

    public StartProcessCommand(ProcessHandleParam param) {
        this.param = param;
    }

    @Override
    protected void validate() {
        guard.assertDefinitionPublished(param.getProcessDefinitionId());
    }

    @Override
    protected ProcessInstance doExecute() {
        com.cat.common.entity.process.ProcessDefinition definition =
                guard.assertDefinitionPublished(param.getProcessDefinitionId());
        String currentUserId = guard.getCurrentUserId();



        LocalDateTime now = LocalDateTime.now();

        ProcessInstance instance;
        if (Objects.nonNull(param.getProcessInstanceId())) {
            // 传了流程实例ID → 草稿发起：校验存在且为草稿
            instance = guard.assertInstanceDraft(param.getProcessInstanceId());
            if (!currentUserId.equals(instance.getCreateBy())) {
                throw new IllegalStateException("无权操作他人草稿: " + param.getProcessInstanceId());
            }
            instance.setTitle(param.getTitle())
                    .setCode(codeGenerator.generate())
                    .setUpdateTime(now);
            processInstanceMapper.updateById(instance);

            processFormService.writeFormData(instance, param.getGlobalFormData());

        } else {

            instance = new ProcessInstance()
                    .setProcessDefinitionId(definition.getId())
                    .setProcessDefinitionVersion(definition.getVersion())
                    .setTitle(param.getTitle())
                    .setCode(codeGenerator.generate())
                    .setProcessStatus(ProcessStatusEnum.DRAFT.getStatus())
                    .setCreateBy(currentUserId)
                    .setCreateTime(now)
                    .setUpdateTime(now);
            processInstanceMapper.insert(instance);
            processFormService.writeFormData(instance, param.getGlobalFormData());

        }

        org.flowable.engine.runtime.ProcessInstance flowableInstance =
                runtimeService.startProcessInstanceByKey(definition.getProcessKey(), String.valueOf(instance.getId()));
        instance.setProcessInstanceId(flowableInstance.getProcessInstanceId());

        processInstanceMapper.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProcessInstance>()
                .eq(ProcessInstance::getId, instance.getId())
                .set(ProcessInstance::getProcessDefinitionId, definition.getId())
                .set(ProcessInstance::getTitle, param.getTitle())
                .set(ProcessInstance::getProcessInstanceId, instance.getProcessInstanceId())
                .set(ProcessInstance::getProcessStatus, ProcessStatusEnum.ACTIVE.getStatus())
                .set(ProcessInstance::getUpdateTime, now));

//        // 创建表单实例并写入数据
//        String startNodeId = processDefinitionService.resolveStartEventNodeId(param.getProcessDefinitionId());
//        processFormService.createFormInstanceIfNeeded(instance.getId(), definition.getId(),
//                definition.getVersion(), startNodeId);
//        processFormService.writeFormData(instance.getId(), definition.getId(),
//                definition.getVersion(), startNodeId,
//                param.getNodeFormData(), param.getGlobalFormData(), false);

//        processFormService.writeFormData(instance, param.getGlobalFormData());

        // 兜底：trivial 流程立即结束
        if (runtimeService.createProcessInstanceQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .singleResult() == null) {
            processInstanceMapper.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, instance.getId())
                    .set(ProcessInstance::getProcessStatus, ProcessStatusEnum.COMPLETED.getStatus())
                    .set(ProcessInstance::getUpdateTime, now));
        }

        return instance;
    }

    @Override
    protected void record(ProcessInstance result) {
        recorder.recordApply(result, guard.getCurrentUserId());
    }

    @Override
    protected void beforeHook() {
        StartContext ctx = new StartContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.beforeStart(ctx);
            }
        }
    }

    @Override
    protected void afterHook(ProcessInstance result) {
        StartContext ctx = new StartContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.afterStart(ctx);
            }
        }

        List<Task> list = taskService.createTaskQuery().processInstanceId(result.getProcessInstanceId()).list();
        if(!CollectionUtils.isEmpty(list) && list.size() == 1 &&  list.get(0).getTaskDefinitionKey().equals("applyNode")){
            Task task = list.get(0);

            List<UserTask> nextUserTasks = findNextUserTasks(task.getTaskDefinitionKey(), task.getProcessInstanceId(), result);
            System.out.println(nextUserTasks);


            taskService.complete(task.getId());
        }


    }

    public List<UserTask> findNextUserTasks(String currentNodeId, String processInstanceId, ProcessInstance processInstance) {

        // 1. 获取 BpmnModel
        org.flowable.engine.runtime.ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (pi == null) {
            throw new IllegalArgumentException("流程实例不存在: " + processInstanceId);
        }

        BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        Process process = bpmnModel.getMainProcess();

        FlowElement currentElement = process.getFlowElement(currentNodeId);
        if (!(currentElement instanceof FlowNode)) {
            throw new IllegalArgumentException("节点不存在或非FlowNode: " + currentNodeId);
        }

        // ✅ 关键修正：不从 currentNodeId 开始递归
        // 而是直接遍历它的出线，从 targetRef 开始找
        List<UserTask> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        FlowNode currentNode = (FlowNode) currentElement;

        for (SequenceFlow sf : currentNode.getOutgoingFlows()) {
            if (true) {
                collectNextUserTasks(process, sf.getTargetRef(), processInstanceId, result, visited, processInstance);
            }
        }

        return result;
    }

    private void collectNextUserTasks(Process process,
                                      String elementId,
                                      String processInstanceId,
                                      List<UserTask> result,
                                      Set<String> visited,
                                      ProcessInstance processInstance) {
        // 防死循环
        if (!visited.add(elementId)) {
            return;
        }

        FlowElement element = process.getFlowElement(elementId);
        if (element == null) {
            return;
        }

        // ✅ 命中 UserTask → 收集并停止该分支
        if (element instanceof UserTask) {
            result.add((UserTask) element);
            return;
        }

        // ✅ 是 FlowNode（网关 / ServiceTask / 中间事件等）→ 沿出线继续
        if (element instanceof FlowNode) {
            FlowNode flowNode = (FlowNode) element;

            if(flowNode.getOutgoingFlows().size() == 1){
                SequenceFlow sf = flowNode.getOutgoingFlows().get(0);
                collectNextUserTasks(
                        process,
                        sf.getTargetRef(),
                        processInstanceId,
                        result,
                        visited,
                        processInstance
                );
            }else {
                if(element instanceof ExclusiveGateway){
                    for (SequenceFlow sf : flowNode.getOutgoingFlows()) {
                        // 🔑 关键：调用你已有的条件判断方法
                        // 返回 true → 这条线可走；返回 false → 跳过
                        if (gatewayConditionEngine.evaluate(processInstance.getProcessDefinitionId(), processInstance.getProcessDefinitionVersion(), sf.getId(), processInstance)) {
                            collectNextUserTasks(
                                    process,
                                    sf.getTargetRef(),
                                    processInstanceId,
                                    result,
                                    visited,
                                    processInstance
                            );
                            break;
                        }
                    }
                }
                else if(element instanceof ParallelGateway){
                    for (SequenceFlow sf : flowNode.getOutgoingFlows()) {
                        // 🔑 关键：调用你已有的条件判断方法
                        // 返回 true → 这条线可走；返回 false → 跳过
                        if (true) {
                            collectNextUserTasks(
                                    process,
                                    sf.getTargetRef(),
                                    processInstanceId,
                                    result,
                                    visited,
                                    processInstance
                            );
                        }
                    }
                }
                else if(element instanceof InclusiveGateway){
                    for (SequenceFlow sf : flowNode.getOutgoingFlows()) {
                        // 🔑 关键：调用你已有的条件判断方法
                        // 返回 true → 这条线可走；返回 false → 跳过
                        if (gatewayConditionEngine.evaluate(processInstance.getProcessDefinitionId(), processInstance.getProcessDefinitionVersion(), sf.getId(), processInstance)) {
                            collectNextUserTasks(
                                    process,
                                    sf.getTargetRef(),
                                    processInstanceId,
                                    result,
                                    visited,
                                    processInstance
                            );
                        }
                    }
                }else {
                    for (SequenceFlow sf : flowNode.getOutgoingFlows()) {
                        // 🔑 关键：调用你已有的条件判断方法
                        // 返回 true → 这条线可走；返回 false → 跳过
                        // TODO 可选是否允许给走线配置条件 不建议
                        if (true) {
                            collectNextUserTasks(
                                    process,
                                    sf.getTargetRef(),
                                    processInstanceId,
                                    result,
                                    visited,
                                    processInstance
                            );
                        }
                    }
                }


            }
        }

    }


}