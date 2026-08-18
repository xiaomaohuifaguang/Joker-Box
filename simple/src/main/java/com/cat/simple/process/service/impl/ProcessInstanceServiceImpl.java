package com.cat.simple.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.process.*;
import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.command.*;
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
import java.util.stream.Collectors;

@Service
public class ProcessInstanceServiceImpl implements ProcessInstanceService {

    @Resource private CommandBus commandBus;
    @Resource private ProcessGuard guard;
    @Resource private ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessHandleInfoMapper processHandleInfoMapper;
    @Resource private FlowableUtils flowableUtils;
    @Resource private TaskService taskService;
    @Resource private ProcessFormService processFormService;
    @Resource private com.cat.simple.process.service.ProcessDefinitionService processDefinitionService;
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
//        List<ProcessHandleInfo> handleList =
//                processHandleInfoMapper.selectDetailListByProcessInstanceId(instance.getId());
//        instance.setProcessHandleInfoList(handleList);
//        instance.setTimeline(buildTimeline(handleList, instance));

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
        }else {
            StartEvent startEvent = flowableUtils.getStartEvent(processDefinition.getProcessKey(), instance.getProcessDefinitionVersion());
            TaskFormVO taskFormVO = processFormService.buildTaskFormByNodeIdWithData(processDefinition.getId(), instance.getProcessDefinitionVersion(), instance.getId(), startEvent.getId());
            instance.setTaskForm(taskFormVO);
        }
        return instance;
    }

    private List<ProcessTimelineNode> buildTimeline(List<ProcessHandleInfo> list, ProcessInstance instance) {
        List<ProcessTimelineNode> timeline = new ArrayList<>();

        // 1. 已有记录：先排序，再分组构建节点，节点之间按 startTime 正序排
        if (list != null && !list.isEmpty()) {
            list.sort(Comparator.comparing(ProcessHandleInfo::getHandleTime,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            timeline = list.stream()
                    .collect(Collectors.groupingBy(h -> {
                        String nodeKey = h.getTaskDefinitionKey() != null ? h.getTaskDefinitionKey() : "_start";
                        int round = h.getRound() != null ? h.getRound() : 1;
                        return nodeKey + "#" + round;
                    }))
                    .values().stream()
                    .map(group -> {
                        group.sort(Comparator.comparing(ProcessHandleInfo::getHandleTime,
                                Comparator.nullsLast(Comparator.naturalOrder())));
                        ProcessHandleInfo first = group.get(0);
                        String nodeId = first.getTaskDefinitionKey() != null ? first.getTaskDefinitionKey() : "_start";
                        ProcessTimelineNode node = new ProcessTimelineNode();
                        node.setNodeId(nodeId);
                        node.setNodeName(first.getTaskName() != null ? first.getTaskName()
                                : ("_start".equals(nodeId) ? "申请" : nodeId));
                        node.setRound(first.getRound() != null ? first.getRound() : 1);
                        node.setHandlers(new ArrayList<>(group));
                        node.setStartTime(first.getHandleTime());
                        node.setEndTime(group.get(group.size() - 1).getHandleTime());
                        boolean hasEndAction = group.stream()
                                .anyMatch(h -> h.getHandleType() != null
                                        && Set.of("pass", "reject", "back", "apply").contains(h.getHandleType()));
                        node.setNodeStatus(hasEndAction ? "completed" : "active");
                        return node;
                    })
                    .collect(Collectors.toList());
            timeline.sort(Comparator.comparing(ProcessTimelineNode::getStartTime,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }

        // 2. 追加待办节点及待处理人：不参与整体排序，直接 append 到末尾
        if (instance != null && StringUtils.hasText(instance.getProcessInstanceId())) {
            List<org.flowable.task.api.Task> activeTasks = taskService.createTaskQuery()
                    .processInstanceId(instance.getProcessInstanceId())
                    .list();
            Map<String, List<org.flowable.task.api.Task>> pendingGroups = new HashMap<>();
            for (org.flowable.task.api.Task task : activeTasks) {
                String nodeId = task.getTaskDefinitionKey();
                if (nodeId == null) continue;
                Integer round = resolveRoundForActiveTask(instance.getId(), task);
                pendingGroups.computeIfAbsent(nodeId + "#" + round, k -> new ArrayList<>()).add(task);
            }
            for (Map.Entry<String, List<org.flowable.task.api.Task>> entry : pendingGroups.entrySet()) {
                String[] parts = entry.getKey().split("#", 2);
                String nodeId = parts[0];
                Integer round = Integer.parseInt(parts[1]);
                List<org.flowable.task.api.Task> tasks = entry.getValue();

                ProcessTimelineNode node = timeline.stream()
                        .filter(n -> nodeId.equals(n.getNodeId()) && round.equals(n.getRound()))
                        .findFirst()
                        .orElse(null);
                if (node == null) {
                    node = new ProcessTimelineNode();
                    node.setNodeId(nodeId);
                    node.setNodeName(tasks.get(0).getName() != null ? tasks.get(0).getName() : nodeId);
                    node.setRound(round);
                    node.setNodeStatus("active");
                    node.setHandlers(new ArrayList<>());
                    if (tasks.get(0).getCreateTime() != null) {
                        node.setStartTime(tasks.get(0).getCreateTime().toInstant()
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                    }
                    timeline.add(node);
                } else {
                    node.setNodeStatus("active");
                }
                List<String> existingTaskIds = node.getHandlers().stream()
                        .map(ProcessHandleInfo::getTaskId)
                        .filter(Objects::nonNull)
                        .toList();
                for (org.flowable.task.api.Task task : tasks) {
                    if (existingTaskIds.contains(task.getId())) continue;
                    ProcessHandleInfo pending = new ProcessHandleInfo();
                    pending.setTaskId(task.getId());
                    pending.setTaskName(task.getName());
                    pending.setTaskDefinitionKey(task.getTaskDefinitionKey());
                    pending.setHandleUser(task.getAssignee());
                    String nickname = null;
                    if (task.getAssignee() != null) {
                        com.cat.common.entity.auth.User user = userMapper.selectById(task.getAssignee());
                        if (user != null) nickname = user.getNickname();
                    }else {
                        nickname = "待认领";
                    }
                    pending.setHandleUserName(nickname);
                    pending.setRound(round);
                    node.getHandlers().add(pending);
                }
            }
        }

        return timeline;
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


}
