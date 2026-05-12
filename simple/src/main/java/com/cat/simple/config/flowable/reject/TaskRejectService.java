package com.cat.simple.config.flowable.reject;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务拒绝服务。
 * v1 策略：拒绝 = 写入拒绝信息后终止整个流程实例。
 * 终止事件由 {@link com.cat.simple.config.flowable.ProcessInstanceEndListener} 捕获并标记状态。
 */
@Slf4j
@Service
public class TaskRejectService {

    /** 流程变量：是否被拒绝 */
    public static final String VAR_REJECTED = "rejected";
    /** 流程变量：拒绝原因 */
    public static final String VAR_REJECT_REASON = "rejectReason";
    /** 流程变量：拒绝人 */
    public static final String VAR_REJECTED_BY = "rejectedBy";

    @Resource
    private TaskService taskService;

    @Resource
    private RuntimeService runtimeService;

    /**
     * 拒绝指定任务并终止流程实例。
     *
     * @param taskId 任务 ID
     * @param reason 拒绝原因
     * @throws IllegalArgumentException 任务不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, String reason) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put(VAR_REJECTED, true);
        vars.put(VAR_REJECT_REASON, reason);
        vars.put(VAR_REJECTED_BY, task.getAssignee());
        runtimeService.setVariables(task.getProcessInstanceId(), vars);

        log.info("拒绝任务 taskId={}, processInstanceId={}, reason={}",
                taskId, task.getProcessInstanceId(), reason);

        runtimeService.deleteProcessInstance(task.getProcessInstanceId(),
                "rejected: " + (reason == null ? "" : reason));
    }
}