package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.BackConfig;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.simple.config.flowable.back.BackConfigReader;
import com.cat.simple.config.flowable.back.BackEngine;
import com.cat.simple.config.flowable.back.BackTargetResolver;
import com.cat.simple.config.flowable.hook.BackContext;
import com.cat.simple.config.flowable.util.BpmnModelUtil;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;

public class BackTaskCommand extends ProcessCommand<Void> {

    @Resource private BackConfigReader backConfigReader;
    @Resource private BackTargetResolver backTargetResolver;
    @Resource private BackEngine backEngine;
    @Resource private BpmnModelUtil bpmnModelUtil;

    private final ProcessHandleParam param;
    private String resolvedTargetNodeId;

    public BackTaskCommand(ProcessHandleParam param) {
        this.param = param;
    }

    @Override
    protected void validate() {
        guard.assertInstanceActive(param.getProcessInstanceId());
        Task task = guard.assertTaskExists(param.getTaskId());
        String currentUserId = guard.getCurrentUserId();
        if (!currentUserId.equals(task.getAssignee())) {
            throw new IllegalStateException("当前用户不是该任务的办理人, taskId: " + param.getTaskId());
        }
    }

    @Override
    protected Void doExecute() {
        com.cat.common.entity.process.ProcessInstance instance =
                guard.getInstance(param.getProcessInstanceId());
        Task task = guard.getTask(param.getTaskId());
        String currentUserId = guard.getCurrentUserId();

        BackConfig cfg = backConfigReader.getBackConfig(param.getTaskId());
        if (!cfg.isAllowBack()) {
            throw new IllegalStateException("该节点未配置驳回方式, taskId: " + param.getTaskId());
        }

        resolvedTargetNodeId = backTargetResolver.resolveTargetNodeId(
                task, cfg.getBackType(), cfg.getBackNodeId(), param.getTargetNodeId());
        String targetNodeName = backTargetResolver.resolveTargetNodeName(
                instance.getProcessInstanceId(), resolvedTargetNodeId);

        boolean isMultiInstance = task.getProcessInstanceId() != null && bpmnModelUtil.isMultiInstance(task);

        if (isMultiInstance) {
            backEngine.backMultiInstanceAllBack(instance, task, resolvedTargetNodeId, currentUserId, param.getRemark(),
                    cfg.getBackAssigneePolicy(), targetNodeName);
        } else {
            backEngine.backSingleInstance(instance, task, resolvedTargetNodeId, currentUserId, param.getRemark(),
                    cfg.getBackAssigneePolicy(), targetNodeName);
        }

        recorder.recordBack(param, task, resolvedTargetNodeId, targetNodeName);
        return null;
    }

    @Override
    protected void record(Void result) {
        // record is called in doExecute because targetNodeId is needed
    }

    @Override
    protected void beforeHook() {
        BackConfig cfg = backConfigReader.getBackConfig(param.getTaskId());
        BackContext ctx = new BackContext(param.getProcessInstanceId(), param.getTaskId(), param.getRemark(), param.getTargetNodeId(), cfg);
        lifecycleHook.beforeBack(ctx);
    }

    @Override
    protected void afterHook(Void result) {
        lifecycleHook.afterBack(guard.getInstance(param.getProcessInstanceId()), resolvedTargetNodeId);
    }
}
