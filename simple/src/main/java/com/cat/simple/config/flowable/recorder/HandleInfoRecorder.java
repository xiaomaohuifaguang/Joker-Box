package com.cat.simple.config.flowable.recorder;

import com.cat.common.entity.process.ProcessHandleInfo;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.HandleTypeEnum;
import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.process.mapper.ProcessHandleInfoMapper;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class HandleInfoRecorder {

    @Resource private ProcessHandleInfoMapper processHandleInfoMapper;
    @Resource private ProcessGuard guard;

    public void recordApply(ProcessInstance instance, String userId) {
        insert(buildBase(instance, userId)
                .setHandleType(HandleTypeEnum.APPLY.getCode())
                .setRemark(HandleTypeEnum.APPLY.getName())
                .setRound(1));
    }

    public void recordClaim(ProcessHandleParam param, Task task) {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        Integer round = resolveRound(instance.getId(), task);
        String remark = StringUtils.hasText(param.getRemark())
                ? param.getRemark() : HandleTypeEnum.CLAIM.getName();
        insert(buildBase(instance, guard.getCurrentUserId())
                .setTaskId(task.getId())
                .setTaskName(task.getName())
                .setTaskDefinitionKey(task.getTaskDefinitionKey())
                .setHandleType(HandleTypeEnum.CLAIM.getCode())
                .setRemark(remark)
                .setRound(round));
    }

    public void recordPass(ProcessHandleParam param, Task task) {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        Integer round = resolveRound(instance.getId(), task);
        String remark = StringUtils.hasText(param.getRemark())
                ? param.getRemark() : HandleTypeEnum.PASS.getName();
        insert(buildBase(instance, guard.getCurrentUserId())
                .setTaskId(task.getId())
                .setTaskName(task.getName())
                .setTaskDefinitionKey(task.getTaskDefinitionKey())
                .setHandleType(HandleTypeEnum.PASS.getCode())
                .setRemark(remark)
                .setRound(round));
    }

    public void recordReject(ProcessHandleParam param, Task task) {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        Integer round = resolveRound(instance.getId(), task);
        String remark = StringUtils.hasText(param.getRemark())
                ? param.getRemark() : HandleTypeEnum.REJECT.getName();
        insert(buildBase(instance, guard.getCurrentUserId())
                .setTaskId(task.getId())
                .setTaskName(task.getName())
                .setTaskDefinitionKey(task.getTaskDefinitionKey())
                .setHandleType(HandleTypeEnum.REJECT.getCode())
                .setRemark(remark)
                .setRound(round));
    }

    public void recordBack(ProcessHandleParam param, Task task,
                           String targetNodeId, String targetNodeName) {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        Integer round = resolveRound(instance.getId(), task);
        String remark = StringUtils.hasText(param.getRemark()) ? param.getRemark() : "驳回";
        String extra = String.format("{\"targetNodeId\":\"%s\",\"targetNodeName\":\"%s\"}",
                targetNodeId, targetNodeName != null ? targetNodeName : "");
        insert(buildBase(instance, guard.getCurrentUserId())
                .setTaskId(task.getId())
                .setTaskName(task.getName())
                .setTaskDefinitionKey(task.getTaskDefinitionKey())
                .setHandleType(HandleTypeEnum.BACK.getCode())
                .setRemark(remark)
                .setRound(round)
                .setExtra(extra));
    }

    private ProcessHandleInfo buildBase(ProcessInstance instance, String userId) {
        return new ProcessHandleInfo()
                .setProcessInstanceId(instance.getId())
                .setHandleUser(userId)
                .setHandleTime(LocalDateTime.now());
    }

    private Integer resolveRound(Integer processInstanceId, Task task) {
        String taskDefinitionKey = task.getTaskDefinitionKey();
        Integer max = processHandleInfoMapper.selectMaxRound(processInstanceId, taskDefinitionKey);
        if (max == null) {
            return 1;
        }
        LocalDateTime latest = processHandleInfoMapper.selectLatestHandleTime(
                processInstanceId, taskDefinitionKey, max);
        if (latest == null) {
            return max;
        }
        Date createTime = task.getCreateTime();
        if (createTime == null) {
            return max;
        }
        LocalDateTime taskCreate = createTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return taskCreate.isAfter(latest) ? max + 1 : max;
    }

    private void insert(ProcessHandleInfo info) {
        if (info.getHandleTime() == null) {
            info.setHandleTime(LocalDateTime.now());
        }
        if (info.getRound() == null) {
            info.setRound(1);
        }
        processHandleInfoMapper.insert(info);
    }
}
