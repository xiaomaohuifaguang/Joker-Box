package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.process.*;
import com.cat.simple.config.flowable.back.BackConfigReader;
import com.cat.simple.config.flowable.back.BackTargetResolver;
import com.cat.simple.config.flowable.command.*;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.mapper.ProcessInstanceMapper;
import com.cat.simple.mapper.ProcessHandleInfoMapper;
import com.cat.simple.service.ProcessInstanceService;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProcessInstanceServiceImpl implements ProcessInstanceService {

    @Resource private CommandBus commandBus;
    @Resource private ProcessGuard guard;
    @Resource private ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessHandleInfoMapper processHandleInfoMapper;
    @Resource private BackConfigReader backConfigReader;
    @Resource private BackTargetResolver backTargetResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstance start(Integer processDefinitionId, String title) {
        return commandBus.execute(new StartProcessCommand(processDefinitionId, title));
    }

    @Override
    public Page<ProcessInstance> queryPage(ProcessInstancePageParam pageParam) {
        pageParam.init();
        pageParam.setUserId(guard.getCurrentUserId());
        return processInstanceMapper.selectPage(new Page<>(pageParam), pageParam);
    }

    @Override
    public ProcessInstance info(Integer id, String taskId) {
        ProcessInstance instance = processInstanceMapper.selectInfoById(id);
        if (instance == null) {
            return null;
        }
        instance.setProcessHandleInfoList(
                processHandleInfoMapper.selectDetailListByProcessInstanceId(instance.getId()));

        if (StringUtils.hasText(taskId)) {
            Task task = guard.assertTaskAssignee(taskId);
            instance.setTaskId(taskId);
            instance.setTaskName(task.getName());
        }
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claim(ProcessHandleParam param) {
        commandBus.execute(new ClaimTaskCommand(param));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pass(ProcessHandleParam param) {
        commandBus.execute(new PassTaskCommand(param));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(ProcessHandleParam param) {
        commandBus.execute(new RejectTaskCommand(param));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void back(ProcessHandleParam param) {
        commandBus.execute(new BackTaskCommand(param));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstance saveDraft(Integer id, Integer processDefinitionId, String title) {
        String currentUserId = guard.getCurrentUserId();
        com.cat.common.entity.process.ProcessDefinition definition =
                guard.assertDefinitionPublished(processDefinitionId);
        LocalDateTime now = LocalDateTime.now();

        if (id != null) {
            ProcessInstance exist = guard.assertInstanceDraft(id);
            if (!currentUserId.equals(exist.getCreateBy())) {
                throw new IllegalStateException("无权更新他人草稿: " + id);
            }
            processInstanceMapper.update(new LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, id)
                    .set(ProcessInstance::getProcessDefinitionId, definition.getId())
                    .set(ProcessInstance::getTitle, title)
                    .set(ProcessInstance::getUpdateTime, now));
            return exist.setProcessDefinitionId(definition.getId()).setTitle(title).setUpdateTime(now);
        }

        ProcessInstance instance = new ProcessInstance()
                .setProcessDefinitionId(definition.getId())
                .setTitle(title)
                .setProcessStatus(ProcessStatusEnum.DRAFT.getStatus())
                .setCreateBy(currentUserId)
                .setCreateTime(now)
                .setUpdateTime(now);
        processInstanceMapper.insert(instance);
        return instance;
    }

    @Override
    public void updateStatus(String flowableProcessInstanceId, ProcessStatusEnum status) {
        ProcessInstance instance = guard.selectByFlowableId(flowableProcessInstanceId);
        if (instance != null && status != ProcessStatusEnum.UNKNOWN) {
            processInstanceMapper.update(new LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, instance.getId())
                    .set(ProcessInstance::getProcessStatus, status.getStatus())
                    .set(ProcessInstance::getUpdateTime, LocalDateTime.now()));
        }
    }

    @Override
    public List<BackTargetNode> getAvailableBackTargets(String taskId) {
        return backConfigReader.getAvailableBackTargets(taskId);
    }

    @Override
    public BackConfig getBackConfig(String taskId) {
        return backConfigReader.getBackConfig(taskId);
    }
}
