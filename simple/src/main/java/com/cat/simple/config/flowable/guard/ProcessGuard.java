package com.cat.simple.config.flowable.guard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.process.ProcessDefinition;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.core.CatTaskInfo;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.process.mapper.ProcessDefinitionMapper;
import com.cat.simple.process.mapper.ProcessInstanceMapper;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 流程守卫，集中封装流程实例、任务、流程定义的状态校验与查询逻辑。
 * 所有命令在执行前通过此类完成权限与状态断言。
 */
@Component
public class ProcessGuard {

    @Resource private ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessDefinitionMapper processDefinitionMapper;
    @Resource private TaskService taskService;
    @Resource private RuntimeService runtimeService;
    @Resource private HistoryService historyService;

    /** 获取当前登录用户 ID，未登录则抛异常。 */
    public String getCurrentUserId() {
        return Optional.ofNullable(SecurityUtils.getLoginUser())
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前未登录"));
    }

    /** 断言流程实例存在且处于审批中状态。 */
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

    /** 断言流程实例存在且处于草稿状态。 */
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

    /** 断言当前用户是指定任务的办理人。 */
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

    /** 断言当前用户是指定任务的候选人。 */
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

    /** 断言任务存在。 */
    public Task assertTaskExists(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return task;
    }

    /** 断言流程定义存在且已发布。 */
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

    public ProcessDefinition assertDefinitionExistAndNoDraft(Integer definitionId) {
        ProcessDefinition definition = processDefinitionMapper.selectById(definitionId);
        if (definition == null) {
            throw new IllegalArgumentException("流程定义不存在: " + definitionId);
        }
        if ("0".equals(definition.getStatus())) {
            throw new IllegalStateException("流程定义未经发布: " + definitionId);
        }
        return definition;
    }

    /** 按业务 ID 查询流程实例。 */
    public ProcessInstance getInstance(Integer id) {
        return processInstanceMapper.selectById(id);
    }

    /** 按任务 ID 查询任务。 */
    public Task getTask(String taskId) {
        return taskService.createTaskQuery().taskId(taskId).singleResult();
    }

    /** 按 Flowable 流程实例 ID 查询业务流程实例。 */
    public ProcessInstance selectByFlowableId(String flowableProcessInstanceId) {
        return processInstanceMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstance>()
                        .eq(ProcessInstance::getProcessInstanceId, flowableProcessInstanceId));
    }

    public ProcessInstance selectByFlowableBusinessKey(String businessKey) {
        return processInstanceMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstance>()
                        .eq(ProcessInstance::getId, businessKey));
    }


    public CatTaskInfo toTaskInfo(Task task){

        String execId = task.getExecutionId();
        String keyId = execId; // 默认：普通节点用自己

        // 第一查：当前 execution
        Execution execution = runtimeService.createExecutionQuery()
                .executionId(execId)
                .singleResult();

        // 判断是否 MI 子执行：不看 getParent()（会懒加载炸），看 parentId 是否存在，
        // 再查父执行，用 isMi... 的公开判断方式
        if (execution != null && execution.getParentId() != null) {
            Execution parent = runtimeService.createExecutionQuery()
                    .executionId(execution.getParentId())
                    .singleResult();
            // ExecutionQuery 查出来的实例是 ExecutionEntity，可安全强转
            // MI root 的 scopeType... 用活动类型判断
            if (parent instanceof ExecutionEntity
                    && isMultiInstanceRoot((ExecutionEntity) parent)) {
                keyId = parent.getId(); // MI 场景：key 用 MI root
            }
        }

        String varName = "ROUND_" + keyId + "_" + task.getTaskDefinitionKey();

        Integer round = (Integer) taskService.getVariable(task.getId(), varName);


        if (round == null) {
            round = 1;
        }

        CatTaskInfo info = new CatTaskInfo();
        info.setRound(round);
        return info;
    }

    private boolean isMultiInstanceRoot(ExecutionEntity parent) {
        // MI root 的活动ID 与子执行相同，且 activityId 非空
        return parent.getActivityId() != null
                && runtimeService.createExecutionQuery()
                .executionId(parent.getId())
                .count() > 0
                && parent.getProcessInstanceId().equals(parent.getRootProcessInstanceId());
    }

}
