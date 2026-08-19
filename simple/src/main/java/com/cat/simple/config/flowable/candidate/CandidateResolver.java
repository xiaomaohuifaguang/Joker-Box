package com.cat.simple.config.flowable.candidate;

import com.cat.common.entity.auth.User;
import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.approval.ApprovalTypeEnum;
import com.cat.simple.process.mapper.ProcessInstanceMapper;
import com.cat.simple.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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
    private RuntimeService runtimeService;

    @Resource
    private ProcessInstanceMapper processInstanceMapper;

    @Resource
    private UserMapper userMapper;


    /**
     * 单例
     * @param execution 当前流程执行上下文
     * @return 去重后的用户 ID 列表；解析结果为空时抛异常，防止 Flowable 跳过节点
     */
    public String resolveSingleAssignee(DelegateExecution execution) {
        ApprovalContext ctx = readContext(execution.getProcessDefinitionId(), execution.getCurrentActivityId());
        if (ctx == null) {
            throw new IllegalStateException("activityId=" + execution.getCurrentActivityId() + " 缺少 approvalType");
        }
        List<String> pool = resolve(ctx, execution.getProcessInstanceId());
        if (ctx.type() == ApprovalTypeEnum.RANDOM) {
            return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        }
        // APPLICANT_SELF：resolve 已返回发起人单例
        return pool.get(0);
    }

    /** 认领节点候选人表达式入口（CLAIM），返回用户 ID 列表 */
    public List<String> resolveCandidateUsers(DelegateExecution execution) {
        ApprovalContext ctx = readContext(execution.getProcessDefinitionId(), execution.getCurrentActivityId());
        if (ctx == null) {
            throw new IllegalStateException("activityId=" + execution.getCurrentActivityId() + " 缺少 approvalType");
        }
        return resolve(ctx, execution.getProcessInstanceId());
    }


    /**
     * 多实例 collection 表达式入口。
     * Flowable 在进入会签 / 或签节点时解析该表达式，获取用户 ID 列表以决定实例数量。
     *
     * @param execution 当前流程执行上下文
     * @return 去重后的用户 ID 列表；解析结果为空时抛异常，防止 Flowable 跳过节点
     */
    public List<String> resolveAssignees(DelegateExecution execution) {

        ApprovalContext ctx = readContext(execution.getProcessDefinitionId(), execution.getCurrentActivityId());
        if (ctx == null) {
            log.warn("activityId={} 缺少 approvalType, 候选人为空", execution.getCurrentActivityId());
            return List.of();
        }
        String cacheKey = "cachedAssignees_" + execution.getCurrentActivityId();
        Object variableLocal = execution.getVariable(cacheKey);
        if (variableLocal instanceof List<?> list) {
            return (List<String>) list;
        }

        List<String> resolve = resolve(ctx, null);

        execution.setVariable(cacheKey, resolve);

        return resolve;
    }

    /**
     * 将 4 个候选源合并去重为最终用户 ID 集合。
     * v1：candidateUsers 直接当用户 ID 处理，其余候选源 TODO 待接入真实展开逻辑。
     *
     * @param ctx 审批上下文
     * @return 去重后的用户 ID 列表
     * @throws IllegalStateException 展开结果为空时抛出
     */
    public List<String> resolve(ApprovalContext ctx, String processInstanceId) {

        if(ctx.type().equals(ApprovalTypeEnum.APPLICANT_SELF) && StringUtils.hasText(processInstanceId)){
            String applicant = findApplicant(processInstanceId);
            if (applicant == null) {
                throw new IllegalStateException("无法解析流程申请人, processInstanceId=" + processInstanceId);
            }
            return List.of(applicant);
        }


        LinkedHashSet<User> usersByCtxWithoutApplicant = getUsersByCtxWithoutApplicant(ctx);
        Set<String> set = usersByCtxWithoutApplicant.stream().map(u -> String.valueOf(u.getId())).collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> result = new java.util.ArrayList<>(List.copyOf(set));
        if (result.isEmpty()) {
            throw new IllegalStateException("候选人解析结果为空，请检查流程配置");
        }
        if(ctx.type().equals(ApprovalTypeEnum.RANDOM_COUNTERSIGN) || ctx.type().equals(ApprovalTypeEnum.RANDOM_OR_SIGN)){
            BigDecimal bigDecimal = ctx.randomCount();
            int size = result.size();
            int count = (bigDecimal == null) ? 1 : bigDecimal.intValue();
            // bigDecimal > size → min 截断到 size
            // bigDecimal < 1   → max 兜底到 1
            // size == 0        → 结果为0，下方短路返回空集合
            count = Math.max(1, Math.min(count, size));

            List<String> mutable = new ArrayList<>(result);
            Collections.shuffle(mutable);
            List<String> strings = mutable.subList(0, count);
            return new ArrayList<>(strings);
        }

        if(ctx.type().equals(ApprovalTypeEnum.CHOOSE) || ctx.type().equals(ApprovalTypeEnum.CHOOSE_COUNTERSIGN) || ctx.type().equals(ApprovalTypeEnum.CHOOSE_OR_SIGN)){
            throw new IllegalStateException("请选择合适的审批人");
        }


        return result;
    }


    public LinkedHashSet<User> getUsersByCtxWithoutApplicant(ApprovalContext ctx){
        LinkedHashSet<User> userList = new LinkedHashSet<>();
        if (!ObjectUtils.isEmpty(ctx.candidateUsers())) {
            List<User> users = userMapper.selectListByIds(ctx.candidateUsers());
            userList.addAll(users);
        }
        if (!ObjectUtils.isEmpty(ctx.candidateRoles())) {
            List<User> users = userMapper.selectListByRoles(ctx.candidateRoles());
            userList.addAll(users);
        }
        // TODO: 用户组 → 用户 展开
        if (!ObjectUtils.isEmpty(ctx.candidateGroups())) {
//            set.addAll(ctx.candidateGroups());
        }
        if (!ObjectUtils.isEmpty(ctx.candidateDepts())) {
            List<User> users = userMapper.selectListByOrgs(ctx.candidateDepts());
            userList.addAll(users);
        }
        return userList;
    }

    /**
     * 解析流程申请人：Flowable 实例 → businessKey → 业务实例 createBy。
     *
     * @param processInstanceId Flowable 流程实例 ID
     * @return 申请人用户 ID；实例缺失时返回 {@code null}
     */
    public String findApplicant(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (processInstance == null || !StringUtils.hasText(processInstance.getBusinessKey())) {
            return null;
        }
        com.cat.common.entity.process.ProcessInstance catProcessInstance =
                processInstanceMapper.selectInfoById(Integer.valueOf(processInstance.getBusinessKey()));
        return catProcessInstance == null ? null : catProcessInstance.getCreateBy();
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