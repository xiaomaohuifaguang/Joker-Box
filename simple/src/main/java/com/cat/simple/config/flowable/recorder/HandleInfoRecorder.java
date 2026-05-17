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

/**
 * 流程处理记录记录器，负责将各类操作（申请、认领、通过、驳回、拒绝）持久化为 ProcessHandleInfo。
 */
@Component
public class HandleInfoRecorder {

    @Resource private ProcessHandleInfoMapper processHandleInfoMapper;
    @Resource private ProcessGuard guard;

    /**
     * 记录流程申请动作。
     */
    public void recordApply(ProcessInstance instance, String userId) {
        insert(buildBase(instance, userId)
                .setHandleType(HandleTypeEnum.APPLY.getCode())
                .setRemark(HandleTypeEnum.APPLY.getName())
                .setRound(1));
    }

    /**
     * 记录任务认领动作。
     */
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

    /**
     * 记录任务通过动作。
     */
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

    /**
     * 记录任务拒绝动作。
     */
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

    /**
     * 记录任务驳回动作。
     */
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

    /**
     * 构建 ProcessHandleInfo 基础字段（流程实例ID、处理人、处理时间）。
     */
    private ProcessHandleInfo buildBase(ProcessInstance instance, String userId) {
        return new ProcessHandleInfo()
                .setProcessInstanceId(instance.getId())
                .setHandleUser(userId)
                .setHandleTime(LocalDateTime.now());
    }

    /**
     * 根据任务创建时间与历史最大轮次的最后处理时间比较，判断当前任务应归属的轮次。
     * 若任务创建时间晚于历史最后处理时间，则视为新一轮次；否则复用当前轮次。
     */
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

    /**
     * 插入处理记录，兜底补全 handleTime 与 round 默认值。
     */
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
