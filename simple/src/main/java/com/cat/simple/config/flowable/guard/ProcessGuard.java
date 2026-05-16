package com.cat.simple.config.flowable.guard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.process.ProcessDefinition;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.ProcessDefinitionMapper;
import com.cat.simple.mapper.ProcessInstanceMapper;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProcessGuard {

    @Resource private ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessDefinitionMapper processDefinitionMapper;
    @Resource private TaskService taskService;
    @Resource private RuntimeService runtimeService;
    @Resource private HistoryService historyService;

    public String getCurrentUserId() {
        return Optional.ofNullable(SecurityUtils.getLoginUser())
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前未登录"));
    }

    public ProcessInstance assertInstanceActive(Integer processInstanceId) {
        ProcessInstance instance = processInstanceMapper.selectById(processInstanceId);
        if (instance == null) {
            throw new IllegalArgumentException("流程实例不存在: " + processInstanceId);
        }
        if (!ProcessStatusEnum.ACTIVE.getStatus().equals(instance.getProcessStatus())) {
            throw new IllegalStateException("流程实例非审批中状态: " + processInstanceId);
        }
        return instance;
    }

    public ProcessInstance assertInstanceDraft(Integer processInstanceId) {
        ProcessInstance instance = processInstanceMapper.selectById(processInstanceId);
        if (instance == null) {
            throw new IllegalArgumentException("草稿不存在: " + processInstanceId);
        }
        if (!ProcessStatusEnum.DRAFT.getStatus().equals(instance.getProcessStatus())) {
            throw new IllegalStateException("该流程实例不是草稿状态: " + processInstanceId);
        }
        return instance;
    }

    public Task assertTaskAssignee(String taskId) {
        String userId = getCurrentUserId();
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .taskAssignee(userId)
                .singleResult();
        if (task == null) {
            throw new IllegalStateException("当前用户没有该任务的办理权限, taskId: " + taskId);
        }
        return task;
    }

    public Task assertTaskCandidate(String taskId) {
        String userId = getCurrentUserId();
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .taskCandidateUser(userId)
                .singleResult();
        if (task == null) {
            throw new IllegalStateException("当前用户没有该任务的认领权限, taskId: " + taskId);
        }
        return task;
    }

    public Task assertTaskExists(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return task;
    }

    public ProcessDefinition assertDefinitionPublished(Integer definitionId) {
        ProcessDefinition definition = processDefinitionMapper.selectById(definitionId);
        if (definition == null) {
            throw new IllegalArgumentException("流程定义不存在: " + definitionId);
        }
        if (!"1".equals(definition.getStatus())) {
            throw new IllegalStateException("流程定义未发布: " + definitionId);
        }
        return definition;
    }

    public ProcessInstance getInstance(Integer id) {
        return processInstanceMapper.selectById(id);
    }

    public Task getTask(String taskId) {
        return taskService.createTaskQuery().taskId(taskId).singleResult();
    }

    public ProcessInstance selectByFlowableId(String flowableProcessInstanceId) {
        return processInstanceMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstance>()
                        .eq(ProcessInstance::getProcessInstanceId, flowableProcessInstanceId));
    }
}
