package com.cat.simple.service.impl;

import com.cat.common.entity.process.*;
import com.cat.common.entity.auth.LoginUser;
import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.candidate.CandidateResolver;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.ProcessHandleInfoMapper;
import com.cat.simple.mapper.ProcessInstanceMapper;
import com.cat.simple.service.ProcessBackService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.*;

import static com.cat.simple.config.flowable.constant.ProcessConstants.*;

@Slf4j
@Service
public class ProcessBackServiceImpl implements ProcessBackService {

    @Resource
    private ProcessInstanceMapper processInstanceMapper;
    @Resource
    private ProcessHandleInfoMapper processHandleInfoMapper;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private TaskService taskService;
    @Resource
    private HistoryService historyService;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private CandidateResolver candidateResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void back(ProcessBackParam param) {
        // 1. 校验并获取基础对象
        ProcessInstance instance = validateAndGetInstance(param.getProcessInstanceId());
        Task task = validateAndGetTask(param.getTaskId());
        String currentUserId = getCurrentUserId();

        // 校验当前用户是任务办理人
        if (!currentUserId.equals(task.getAssignee())) {
            throw new IllegalStateException("当前用户不是该任务的办理人, taskId: " + param.getTaskId());
        }

        // 2. 读取节点配置
        String backType = getTaskLocalVar(task, EL_BACK_TYPE);
        String backNodeId = getTaskLocalVar(task, EL_BACK_NODE_ID);
        String backAssigneePolicy = getTaskLocalVar(task, EL_BACK_ASSIGNEE_POLICY);
        String multiInstanceBackPolicy = getTaskLocalVar(task, EL_MULTI_INSTANCE_BACK_POLICY);

        if (backType == null || backType.isBlank()) {
            throw new IllegalStateException("该节点未配置驳回方式, taskId: " + param.getTaskId());
        }

        // 3. 解析目标节点
        String targetNodeId = resolveTargetNodeId(task, backType, backNodeId, param.getTargetNodeId());
        String targetNodeName = resolveTargetNodeName(instance.getProcessInstanceId(), targetNodeId);

        // 4. 多实例协调
        boolean isMultiInstance = task.getProcessInstanceId() != null && isMultiInstance(task);
        String effectivePolicy = resolveMultiInstancePolicy(multiInstanceBackPolicy, isMultiInstance);

        if (isMultiInstance && "all_back".equals(effectivePolicy)) {
            backMultiInstanceAllBack(instance, task, targetNodeId, currentUserId, param.getRemark(),
                    backAssigneePolicy, targetNodeName);
        } else if (isMultiInstance && "independent".equals(effectivePolicy)) {
            throw new UnsupportedOperationException("independent 回退策略暂不支持");
        } else {
            // 单实例直接回退
            backSingleInstance(instance, task, targetNodeId, currentUserId, param.getRemark(),
                    backAssigneePolicy, targetNodeName);
        }
    }

    @Override
    public List<BackTargetNode> getAvailableBackTargets(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        List<HistoricActivityInstance> userTasks = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .orderByHistoricActivityInstanceEndTime().desc()
                .list();

        Map<String, BackTargetNode> targets = new LinkedHashMap<>();
        for (HistoricActivityInstance h : userTasks) {
            String nodeId = h.getActivityId();
            if (nodeId.equals(task.getTaskDefinitionKey())) continue;

            targets.putIfAbsent(nodeId, new BackTargetNode()
                    .setNodeId(nodeId)
                    .setNodeName(h.getActivityName()));
        }
        return new ArrayList<>(targets.values());
    }

    @Override
    public BackConfig getBackConfig(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        String actionButtons = getTaskLocalVar(task, EL_ACTION_BUTTONS);
        String backType = getTaskLocalVar(task, EL_BACK_TYPE);
        String backNodeId = getTaskLocalVar(task, EL_BACK_NODE_ID);
        String backAssigneePolicy = getTaskLocalVar(task, EL_BACK_ASSIGNEE_POLICY);
        String multiInstanceBackPolicy = getTaskLocalVar(task, EL_MULTI_INSTANCE_BACK_POLICY);

        BackConfig config = new BackConfig();
        config.setAllowBack(backType != null && !backType.isBlank());
        config.setBackType(backType);
        config.setBackNodeId(backNodeId);
        config.setBackAssigneePolicy(backAssigneePolicy != null ? backAssigneePolicy : "auto");
        config.setMultiInstanceBackPolicy(multiInstanceBackPolicy != null ? multiInstanceBackPolicy : "auto");
        config.setActionButtons(actionButtons != null && !actionButtons.isBlank()
                ? Arrays.asList(actionButtons.split(",")) : List.of());
        return config;
    }

    // ==================== Private helper methods ====================

    private ProcessInstance validateAndGetInstance(Integer processInstanceId) {
        ProcessInstance instance = processInstanceMapper.selectById(processInstanceId);
        if (instance == null) {
            throw new IllegalArgumentException("流程实例不存在: " + processInstanceId);
        }
        if (!"1".equals(instance.getProcessStatus())) {
            throw new IllegalStateException("流程实例非审批中状态, 无法驳回: " + processInstanceId);
        }
        return instance;
    }

    private Task validateAndGetTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return task;
    }

    private String getCurrentUserId() {
        return Optional.ofNullable(SecurityUtils.getLoginUser())
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前未登录, 无法驳回"));
    }

    private String getTaskLocalVar(Task task, String varName) {
        Object value = taskService.getVariableLocal(task.getId(), varName);
        return value != null ? value.toString() : null;
    }

    private boolean isMultiInstance(Task task) {
        BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        if (model == null) return false;
        if (!(model.getFlowElement(task.getTaskDefinitionKey()) instanceof UserTask ut)) return false;
        return ut.getLoopCharacteristics() != null;
    }

    private String resolveMultiInstancePolicy(String policy, boolean isMultiInstance) {
        if (!isMultiInstance) return "single";
        if (policy == null || policy.isBlank() || "auto".equals(policy)) {
            return "all_back";
        }
        return policy;
    }

    private String resolveTargetNodeId(Task task, String backType, String backNodeId, String paramTargetNodeId) {
        return switch (backType) {
            case "prev" -> resolvePrevNodeId(task);
            case "specific" -> {
                if (backNodeId == null || backNodeId.isBlank()) {
                    throw new IllegalStateException("该节点未配置固定驳回目标");
                }
                yield backNodeId;
            }
            case "choose" -> {
                if (paramTargetNodeId == null || paramTargetNodeId.isBlank()) {
                    throw new IllegalArgumentException("请选择驳回目标节点");
                }
                validateTargetNode(task.getProcessInstanceId(), paramTargetNodeId);
                yield paramTargetNodeId;
            }
            default -> throw new IllegalStateException("不支持的驳回方式: " + backType);
        };
    }

    private String resolvePrevNodeId(Task task) {
        HistoricActivityInstance last = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .finishedBefore(task.getCreateTime())
                .orderByHistoricActivityInstanceEndTime().desc()
                .list().stream()
                .filter(h -> !h.getActivityId().equals(task.getTaskDefinitionKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("没有可驳回的上级节点"));
        return last.getActivityId();
    }

    private void validateTargetNode(String processInstanceId, String targetNodeId) {
        long count = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityId(targetNodeId)
                .activityType("userTask")
                .count();
        if (count == 0) {
            throw new IllegalArgumentException("无效的回退目标节点: " + targetNodeId);
        }
    }

    private String resolveTargetNodeName(String processInstanceId, String targetNodeId) {
        BpmnModel model = repositoryService.getBpmnModel(
                runtimeService.createProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult()
                        .getProcessDefinitionId());
        if (model != null && model.getFlowElement(targetNodeId) instanceof UserTask ut) {
            return ut.getName();
        }
        return targetNodeId;
    }

    private void backSingleInstance(ProcessInstance instance, Task task, String targetNodeId,
                                    String currentUserId, String remark, String backAssigneePolicy,
                                    String targetNodeName) {
        // 校验目标节点是否已有进行中的任务
        long activeCount = taskService.createTaskQuery()
                .processInstanceId(task.getProcessInstanceId())
                .taskDefinitionKey(targetNodeId)
                .count();
        if (activeCount > 0) {
            throw new IllegalStateException("目标节点已有进行中的任务，无法驳回");
        }

        // Flowable 改状态
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveExecutionToActivityId(task.getExecutionId(), targetNodeId)
                .changeState();

        // 新任务分派
        Task newTask = taskService.createTaskQuery()
                .processInstanceId(task.getProcessInstanceId())
                .taskDefinitionKey(targetNodeId)
                .singleResult();

        if (newTask != null) {
            String assignee = resolveAssignee(newTask, backAssigneePolicy, instance);
            if (assignee != null) {
                taskService.setAssignee(newTask.getId(), assignee);
            }
        }

        // 记录轨迹
        saveBackHandleInfo(instance, task, currentUserId, remark, targetNodeId, targetNodeName);
    }

    private void backMultiInstanceAllBack(ProcessInstance instance, Task task, String targetNodeId,
                                          String currentUserId, String remark, String backAssigneePolicy,
                                          String targetNodeName) {
        // 校验目标节点
        long activeCount = taskService.createTaskQuery()
                .processInstanceId(task.getProcessInstanceId())
                .taskDefinitionKey(targetNodeId)
                .count();
        if (activeCount > 0) {
            throw new IllegalStateException("目标节点已有进行中的任务，无法驳回");
        }

        // 找到多实例父执行
        Execution childExecution = runtimeService.createExecutionQuery()
                .executionId(task.getExecutionId())
                .singleResult();
        if (childExecution == null || childExecution.getParentId() == null) {
            throw new IllegalStateException("无法定位多实例父执行");
        }
        Execution miBody = runtimeService.createExecutionQuery()
                .executionId(childExecution.getParentId())
                .singleResult();
        if (miBody == null) {
            throw new IllegalStateException("无法定位多实例父执行");
        }

        // 用父执行改状态
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveExecutionToActivityId(miBody.getId(), targetNodeId)
                .changeState();

        // 新任务分派
        Task newTask = taskService.createTaskQuery()
                .processInstanceId(task.getProcessInstanceId())
                .taskDefinitionKey(targetNodeId)
                .singleResult();

        if (newTask != null) {
            String assignee = resolveAssignee(newTask, backAssigneePolicy, instance);
            if (assignee != null) {
                taskService.setAssignee(newTask.getId(), assignee);
            }
        }

        // 记录轨迹
        saveBackHandleInfo(instance, task, currentUserId, remark, targetNodeId, targetNodeName);
    }

    private String resolveAssignee(Task newTask, String policy, ProcessInstance instance) {
        String effectivePolicy = (policy == null || policy.isBlank()) ? "auto" : policy;

        return switch (effectivePolicy) {
            case "last_handler" -> findLastHandler(instance.getId(), newTask.getTaskDefinitionKey());
            case "reassign" -> resolveByCandidateConfig(newTask);
            case "auto" -> {
                String last = findLastHandler(instance.getId(), newTask.getTaskDefinitionKey());
                yield last != null ? last : resolveByCandidateConfig(newTask);
            }
            default -> {
                String last = findLastHandler(instance.getId(), newTask.getTaskDefinitionKey());
                yield last != null ? last : resolveByCandidateConfig(newTask);
            }
        };
    }

    private String findLastHandler(Integer processInstanceId, String taskDefinitionKey) {
        List<ProcessHandleInfo> list = processHandleInfoMapper.selectDetailListByProcessInstanceId(processInstanceId);
        return list.stream()
                .filter(h -> taskDefinitionKey.equals(h.getTaskDefinitionKey()))
                .max(Comparator.comparing(ProcessHandleInfo::getHandleTime))
                .map(ProcessHandleInfo::getHandleUser)
                .orElse(null);
    }

    private String resolveByCandidateConfig(Task task) {
        BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        if (model == null) return null;
        if (!(model.getFlowElement(task.getTaskDefinitionKey()) instanceof UserTask ut)) return null;

        ApprovalContext ctx = ApprovalContext.from(ut);
        if (ctx == null) return null;

        List<String> assignees = candidateResolver.resolve(ctx);
        if (!ObjectUtils.isEmpty(assignees)) {
            return assignees.get(0);
        }
        return null;
    }

    private void saveBackHandleInfo(ProcessInstance instance, Task task, String currentUserId,
                                    String remark, String targetNodeId, String targetNodeName) {
        Integer maxRound = processHandleInfoMapper.selectMaxRound(instance.getId(), targetNodeId);
        int newRound = (maxRound == null) ? 1 : maxRound + 1;

        String extraJson = String.format("{\"targetNodeId\":\"%s\",\"targetNodeName\":\"%s\"}",
                targetNodeId, targetNodeName != null ? targetNodeName : "");

        ProcessHandleInfo info = new ProcessHandleInfo()
                .setProcessInstanceId(instance.getId())
                .setTaskId(task.getId())
                .setTaskName(task.getName())
                .setHandleUser(currentUserId)
                .setHandleType("back")
                .setRemark(remark != null && !remark.isBlank() ? remark : "驳回")
                .setHandleTime(LocalDateTime.now())
                .setTaskDefinitionKey(task.getTaskDefinitionKey())
                .setRound(newRound)
                .setExtra(extraJson);

        processHandleInfoMapper.insert(info);
    }
}
