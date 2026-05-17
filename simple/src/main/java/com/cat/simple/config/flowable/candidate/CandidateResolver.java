package com.cat.simple.config.flowable.candidate;

import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import org.springframework.util.ObjectUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cat.simple.config.flowable.constant.ProcessConstants.BACK_ASSIGNEE_VAR_PREFIX;

/**
 * 候选人解析器，负责把 BPMN 上的候选源（用户 / 角色 / 用户组 / 部门）展开为最终用户 ID 列表。
 * 作为 Spring Bean 被多实例 collection 表达式 {@code ${candidateResolver.resolveAssignees(execution)}} 调用。
 */
@Slf4j
@Component("candidateResolver")
public class CandidateResolver {

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private UserMapper userMapper;

    /**
     * 多实例 collection 表达式入口。
     * Flowable 在进入会签 / 或签节点时解析该表达式，获取用户 ID 列表以决定实例数量。
     *
     * @param execution 当前流程执行上下文
     * @return 去重后的用户 ID 列表；解析结果为空时抛异常，防止 Flowable 跳过节点
     */
    public List<String> resolveAssignees(DelegateExecution execution) {
        // 回退场景：优先使用注入的历史处理人列表，保证任务数和上一轮一致
        String backVarName = BACK_ASSIGNEE_VAR_PREFIX + execution.getCurrentActivityId();
        Object backAssignees = execution.getVariable(backVarName);
        if (backAssignees instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(Object::toString).toList();
        }

        ApprovalContext ctx = readContext(execution.getProcessDefinitionId(), execution.getCurrentActivityId());
        if (ctx == null) {
            log.warn("activityId={} 缺少 approvalType, 候选人为空", execution.getCurrentActivityId());
            return List.of();
        }
        return resolve(ctx);
    }

    /**
     * 将 4 个候选源合并去重为最终用户 ID 集合。
     * v1：candidateUsers 直接当用户 ID 处理，其余候选源 TODO 待接入真实展开逻辑。
     *
     * @param ctx 审批上下文
     * @return 去重后的用户 ID 列表
     * @throws IllegalStateException 展开结果为空时抛出
     */
    public List<String> resolve(ApprovalContext ctx) {
        Set<String> set = new LinkedHashSet<>();
        if (!ObjectUtils.isEmpty(ctx.candidateUsers())) {
            set.addAll(userMapper.selectListByIds(ctx.candidateUsers()).stream().map(u -> String.valueOf(u.getId())).collect(Collectors.toSet()));
        }
        if (!ObjectUtils.isEmpty(ctx.candidateRoles())) {
            set.addAll(userMapper.selectListByRoles(ctx.candidateRoles()).stream().map(u -> String.valueOf(u.getId())).collect(Collectors.toSet()));
        }
        // TODO: 用户组 → 用户 展开
        if (!ObjectUtils.isEmpty(ctx.candidateGroups())) {
//            set.addAll(ctx.candidateGroups());
        }
        if (!ObjectUtils.isEmpty(ctx.candidateDepts())) {
            set.addAll(userMapper.selectListByOrgs(ctx.candidateDepts()).stream().map(u -> String.valueOf(u.getId())).collect(Collectors.toSet()));
        }
        List<String> result = List.copyOf(set);
        if (result.isEmpty()) {
            throw new IllegalStateException("候选人解析结果为空，请检查流程配置");
        }
        return result;
    }

    /**
     * 从流程定义模型中读取当前活动节点的审批上下文。
     *
     * @param processDefinitionId 流程定义 ID
     * @param activityId          当前活动 ID
     * @return 审批上下文；节点不存在或非审批节点返回 {@code null}
     */
    private ApprovalContext readContext(String processDefinitionId, String activityId) {
        BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
        if (model == null) {
            return null;
        }
        if (!(model.getFlowElement(activityId) instanceof UserTask userTask)) {
            return null;
        }
        return ApprovalContext.from(userTask);
    }
}