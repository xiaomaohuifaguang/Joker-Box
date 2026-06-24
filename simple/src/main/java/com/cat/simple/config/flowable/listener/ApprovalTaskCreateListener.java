package com.cat.simple.config.flowable.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.approval.ApprovalTypeEnum;
import com.cat.simple.config.flowable.approval.ApprovalTypeHandler;
import com.cat.simple.config.flowable.variable.ProcessVariableStore;
import com.cat.simple.config.flowable.variable.VariableNames;
import com.cat.simple.process.mapper.ProcessInstanceMapper;
import com.cat.simple.process.service.ProcessFormService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 审批任务创建监听器，由 {@link com.cat.simple.config.flowable.parse.ApprovalUserTaskParseHandler}
 * 在解析期为每个带 approvalType 的 UserTask 注册。
 * 通过 Spring Bean 表达式 {@code ${approvalTaskCreateListener}} 被 Flowable 调用。
 */
@Slf4j
@Component("approvalTaskCreateListener")
public class ApprovalTaskCreateListener implements TaskListener {

    @Resource
    private List<ApprovalTypeHandler> handlers;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private ProcessVariableStore variableStore;

    @Resource
    private ProcessInstanceMapper processInstanceMapper;

    @Resource
    private ProcessFormService processFormService;

    private Map<ApprovalTypeEnum, ApprovalTypeHandler> handlerMap;

    /**
     * 将所有 ApprovalTypeHandler Bean 按支持类型建立索引，便于 O(1) 分发。
     */
    @PostConstruct
    public void init() {
        handlerMap = new EnumMap<>(ApprovalTypeEnum.class);
        for (ApprovalTypeHandler handler : handlers) {
            handlerMap.put(handler.supports(), handler);
        }
    }


    @Override
    public void notify(DelegateTask delegateTask) {
        UserTask userTask = lookupUserTask(delegateTask);
        if (userTask == null) {
            return;
        }
        ApprovalContext ctx = ApprovalContext.from(userTask);
        if (ctx != null) {
            ApprovalTypeHandler handler = handlerMap.get(ctx.type());
            if (handler == null) {
                log.warn("approvalType={} 无匹配 handler, taskId={}", ctx.type(), delegateTask.getId());
            } else {
                handler.applyOnCreate(delegateTask, ctx);

                // 将按钮配置写入任务变量，供后端接口查询和校验
                if (ctx.actionButtons() != null && !ctx.actionButtons().isEmpty()) {
                    variableStore.setLocal(delegateTask, VariableNames.ACTION_BUTTONS, String.join(",", ctx.actionButtons()));
                }
                if (ctx.backType() != null && !ctx.backType().isBlank()) {
                    variableStore.setLocal(delegateTask, VariableNames.BACK_TYPE, ctx.backType());
                }
                if (ctx.backNodeId() != null && !ctx.backNodeId().isBlank()) {
                    variableStore.setLocal(delegateTask, VariableNames.BACK_NODE_ID, ctx.backNodeId());
                }
                if (ctx.backAssigneePolicy() != null && !ctx.backAssigneePolicy().isBlank()) {
                    variableStore.setLocal(delegateTask, VariableNames.BACK_ASSIGNEE_POLICY, ctx.backAssigneePolicy());
                }
            }
        }
        createNodeFormInstance(delegateTask);
    }

    /**
     * 根据 task 中的流程定义信息反查 BPMN 模型中的 UserTask 节点。
     *
     * @param delegateTask Flowable 任务委托对象
     * @return 对应的 BPMN UserTask 模型，找不到返回 {@code null}
     */
    private UserTask lookupUserTask(DelegateTask delegateTask) {
        BpmnModel model = repositoryService.getBpmnModel(delegateTask.getProcessDefinitionId());
        if (model == null) {
            return null;
        }
        return model.getFlowElement(delegateTask.getTaskDefinitionKey()) instanceof UserTask ut ? ut : null;
    }

    /**
     * 为当前任务节点创建表单实例（如果该节点绑定了表单）。
     * 即使表单实例创建失败也不应阻塞任务创建，因此用 try-catch 包裹。
     */
    private void createNodeFormInstance(DelegateTask delegateTask) {
        try {
            String flowableInstanceId = delegateTask.getProcessInstanceId();
            ProcessInstance instance = processInstanceMapper.selectOne(
                    new LambdaQueryWrapper<ProcessInstance>()
                            .eq(ProcessInstance::getProcessInstanceId, flowableInstanceId));
            if (instance == null) {
                return;
            }
            String nodeId = delegateTask.getTaskDefinitionKey();
            processFormService.createFormInstanceIfNeeded(
                    instance.getId(), instance.getProcessDefinitionId(),
                    instance.getProcessDefinitionVersion(), nodeId);
        } catch (Exception e) {
            log.error("创建节点表单实例失败, taskId={}", delegateTask.getId(), e);
        }
    }
}