package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import com.cat.common.entity.Page;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.process.*;


import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.candidate.CandidateResolver;
import com.cat.simple.config.flowable.enums.BackAssigneePolicyEnum;
import com.cat.simple.config.flowable.enums.BackTypeEnum;
import com.cat.simple.config.flowable.enums.HandleTypeEnum;
import com.cat.simple.config.flowable.enums.MultiInstanceBackPolicyEnum;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.process.ProcessCodeGenerator;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.ProcessDefinitionMapper;
import com.cat.simple.mapper.ProcessHandleInfoMapper;
import com.cat.simple.mapper.ProcessInstanceMapper;
import com.cat.simple.service.ProcessInstanceService;
import jakarta.annotation.Resource;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.*;

import static com.cat.simple.config.flowable.constant.ProcessConstants.*;
import static com.cat.simple.config.flowable.enums.ProcessStatusEnum.*;

@Service
public class ProcessInstanceServiceImpl implements ProcessInstanceService {


    @Resource
    private ProcessInstanceMapper processInstanceMapper;
    @Resource
    private ProcessDefinitionMapper processDefinitionMapper;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private HistoryService historyService;

    @Resource
    private ProcessHandleInfoMapper processHandleInfoMapper;

    @Resource
    private ProcessCodeGenerator codeGenerator;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private CandidateResolver candidateResolver;

    @Override
    public void updateStatus(String flowableProcessInstanceId,  ProcessStatusEnum processStatusEnum) {

        ProcessInstance processInstance = selectOneByFlowableProcessInstanceId(flowableProcessInstanceId);

        if(!Objects.isNull(processInstance)){
            updateProcessInstanceInfo(processInstance.getId(), flowableProcessInstanceId, LocalDateTime.now(), processStatusEnum);
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstance start(Integer processDefinitionId, String title) {
        ProcessDefinition definition = Optional.ofNullable(processDefinitionMapper.selectById(processDefinitionId))
                .orElseThrow(() -> new IllegalArgumentException("流程定义不存在: " + processDefinitionId));

        if (!"1".equals(definition.getStatus())) {
            throw new IllegalStateException("流程定义未发布, 无法发起: " + processDefinitionId);
        }

        String currentUserId = Optional.ofNullable(SecurityUtils.getLoginUser())
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前未登录, 无法发起流程"));

        org.flowable.engine.runtime.ProcessInstance flowableInstance =
                runtimeService.startProcessInstanceByKey(definition.getProcessKey());

        LocalDateTime now = LocalDateTime.now();
        ProcessInstance instance = new ProcessInstance()
                .setProcessDefinitionId(definition.getId())
                .setTitle(title)
                .setCode(codeGenerator.generate())
                .setProcessInstanceId(flowableInstance.getProcessInstanceId())
                .setProcessStatus(ACTIVE.getStatus())
                .setCreateBy(currentUserId)
                .setCreateTime(now)
                .setUpdateTime(now);
        processInstanceMapper.insert(instance);

        // 兜底：如果流程在启动期间就已经结束（如开始->结束的 trivial 流程），同步更新状态
        if (runtimeService.createProcessInstanceQuery()
                .processInstanceId(flowableInstance.getProcessInstanceId())
                .singleResult() == null) {
            updateProcessInstanceInfo(instance.getId(), flowableInstance.getProcessInstanceId(), now, COMPLETED);
        }

        saveHandleInfo(new ProcessHandleInfo()
                .setProcessInstanceId(instance.getId())
                .setHandleUser(currentUserId)
                .setHandleType(HandleTypeEnum.APPLY.getCode())
                .setRemark(HandleTypeEnum.APPLY.getName())
                .setRound(1));

        return instance;
    }


    @Override
    public Page<ProcessInstance> queryPage(ProcessInstancePageParam pageParam) {
        pageParam.init();

        String currentUserId = Optional.ofNullable(SecurityUtils.getLoginUser())
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前未登录, 无法查询流程实例"));
        pageParam.setUserId(currentUserId);

        return processInstanceMapper.selectPage(new Page<>(pageParam), pageParam);
    }


    @Override
    public ProcessInstance info(Integer id) {
        ProcessInstance processInstance = processInstanceMapper.selectInfoById(id);
        // 填充处理信息
        List<ProcessHandleInfo> processHandleInfoList = processHandleInfoMapper.selectDetailListByProcessInstanceId(processInstance.getId());
        processInstance.setProcessHandleInfoList(processHandleInfoList);
        return processInstance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claim(ProcessHandleParam param) {
        ProcessInstance instance = Optional.ofNullable(processInstanceMapper.selectById(param.getProcessInstanceId()))
                .orElseThrow(() -> new IllegalArgumentException("流程实例不存在: " + param.getProcessInstanceId()));

        if (!ACTIVE.getStatus().equals(instance.getProcessStatus())) {
            throw new IllegalStateException("流程实例非审批中状态, 无法认领: " + param.getProcessInstanceId());
        }

        String currentUserId = Optional.ofNullable(SecurityUtils.getLoginUser())
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前未登录, 无法认领任务"));

        Task task = taskService.createTaskQuery()
                .taskId(param.getTaskId())
                .taskCandidateUser(currentUserId)
                .singleResult();
        if (Objects.isNull(task)) {
            throw new IllegalStateException("当前用户没有该任务的认领权限, taskId: " + param.getTaskId());
        }

        taskService.claim(param.getTaskId(), currentUserId);

        String remark = param.getRemark() != null && !param.getRemark().isBlank()
                ? param.getRemark() : HandleTypeEnum.CLAIM.getName();

        Integer round = processHandleInfoMapper.selectMaxRound(instance.getId(), task.getTaskDefinitionKey());
        saveHandleInfo(new ProcessHandleInfo()
                .setProcessInstanceId(instance.getId())
                .setTaskId(param.getTaskId())
                .setTaskName(task.getName())
                .setHandleUser(currentUserId)
                .setHandleType(HandleTypeEnum.CLAIM.getCode())
                .setRemark(remark)
                .setTaskDefinitionKey(task.getTaskDefinitionKey())
                .setRound(round != null ? round : 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pass(ProcessHandleParam param) {
        ProcessInstance instance = Optional.ofNullable(processInstanceMapper.selectById(param.getProcessInstanceId()))
                .orElseThrow(() -> new IllegalArgumentException("流程实例不存在: " + param.getProcessInstanceId()));

        if (!ACTIVE.getStatus().equals(instance.getProcessStatus())) {
            throw new IllegalStateException("流程实例非审批中状态, 无法审批通过: " + param.getProcessInstanceId());
        }

        String currentUserId = Optional.ofNullable(SecurityUtils.getLoginUser())
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前未登录, 无法审批通过"));

        Task task = taskService.createTaskQuery()
                .taskId(param.getTaskId())
                .taskAssignee(currentUserId)
                .singleResult();
        if (Objects.isNull(task)) {
            throw new IllegalStateException("当前用户没有该任务的办理权限, taskId: " + param.getTaskId());
        }

        String remark = param.getRemark() != null && !param.getRemark().isBlank()
                ? param.getRemark() : HandleTypeEnum.PASS.getName();

        taskService.complete(task.getId());

        Integer round = processHandleInfoMapper.selectMaxRound(instance.getId(), task.getTaskDefinitionKey());
        saveHandleInfo(new ProcessHandleInfo()
                .setProcessInstanceId(instance.getId())
                .setTaskId(param.getTaskId())
                .setTaskName(task.getName())
                .setHandleUser(currentUserId)
                .setHandleType(HandleTypeEnum.PASS.getCode())
                .setRemark(remark)
                .setTaskDefinitionKey(task.getTaskDefinitionKey())
                .setRound(round != null ? round : 1));

        updateProcessInstanceInfo(instance.getId(), instance.getProcessInstanceId(), LocalDateTime.now(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(ProcessHandleParam param) {
        ProcessInstance instance = Optional.ofNullable(processInstanceMapper.selectById(param.getProcessInstanceId()))
                .orElseThrow(() -> new IllegalArgumentException("流程实例不存在: " + param.getProcessInstanceId()));

        if (!ACTIVE.getStatus().equals(instance.getProcessStatus())) {
            throw new IllegalStateException("流程实例非审批中状态, 无法审批拒绝: " + param.getProcessInstanceId());
        }

        String currentUserId = Optional.ofNullable(SecurityUtils.getLoginUser())
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前未登录, 无法审批拒绝"));

        Task task = taskService.createTaskQuery()
                .taskId(param.getTaskId())
                .taskAssignee(currentUserId)
                .singleResult();
        if (Objects.isNull(task)) {
            throw new IllegalStateException("当前用户没有该任务的办理权限, taskId: " + param.getTaskId());
        }

        String remark = param.getRemark() != null && !param.getRemark().isBlank()
                ? param.getRemark() : HandleTypeEnum.REJECT.getName();

        runtimeService.deleteProcessInstance(instance.getProcessInstanceId(), remark);

        Integer round = processHandleInfoMapper.selectMaxRound(instance.getId(), task.getTaskDefinitionKey());
        saveHandleInfo(new ProcessHandleInfo()
                .setProcessInstanceId(instance.getId())
                .setTaskId(param.getTaskId())
                .setTaskName(task.getName())
                .setHandleUser(currentUserId)
                .setHandleType(HandleTypeEnum.REJECT.getCode())
                .setRemark(remark)
                .setTaskDefinitionKey(task.getTaskDefinitionKey())
                .setRound(round != null ? round : 1));

        updateProcessInstanceInfo(instance.getId(), instance.getProcessInstanceId(), LocalDateTime.now(), TERMINATED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstance saveDraft(Integer id, Integer processDefinitionId, String title) {
        ProcessDefinition definition = Optional.ofNullable(processDefinitionMapper.selectById(processDefinitionId))
                .orElseThrow(() -> new IllegalArgumentException("流程定义不存在: " + processDefinitionId));

        String currentUserId = Optional.ofNullable(SecurityUtils.getLoginUser())
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前未登录, 无法保存草稿"));

        LocalDateTime now = LocalDateTime.now();

        if (!Objects.isNull(id)) {
            ProcessInstance exist = Optional.ofNullable(processInstanceMapper.selectById(id))
                    .orElseThrow(() -> new IllegalArgumentException("草稿不存在: " + id));
            if (!DRAFT.getStatus().equals(exist.getProcessStatus())) {
                throw new IllegalStateException("该流程实例不是草稿状态, 无法更新: " + id);
            }
            if (!currentUserId.equals(exist.getCreateBy())) {
                throw new IllegalStateException("无权更新他人草稿: " + id);
            }
            processInstanceMapper.update(new LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, id)
                    .set(ProcessInstance::getProcessDefinitionId, definition.getId())
                    .set(ProcessInstance::getTitle, title)
                    .set(ProcessInstance::getUpdateTime, now)
            );
            return exist.setProcessDefinitionId(definition.getId()).setTitle(title).setUpdateTime(now);
        }

        ProcessInstance instance = new ProcessInstance()
                .setProcessDefinitionId(definition.getId())
                .setTitle(title)
                .setProcessStatus(DRAFT.getStatus())
                .setCreateBy(currentUserId)
                .setCreateTime(now)
                .setUpdateTime(now);
        processInstanceMapper.insert(instance);
        return instance;
    }

    private void updateProcessInstanceInfo(Integer processInstanceId, String flowableProcessInstanceId, LocalDateTime updateTime, ProcessStatusEnum processStatusEnum){
        processStatusEnum = !Objects.isNull(processStatusEnum) ? processStatusEnum : getDetailedProcessStatus(flowableProcessInstanceId);

        if(!processStatusEnum.equals(UNKNOWN)){
            processInstanceMapper.update(new LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, processInstanceId)
                    .set(ProcessInstance::getProcessStatus, processStatusEnum.getStatus())
                    .set(ProcessInstance::getUpdateTime, updateTime)
            );
        }
    }

    private ProcessStatusEnum getDetailedProcessStatus(String processInstanceId) {
        org.flowable.engine.runtime.ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance != null) {
            if (processInstance.isSuspended()) {
                return SUSPENDED; // 已挂起
            } else {
                return ACTIVE; // 活动中
            }
        } else {
            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (historicInstance != null) {
                if (historicInstance.getEndTime() != null) {
                    return COMPLETED; // 已完成
                } else {
                    return TERMINATED; // 已终止
                }
            }
        }

        return UNKNOWN; // 未找到
    }


    private ProcessInstance selectOneByFlowableProcessInstanceId(String flowableProcessInstanceId) {
        return processInstanceMapper.selectOne(new LambdaQueryWrapper<ProcessInstance>().eq(ProcessInstance::getProcessInstanceId, flowableProcessInstanceId));
    }

    private void saveHandleInfo(ProcessHandleInfo info) {
        if (info.getHandleTime() == null) {
            info.setHandleTime(LocalDateTime.now());
        }
        if (info.getRound() == null) {
            info.setRound(1);
        }
        processHandleInfoMapper.insert(info);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void back(ProcessHandleParam param) {
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

        if (isMultiInstance && MultiInstanceBackPolicyEnum.ALL_BACK.getCode().equals(effectivePolicy)) {
            backMultiInstanceAllBack(instance, task, targetNodeId, currentUserId, param.getRemark(),
                    backAssigneePolicy, targetNodeName);
        } else if (isMultiInstance && MultiInstanceBackPolicyEnum.INDEPENDENT.getCode().equals(effectivePolicy)) {
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
        config.setBackAssigneePolicy(backAssigneePolicy != null ? backAssigneePolicy : BackAssigneePolicyEnum.AUTO.getCode());
        config.setMultiInstanceBackPolicy(multiInstanceBackPolicy != null ? multiInstanceBackPolicy : MultiInstanceBackPolicyEnum.AUTO.getCode());
        config.setActionButtons(actionButtons != null && !actionButtons.isBlank()
                ? Arrays.asList(actionButtons.split(",")) : List.of());
        return config;
    }

    // ==================== Back helper methods ====================

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
        if (policy == null || policy.isBlank() || MultiInstanceBackPolicyEnum.AUTO.getCode().equals(policy)) {
            return MultiInstanceBackPolicyEnum.ALL_BACK.getCode();
        }
        return policy;
    }

    private String resolveTargetNodeId(Task task, String backType, String backNodeId, String paramTargetNodeId) {
        BackTypeEnum type = BackTypeEnum.of(backType);
        if (type == null) {
            throw new IllegalStateException("不支持的驳回方式: " + backType);
        }
        return switch (type) {
            case PREV -> resolvePrevNodeId(task);
            case SPECIFIC -> {
                if (backNodeId == null || backNodeId.isBlank()) {
                    throw new IllegalStateException("该节点未配置固定驳回目标");
                }
                yield backNodeId;
            }
            case CHOOSE -> {
                if (paramTargetNodeId == null || paramTargetNodeId.isBlank()) {
                    throw new IllegalArgumentException("请选择驳回目标节点");
                }
                validateTargetNode(task.getProcessInstanceId(), paramTargetNodeId);
                yield paramTargetNodeId;
            }
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
        List<Task> newTasks = taskService.createTaskQuery()
                .processInstanceId(task.getProcessInstanceId())
                .taskDefinitionKey(targetNodeId)
                .list();

        if (!CollectionUtils.isEmpty(newTasks)) {
            for (Task newTask : newTasks) {
                String assignee = resolveAssignee(newTask, backAssigneePolicy, instance);
                if (assignee != null) {
                    taskService.setAssignee(newTask.getId(), assignee);
                }
            }
        }

        // 记录轨迹
        saveBackHandleInfo(instance, task, currentUserId, remark, targetNodeId, targetNodeName);
    }

    private String resolveAssignee(Task newTask, String policy, ProcessInstance instance) {
        BackAssigneePolicyEnum p = BackAssigneePolicyEnum.of(policy);
        if (p == null) {
            p = BackAssigneePolicyEnum.AUTO;
        }

        return switch (p) {
            case LAST_HANDLER -> findLastHandler(instance.getId(), newTask.getTaskDefinitionKey());
            case REASSIGN -> resolveByCandidateConfig(newTask);
            case AUTO -> {
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

        saveHandleInfo(new ProcessHandleInfo()
                .setProcessInstanceId(instance.getId())
                .setTaskId(task.getId())
                .setTaskName(task.getName())
                .setHandleUser(currentUserId)
                .setHandleType(HandleTypeEnum.BACK.getCode())
                .setRemark(remark != null && !remark.isBlank() ? remark : "驳回")
                .setTaskDefinitionKey(targetNodeId)
                .setRound(newRound)
                .setExtra(extraJson));
    }

}