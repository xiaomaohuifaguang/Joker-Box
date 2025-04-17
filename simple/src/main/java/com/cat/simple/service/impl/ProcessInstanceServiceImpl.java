package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.auth.User;
import com.cat.common.entity.process.*;
import com.cat.common.entity.process.enums.HandleButtonEnum;

import com.cat.common.entity.process.enums.ProcessStatusEnum;
import com.cat.simple.config.process.core.enums.BpmUserTaskRejectHandlerTypeEnum;
import com.cat.simple.config.process.core.utils.FlowElementUtils;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.ProcessDefinitionMapper;
import com.cat.simple.mapper.ProcessHandleInfoMapper;
import com.cat.simple.mapper.ProcessInstanceMapper;
import com.cat.simple.mapper.UserMapper;
import com.cat.simple.service.ProcessInstanceService;
import jakarta.annotation.Resource;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ProcessInstanceServiceImpl implements ProcessInstanceService {


    @Resource
    private ProcessInstanceMapper processInstanceMapper;
    @Resource
    private ProcessDefinitionMapper processDefinitionMapper;
    @Resource
    private UserMapper userMapper;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private HistoryService historyService;

    @Resource
    private ProcessHandleInfoMapper processHandleInfoMapper;


    @Override
    public Page<ProcessInstance> queryPage(ProcessInstancePageParam pageParam){

        pageParam.init();
        pageParam.setUserId(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());


        Page<ProcessInstance> page = new Page<>(pageParam);
        page = processInstanceMapper.selectPage(page,pageParam);

        page.getRecords().forEach(r->{
            r.setProcessDefinitionName(processDefinitionMapper.selectById(r.getProcessDefinitionId()).getProcessName());
            r.setCreateByName(userMapper.selectById(r.getCreateBy()).getNickname());
        });

        return page;
    }

    @Override
    public ProcessInfo info(Integer processInstanceId) {
        ProcessInstance processInstance = processInstanceMapper.selectById(processInstanceId);
        return new ProcessInfo().setProcessInstance(processInstance);
    }

    @Override
    public ProcessInfo handleInfo(Integer processInstanceId) {

        ProcessInfo processInfo = new ProcessInfo();

        ProcessInstance processInstance = processInstanceMapper.selectById(processInstanceId);

        processInfo.setProcessInstance(processInstance);

        List<String> buttonSettings = getButtonSettings(processInstance.getProcessInstanceId());
        processInfo.setHandleButton(buttonSettings);

        List<ProcessHandleInfo> processHandleInfos = processHandleInfoMapper.selectDetailListByProcessInstanceId(processInstanceId);
        processInfo.setHandleInfos(processHandleInfos);


        return processInfo;
    }




    @Override
    @Transactional
    public ProcessInstance start(Integer processDefinitionId) {


        ProcessDefinition processDefinition = processDefinitionMapper.selectById(processDefinitionId);
        if(Objects.isNull(processDefinition)){
            return null;
        }

        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        String userId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();
        Authentication.setAuthenticatedUserId(userId);
        org.flowable.engine.runtime.ProcessInstance start = processInstanceBuilder
                .processDefinitionKey(processDefinition.getProcessKey())
                .startAsync();

        Authentication.setAuthenticatedUserId(null);

        ProcessInstance processInstance = new ProcessInstance();

        processInstance.setProcessDefinitionId(processDefinitionId);
        processInstance.setProcessInstanceId(start.getId());
        processInstance.setProcessStatus(ProcessStatusEnum.ACTIVE.getStatus());
        processInstance.setCreateBy(userId);
        processInstance.setCreateTime(LocalDateTime.now());
        processInstance.setUpdateTime(processInstance.getCreateTime());


        processInstanceMapper.insert(processInstance);


        saveHandleInfo(processInstance.getId(), HandleButtonEnum.APPLY.getName(), HandleButtonEnum.APPLY.getName(), userId,HandleButtonEnum.APPLY.getCode(), HandleButtonEnum.APPLY.getName(), processInstance.getUpdateTime());


        return processInstance;
    }

    @Override
    @Transactional
    public boolean pass(Integer processInstanceId) {

        ProcessInstance processInstance = processInstanceMapper.selectById(processInstanceId);

        String currentUserId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getProcessInstanceId()).active()
                .taskAssignee(currentUserId).singleResult();

        List<String> buttonSettings = getButtonSettings(processInstance.getProcessInstanceId());
        if(!buttonSettings.contains(HandleButtonEnum.PASS.getCode())){
            return false;
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("handleType", HandleButtonEnum.PASS);

        taskService.complete(task.getId(), vars);
        LocalDateTime updateTime = LocalDateTime.now();
        updateProcessInstanceInfo(processInstanceId, processInstance.getProcessInstanceId(), updateTime, null);
        saveHandleInfo(processInstance.getId(), task.getId(), task.getName(),currentUserId, HandleButtonEnum.PASS.getCode(), HandleButtonEnum.PASS.getName(), updateTime);
        return true;
    }

    @Override
    public boolean transfer(Integer processInstanceId, Integer userId) {

        ProcessInstance processInstance = processInstanceMapper.selectById(processInstanceId);

        User user = userMapper.selectById(userId);

        String currentUserId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getProcessInstanceId()).active()
                .taskAssignee(currentUserId).singleResult();

        List<String> buttonSettings = getButtonSettings(processInstance.getProcessInstanceId());
        if(!buttonSettings.contains(HandleButtonEnum.TRANSFER.getCode())){
            return false;
        }

        // 转办任务
        taskService.setAssignee(task.getId(), String.valueOf(user.getId()));

        LocalDateTime updateTime = LocalDateTime.now();
        updateProcessInstanceInfo(processInstanceId, processInstance.getProcessInstanceId(), updateTime, null);
        saveHandleInfo(processInstance.getId(), task.getId(), task.getName(), currentUserId, HandleButtonEnum.TRANSFER.getCode(), HandleButtonEnum.TRANSFER.getName(), updateTime);
        return true;
    }

    @Override
    public boolean reject(Integer processInstanceId) {

        ProcessInstance processInstance = processInstanceMapper.selectById(processInstanceId);

        String currentUserId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();
        Task currentTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getProcessInstanceId()).active()
                .taskAssignee(currentUserId).singleResult();

        List<String> buttonSettings = getButtonSettings(processInstance.getProcessInstanceId());
        if(!buttonSettings.contains(HandleButtonEnum.REJECT.getCode())){
            return false;
        }


        FlowElement currentFlowElement = getCurrentFlowElement(currentTask);
        String rejectHandlerType = FlowElementUtils.getRejectHandlerType(currentFlowElement);
        if(!Objects.isNull(rejectHandlerType) && rejectHandlerType.equals(BpmUserTaskRejectHandlerTypeEnum.RETURN_USER_TASK.getType())){
            String rejectReturnTaskId = FlowElementUtils.getRejectReturnTaskId(currentFlowElement);

            BpmnModel bpmnModel = getBpmnModel(currentTask.getProcessDefinitionId());
            FlowElement source = FlowElementUtils.getFlowElementById(bpmnModel, currentTask.getTaskDefinitionKey());
            FlowElement target = FlowElementUtils.getFlowElementById(bpmnModel, rejectReturnTaskId);
            // 2.2 只有串行可到达的节点，才可以退回。类似非串行、子流程无法退回
            if (!FlowElementUtils.isSequentialReachable(source, target, null)) {
                return false;
            }
            List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(currentTask.getProcessInstanceId()).active().list();
            List<String> activityIds = activeTasks.stream().map(Task::getTaskDefinitionKey).toList();
            // 1.2 通过 targetElement 的出口连线，计算在 runTaskKeyList 有哪些 key 需要被撤回
            List<UserTask> returnUserTaskList = FlowElementUtils.iteratorFindChildUserTasks(target, activityIds, null, null);
            List<String> returnTaskKeyList = returnUserTaskList.stream().map(UserTask::getId).toList();


            // 2. 给当前要被退回的 task 数组，设置退回意见
            activeTasks.forEach(itemTask -> {
                // 需要排除掉，不需要设置退回意见的任务
                if (!returnTaskKeyList.contains(itemTask.getTaskDefinitionKey())) {
                    return;
                }

                // 判断是否分配给自己任务，因为会签任务，一个节点会有多个任务
                // if (userId.equals(itemTask.getAssignee())) { // 情况一：自己的任务，进行 RETURN 标记
                //
                // } else { // 情况二：别人的任务，进行 CANCEL 标记
                // }
            });

            List<String> runExecutionIds = activeTasks.stream().map(Task::getExecutionId).toList();

            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(currentTask.getProcessInstanceId())
                    .moveExecutionsToSingleActivityId(runExecutionIds, rejectReturnTaskId)
                    .changeState();

        }else {
            List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).active().list();
            BpmnModel bpmnModel = getBpmnModel(currentTask.getProcessDefinitionId());
            List<String> activityIds = activeTasks.stream().map(Task::getTaskDefinitionKey).toList();
            EndEvent endEvent = FlowElementUtils.getEndEvent(bpmnModel);
            Assert.notNull(endEvent, "结束节点不能未空");



            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .moveActivityIdsToSingleActivityId(activityIds, endEvent.getId())
                    .changeState();

        }

        LocalDateTime updateTime = LocalDateTime.now();
        updateProcessInstanceInfo(processInstanceId, processInstance.getProcessInstanceId(), updateTime, null);

        saveHandleInfo(processInstance.getId(), currentTask.getId(), currentTask.getName(), currentUserId, HandleButtonEnum.REJECT.getCode(), HandleButtonEnum.REJECT.getName(), updateTime);

        return true;
    }

    @Override
    public List<String> taskNames(Integer processInstanceId) {
        List<Task> list = taskService.createTaskQuery().processInstanceId(processInstanceMapper.selectById(processInstanceId).getProcessInstanceId()).list();
        return list.stream().map(Task::getName).toList();
    }

    @Override
    public void updateStatus(String flowableProcessInstanceId, String processStatus, LocalDateTime now) {

        ProcessInstance processInstance = selectOneByFlowableProcessInstanceId(flowableProcessInstanceId);

        if(!Objects.isNull(processInstance)){
            updateProcessInstanceInfo(processInstance.getId(), flowableProcessInstanceId, now, processStatus);
        }
    }

    private List<String> getButtonSettings(String processInstanceId){

        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId).active()
                .taskAssignee(Objects.requireNonNull(SecurityUtils.getLoginUser())
                        .getUserId()).singleResult();

        if(Objects.nonNull(task)) {

            FlowElement currentFlowElement = getCurrentFlowElement(task);

            return FlowElementUtils.getButtonsSettings(currentFlowElement);
        }

        return List.of();

    }


    public void saveHandleInfo(Integer processInstanceId,String taskId, String taskName,String handleUser , String handleType, String remark, LocalDateTime updateTime){
        ProcessHandleInfo processHandleInfo = new ProcessHandleInfo();
        processHandleInfo.setProcessInstanceId(processInstanceId);
        processHandleInfo.setTaskId(taskId);
        processHandleInfo.setTaskName(taskName);
        processHandleInfo.setHandleType(handleType);
        processHandleInfo.setRemark(remark);
        processHandleInfo.setHandleTime(updateTime);
        processHandleInfo.setHandleUser(handleUser);
        processHandleInfoMapper.insert(processHandleInfo);
    }

    @Override
    public void saveHandleInfo(String flowableProcessInstanceId, String taskId, String taskName, String handleUser, String handleType, String remark, LocalDateTime updateTime) {

        ProcessInstance processInstance = selectOneByFlowableProcessInstanceId(flowableProcessInstanceId);

        ProcessHandleInfo processHandleInfo = new ProcessHandleInfo();
        processHandleInfo.setProcessInstanceId(processInstance.getId());
        processHandleInfo.setTaskId(taskId);
        processHandleInfo.setTaskName(taskName);
        processHandleInfo.setHandleType(handleType);
        processHandleInfo.setRemark(remark);
        processHandleInfo.setHandleTime(updateTime);
        processHandleInfo.setHandleUser(handleUser);
        processHandleInfoMapper.insert(processHandleInfo);
    }

    @Override
    public ProcessInstance selectOneByFlowableProcessInstanceId(String flowableProcessInstanceId) {
        return processInstanceMapper.selectOne(new LambdaQueryWrapper<ProcessInstance>().eq(ProcessInstance::getProcessInstanceId, flowableProcessInstanceId));
    }


    private void updateProcessInstanceInfo(Integer processInstanceId, String flowableProcessInstanceId, LocalDateTime updateTime, String processStatus){
        String detailedProcessStatus = StringUtils.hasText(processStatus) ? processStatus : getDetailedProcessStatus(flowableProcessInstanceId);
        String status = switch (detailedProcessStatus) {
            case "ACTIVE" -> ProcessStatusEnum.ACTIVE.getStatus();
            case "COMPLETED" -> ProcessStatusEnum.COMPLETED.getStatus();
            case "SUSPENDED" -> ProcessStatusEnum.SUSPENDED.getStatus();
            case "TERMINATED" -> ProcessStatusEnum.TERMINATED.getStatus();
            default -> "";
        };
        if(StringUtils.hasText(status)){
            processInstanceMapper.update(new LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, processInstanceId)
                    .set(ProcessInstance::getProcessStatus, status)
                    .set(ProcessInstance::getUpdateTime, updateTime)
            );
        }
    }



    private String getDetailedProcessStatus(String processInstanceId) {
        org.flowable.engine.runtime.ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance != null) {
            if (processInstance.isSuspended()) {
                return "SUSPENDED"; // 已挂起
            } else {
                return "ACTIVE"; // 活动中
            }
        } else {
            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (historicInstance != null) {
                if (historicInstance.getEndTime() != null) {
                    return "COMPLETED"; // 已完成
                } else {
                    return "TERMINATED"; // 已终止
                }
            }
        }

        return "NOT_FOUND"; // 未找到
    }

    private FlowElement getCurrentFlowElement(Task task) {
        BpmnModel bpmnModel = getBpmnModel(task.getProcessDefinitionId());
        Process process = bpmnModel.getProcessById(task.getProcessDefinitionId().split(":")[0]);


        Execution execution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        String activityId = execution.getActivityId();
        return process.getFlowElement(activityId);
    }

    private BpmnModel getBpmnModel(String processDefinitionId) {
        // 4. 获取BPMN模型和当前流程元素
        return repositoryService.getBpmnModel(processDefinitionId);
    }

}