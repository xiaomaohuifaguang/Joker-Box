package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import com.cat.common.entity.Page;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.process.*;

import com.cat.simple.config.flowable.back.BackConfigReader;
import com.cat.simple.config.flowable.back.BackEngine;
import com.cat.simple.config.flowable.back.BackTargetResolver;
import com.cat.simple.config.flowable.enums.HandleTypeEnum;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.flowable.util.BpmnModelUtil;
import com.cat.simple.config.process.ProcessCodeGenerator;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.ProcessDefinitionMapper;
import com.cat.simple.mapper.ProcessHandleInfoMapper;
import com.cat.simple.mapper.ProcessInstanceMapper;
import com.cat.simple.service.ProcessInstanceService;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

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
    private BackEngine backEngine;

    @Resource
    private BackTargetResolver backTargetResolver;

    @Resource
    private BackConfigReader backConfigReader;

    @Resource
    private BpmnModelUtil bpmnModelUtil;

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
    public ProcessInstance info(Integer id, String taskId) {
        Task task = null;
        if(StringUtils.hasText(taskId)){
            task = taskService.createTaskQuery().taskId(taskId).taskAssignee(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId()).singleResult();
            if(Objects.isNull(task)){
                throw new IllegalStateException("任务不存在/非本人处理任务");
            }
        }

        ProcessInstance processInstance = processInstanceMapper.selectInfoById(id);
        // 填充处理信息
        List<ProcessHandleInfo> processHandleInfoList = processHandleInfoMapper.selectDetailListByProcessInstanceId(processInstance.getId());
        if(!Objects.isNull(task)){
            processInstance.setTaskId(taskId);
            processInstance.setTaskName(task.getName());
        }
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
        BackConfig cfg = backConfigReader.getBackConfig(param.getTaskId());
        if (!cfg.isAllowBack()) {
            throw new IllegalStateException("该节点未配置驳回方式, taskId: " + param.getTaskId());
        }

        // 3. 解析目标节点
        String targetNodeId = backTargetResolver.resolveTargetNodeId(
                task, cfg.getBackType(), cfg.getBackNodeId(), param.getTargetNodeId());
        String targetNodeName = backTargetResolver.resolveTargetNodeName(
                instance.getProcessInstanceId(), targetNodeId);

        // 4. 多实例回退统一走 ALL_BACK
        boolean isMultiInstance = task.getProcessInstanceId() != null && bpmnModelUtil.isMultiInstance(task);

        if (isMultiInstance) {
            backEngine.backMultiInstanceAllBack(instance, task, targetNodeId, currentUserId, param.getRemark(),
                    cfg.getBackAssigneePolicy(), targetNodeName);
        } else {
            // 单实例直接回退
            backEngine.backSingleInstance(instance, task, targetNodeId, currentUserId, param.getRemark(),
                    cfg.getBackAssigneePolicy(), targetNodeName);
        }

        // 记录轨迹
        saveBackHandleInfo(instance, task, currentUserId, param.getRemark(), targetNodeId, targetNodeName);
    }

    @Override
    public List<BackTargetNode> getAvailableBackTargets(String taskId) {
        return backConfigReader.getAvailableBackTargets(taskId);
    }

    @Override
    public BackConfig getBackConfig(String taskId) {
        return backConfigReader.getBackConfig(taskId);
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