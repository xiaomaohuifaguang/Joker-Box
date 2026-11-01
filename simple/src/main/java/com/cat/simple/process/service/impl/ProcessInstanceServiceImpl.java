package com.cat.simple.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.auth.User;
import com.cat.common.entity.process.*;
import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.command.*;
import com.cat.simple.config.flowable.core.CatTaskInfo;
import com.cat.simple.config.flowable.enums.BackTypeEnum;
import com.cat.simple.config.flowable.enums.HandleTypeEnum;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.config.flowable.util.FlowableUtils;
import com.cat.simple.process.mapper.ProcessDefinitionMapper;
import com.cat.simple.process.mapper.ProcessInstanceMapper;
import com.cat.simple.process.mapper.ProcessHandleInfoMapper;
import com.cat.simple.process.service.ProcessInstanceService;
import com.cat.simple.process.service.ProcessFormService;
import com.cat.simple.system.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProcessInstanceServiceImpl implements ProcessInstanceService {

    @Resource private CommandBus commandBus;
    @Resource private ProcessGuard guard;
    @Resource private ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessHandleInfoMapper processHandleInfoMapper;
    @Resource private FlowableUtils flowableUtils;
    @Resource private TaskService taskService;
    @Resource private ProcessFormService processFormService;
    @Resource private UserMapper userMapper;
    @Resource private ProcessDefinitionMapper processDefinitionMapper;

    @Override
    public ProcessInstance start(ProcessHandleParam param) {
        return commandBus.execute(new StartProcessCommand(param));
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

        ProcessDefinition processDefinition = processDefinitionMapper.selectById(instance.getProcessDefinitionId());

        if (StringUtils.hasText(taskId)) {
            Task task = guard.assertTaskExists(taskId);
            if (task.getProcessInstanceId().equals(instance.getProcessInstanceId())) {
                String userId = guard.getCurrentUserId();
                boolean isAssignee = userId.equals(task.getAssignee());
                boolean isCandidate = taskService.createTaskQuery()
                        .taskId(taskId)
                        .taskCandidateUser(userId)
                        .singleResult() != null;
                boolean editable = isAssignee || isCandidate;

                if (editable) {
                    instance.setTaskId(taskId);
                    instance.setTaskName(task.getName());
                    ApprovalContext approvalContext = flowableUtils.getApprovalContext(taskId);
                    instance.setButtonActions(approvalContext.actionButtons());
                    if(approvalContext.actionButtons().contains(HandleTypeEnum.BACK.getCode())){
                        BackConfig backConfig = flowableUtils.getBackConfig(approvalContext);
                        if(backConfig.isAllowBack() && backConfig.getBackType().equals(BackTypeEnum.CHOOSE.getCode())){
                            List<BackTargetNode> availableBackTargets = flowableUtils.getAvailableBackTargets(taskId);
                            backConfig.setAvailableBackTargets(availableBackTargets);
                        }
                        instance.setBackConfig(backConfig);
                    }
                }


                TaskFormVO taskFormVO = processFormService.buildTaskFormByNodeIdWithData(processDefinition.getId(), instance.getProcessDefinitionVersion(), instance.getId(), task.getTaskDefinitionKey());
                instance.setTaskForm(taskFormVO);

//                // 组装 taskForm
//                TaskFormVO taskForm = processFormService.buildTaskForm(
//                        id, instance.getProcessDefinitionId(),
//                        instance.getProcessDefinitionVersion(),
//                        task.getTaskDefinitionKey(), editable);
//                instance.setTaskForm(taskForm);
            }


            List<NextUserTaskInfo> nextUserTaskInfosSkipGateway = flowableUtils.findNextUserTaskInfosSkipGateway(processDefinition.getProcessKey(), processDefinition.getVersion(),
                    task.getTaskDefinitionKey());
            instance.setNextUserTaskInfos(nextUserTaskInfosSkipGateway);

            ProcessTrack processTrack = makeProcessTrack(instance);
            processTrack.setCurrentNodeId(task.getTaskDefinitionKey());
            instance.setProcessTrack(processTrack);
            instance.setProcessTrack(processTrack);

        } else if (ProcessStatusEnum.DRAFT.getStatus().equals(instance.getProcessStatus())) {
            StartEvent startEvent = flowableUtils.getStartEvent(processDefinition.getProcessKey(), instance.getProcessDefinitionVersion());
            TaskFormVO taskFormVO = processFormService.buildTaskFormByNodeIdWithData(processDefinition.getId(), instance.getProcessDefinitionVersion(), instance.getId(), startEvent.getId());
            instance.setTaskForm(taskFormVO);
//            String startNodeId = processDefinitionService.resolveStartEventNodeId(
//                    instance.getProcessDefinitionId());
//            if (startNodeId != null) {
//                TaskFormVO draftForm = processFormService.buildTaskForm(
//                        id, instance.getProcessDefinitionId(),
//                        instance.getProcessDefinitionVersion(),
//                        startNodeId, true);
//                instance.setTaskForm(draftForm);
//            }

            List<NextUserTaskInfo> nextUserTaskInfos = flowableUtils.findStartNextUserTaskInfosSkipGateway(processDefinition.getProcessKey(), processDefinition.getVersion());
            instance.setNextUserTaskInfos(nextUserTaskInfos);

        }else {
//            StartEvent startEvent = flowableUtils.getStartEvent(processDefinition.getProcessKey(), instance.getProcessDefinitionVersion());
            TaskFormVO taskFormVO = processFormService.buildGlobalFormWithData(processDefinition.getId(), instance.getProcessDefinitionVersion(), instance.getId());
            instance.setTaskForm(taskFormVO);

            instance.setProcessTrack(makeProcessTrack(instance));
        }
        return instance;
    }


    private Integer resolveRoundForActiveTask(Integer processInstanceId, org.flowable.task.api.Task task) {
        String taskDefinitionKey = task.getTaskDefinitionKey();
        Integer max = processHandleInfoMapper.selectMaxRound(processInstanceId, taskDefinitionKey);
        if (max == null) {
            return 1;
        }
        java.time.LocalDateTime latest = processHandleInfoMapper.selectLatestHandleTime(
                processInstanceId, taskDefinitionKey, max);
        if (latest == null) {
            return max;
        }
        java.util.Date createTime = task.getCreateTime();
        if (createTime == null) {
            return max;
        }
        java.time.LocalDateTime taskCreate = createTime.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        return taskCreate.isAfter(latest) ? max + 1 : max;
    }

    @Override
    public void claim(ProcessHandleParam param) {
        commandBus.execute(new ClaimTaskCommand(param));
    }

    @Override
    public void pass(ProcessHandleParam param) {
        commandBus.execute(new PassTaskCommand(param));
    }

    @Override
    public void autoPass(String taskId) {
        commandBus.execute(new AutoPassTaskCommand(taskId));
    }

    @Override
    public void reject(ProcessHandleParam param) {
        commandBus.execute(new RejectTaskCommand(param));
    }

    @Override
    public void back(ProcessHandleParam param) {
         commandBus.execute(new BackTaskCommand(param));
    }

    @Override
    public ProcessInstance saveDraft(ProcessHandleParam param) {
        Integer id = param.getProcessInstanceId();
        Integer processDefinitionId = param.getProcessDefinitionId();
        String title = param.getTitle();

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


            processFormService.writeFormData(exist, param.getGlobalFormData());

            return exist.setProcessDefinitionId(definition.getId())
                    .setProcessDefinitionVersion(definition.getVersion())
                    .setTitle(title).setUpdateTime(now);
        }

        ProcessInstance instance = new ProcessInstance()
                .setProcessDefinitionId(definition.getId())
                .setProcessDefinitionVersion(definition.getVersion())
                .setTitle(title)
                .setProcessStatus(ProcessStatusEnum.DRAFT.getStatus())
                .setCreateBy(currentUserId)
                .setCreateTime(now)
                .setUpdateTime(now);
        processInstanceMapper.insert(instance);

        processFormService.writeFormData(instance, param.getGlobalFormData());

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
        return flowableUtils.getAvailableBackTargets(taskId);
    }

    @Override
    public BackConfig getBackConfig(String taskId) {
        return flowableUtils.getBackConfig(taskId);
    }


    private ProcessTrack makeProcessTrack(ProcessInstance instance){
        ProcessTrack processTrack = flowableUtils.getProcessTrack(instance.getProcessInstanceId());
        List<ProcessHandleInfo> handleList = processHandleInfoMapper.selectDetailListByProcessInstanceId(instance.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(instance.getProcessInstanceId()).list();

        for (Task task : tasks) {
            ProcessHandleInfo handleInfo = new ProcessHandleInfo();
            handleInfo.setTaskName(task.getName());
            handleInfo.setTaskDefinitionKey(task.getTaskDefinitionKey());
            CatTaskInfo taskInfo = guard.toTaskInfo(task);
            handleInfo.setRound(taskInfo.getRound());

            if(task.getAssignee() == null){
                handleInfo.setHandleType("to_claim");
            }else {
                handleInfo.setHandleUser(task.getAssignee());
                handleInfo.setHandleType("to_do");
                User user = userMapper.selectById(task.getAssignee());
                handleInfo.setHandleUserName(user.getNickname());
            }

            handleList.add(handleInfo);
        }

        processTrack.setProcessHandleInfoList(handleList);
        return processTrack;
    }


}
