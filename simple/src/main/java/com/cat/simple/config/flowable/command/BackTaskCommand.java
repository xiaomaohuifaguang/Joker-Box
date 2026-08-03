package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.BackConfig;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.simple.config.flowable.engine.BackEngine;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.hook.context.BackContext;
import com.cat.simple.config.flowable.util.FlowableUtils;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;
import org.springframework.util.CollectionUtils;

/**
 * 驳回任务命令，将当前任务回退到指定或上一节点。
 */
public class BackTaskCommand extends ProcessCommand<Void> {


    @Resource private FlowableUtils flowableUtils;
    @Resource private BackEngine backEngine;

    private final ProcessHandleParam param;
    private Task task;

    public BackTaskCommand(ProcessHandleParam param) {
        this.param = param;
    }

    @Override
    protected void validate() {
        guard.assertInstanceActive(param.getProcessInstanceId());
        guard.assertTaskAssignee(param.getTaskId());
    }

    @Override
    protected Void doExecute() {
        com.cat.common.entity.process.ProcessInstance instance =
                guard.getInstance(param.getProcessInstanceId());
        task = guard.getTask(param.getTaskId());
//        String currentUserId = guard.getCurrentUserId();
//
//        BackConfig cfg = flowableUtils.getBackConfig(param.getTaskId());
//
//        if (!cfg.isAllowBack()) {
//            throw new IllegalStateException("该节点未配置驳回方式, taskId: " + param.getTaskId());
//        }


        backEngine.back(param, task);


//        param.setTargetNodeId(flowableUtils.resolveTargetNodeId(
//                task, cfg.getBackType(), cfg.getBackNodeId(), param.getTargetNodeId()));
//        param.setTargetNodeName(flowableUtils.resolveTargetNodeName(
//                instance.getProcessInstanceId(), param.getTargetNodeId()));
//
//        boolean isMultiInstance = task.getProcessInstanceId() != null && flowableUtils.isMultiInstance(task);
//
//        if (isMultiInstance) {
//            flowableUtils.backMultiInstanceAllBack(instance, task, param.getTargetNodeId(), currentUserId, param.getRemark(),
//                    cfg.getBackAssigneePolicy(), param.getTargetNodeName());
//        } else {
//            flowableUtils.backSingleInstance(instance, task, param.getTargetNodeId(), currentUserId, param.getRemark(),
//                    cfg.getBackAssigneePolicy(), param.getTargetNodeName());
//        }


        return null;
    }

    @Override
    protected void record(Void result) {
        recorder.recordBack(param, task);
    }

    @Override
    protected void beforeHook() {
        BackContext ctx = new BackContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.beforeBack(ctx);
            }
        }
    }

    @Override
    protected void afterHook(Void result) {
        BackContext ctx = new BackContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.afterBack(ctx);
            }
        }
    }



}
