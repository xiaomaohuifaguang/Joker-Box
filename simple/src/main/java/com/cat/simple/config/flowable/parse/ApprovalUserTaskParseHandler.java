package com.cat.simple.config.flowable.parse;

import com.cat.simple.config.flowable.approval.ApprovalContext;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.parse.BpmnParseHandler;
import org.flowable.task.service.delegate.TaskListener;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * Pre-parse 阶段处理器，在 Flowable 默认 UserTaskParseHandler 之前运行。
 * 把 BPMN 中 UserTask 上的自定义扩展元素（approvalType / candidate* / passRate）翻译为 Flowable 原生模型：
 * <ul>
 *   <li>所有四种审批类型：注册 create TaskListener，运行期由 ApprovalTaskCreateListener 完成动态分配</li>
 *   <li>会签 / 或签：注入 MultiInstanceLoopCharacteristics，collection 表达式延迟到运行时由 CandidateResolver 解析</li>
 * </ul>
 */
@Slf4j
public class ApprovalUserTaskParseHandler implements BpmnParseHandler {

    /** 多实例 elementVariable 名，每个子执行的 assignee 变量 */
    public static final String ASSIGNEE_VARIABLE = "approvalAssignee";
    /** 多实例 collection 表达式，运行时调用 CandidateResolver 获取候选人列表 */
    public static final String COLLECTION_EXPRESSION = "${candidateResolver.resolveAssignees(execution)}";
    /** TaskListener 表达式，指向 Spring Bean approvalTaskCreateListener */
    public static final String LISTENER_EXPRESSION = "${approvalTaskCreateListener}";

    @Override
    public Collection<Class<? extends BaseElement>> getHandledTypes() {
        return List.of(UserTask.class);
    }

    @Override
    public void parse(BpmnParse bpmnParse, BaseElement element) {
        if (!(element instanceof UserTask userTask)) {
            return;
        }
        ApprovalContext ctx = ApprovalContext.from(userTask);
        if (ctx == null) {
            return;
        }
        registerCreateListener(userTask);

        switch (ctx.type()) {
            case COUNTERSIGN -> setupMultiInstance(userTask, completionByPassRate(ctx.passRate()));
            case OR_SIGN -> setupMultiInstance(userTask, "${nrOfCompletedInstances >= 1}");
            default -> { /* RANDOM / CLAIM 走运行期监听器 */ }
        }
        log.debug("已处理审批任务 id={}, type={}, passRate={}",
                userTask.getId(), ctx.type(), ctx.passRate());
    }

    /**
     * 为 UserTask 注册 create 事件监听器，避免重复注册。
     */
    private void registerCreateListener(UserTask userTask) {
        boolean exists = userTask.getTaskListeners().stream()
                .anyMatch(l -> TaskListener.EVENTNAME_CREATE.equals(l.getEvent())
                        && LISTENER_EXPRESSION.equals(l.getImplementation()));
        if (exists) {
            return;
        }
        FlowableListener listener = new FlowableListener();
        listener.setEvent(TaskListener.EVENTNAME_CREATE);
        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        listener.setImplementation(LISTENER_EXPRESSION);
        userTask.getTaskListeners().add(listener);
    }

    /**
     * 为多实例审批任务（会签 / 或签）注入 MultiInstanceLoopCharacteristics。
     * 已存在 loopCharacteristics 时跳过，防止覆盖。
     */
    private void setupMultiInstance(UserTask userTask, String completionCondition) {
        if (userTask.getLoopCharacteristics() != null) {
            return;
        }
        MultiInstanceLoopCharacteristics mi = new MultiInstanceLoopCharacteristics();
        mi.setSequential(false);
        mi.setInputDataItem(COLLECTION_EXPRESSION);
        mi.setElementVariable(ASSIGNEE_VARIABLE);
        mi.setCompletionCondition(completionCondition);
        userTask.setLoopCharacteristics(mi);
        userTask.setAssignee("${" + ASSIGNEE_VARIABLE + "}");
    }

    /**
     * 根据 passRate 生成会签完成条件表达式。
     * passRate=1.0 表示全部通过，0.5 表示一半通过。
     *
     * @param passRate 通过率，可为 {@code null}
     * @return JUEL 表达式字符串
     */
    private String completionByPassRate(BigDecimal passRate) {
        BigDecimal rate = (passRate == null ? BigDecimal.ONE : passRate);
        return "${nrOfCompletedInstances >= nrOfInstances * " + rate.toPlainString() + "}";
    }
}