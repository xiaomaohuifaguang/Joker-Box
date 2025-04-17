package com.cat.simple.config.process.core.userTask;


import com.cat.simple.config.process.core.enums.BpmUserTaskApproveMethodEnum;
import com.cat.simple.config.process.core.enums.BpmUserTaskStrategyEnum;
import com.cat.simple.config.process.core.utils.FindProcessorUtils;
import com.cat.simple.config.process.core.utils.FlowElementUtils;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;



import java.util.List;
import java.util.Random;

public class BpmUserTaskActivityBehavior extends UserTaskActivityBehavior {

    private static final Random RANDOM = new Random();


    public BpmUserTaskActivityBehavior(UserTask userTask) {
        super(userTask);
    }


    @Override
    protected void handleAssignments(TaskService taskService,
                                     String assignee, String owner,
                                     List<String> candidateUsers,
                                     List<String> candidateGroups,
                                     TaskEntity task,
                                     ExpressionManager expressionManager,
                                     DelegateExecution execution,
                                     ProcessEngineConfigurationImpl processEngineConfiguration) {
        // 处理逻辑 判断处理人
        FlowElement currentFlowElement = execution.getCurrentFlowElement();

        // 候选人策略
        String candidateStrategy = FlowElementUtils.getCandidateStrategy(currentFlowElement);
        // 候选 参数 人员/角色
//        String candidateParam = FlowElementUtils.getExtensionElementTextByTagName(currentFlowElement, "candidateParam");
//        List<String> candidateParams = new ArrayList<>();
//        if (candidateParam != null) {
//            List<String> list = Arrays.stream(candidateParam.split(",")).toList();
//            candidateParams.addAll(list);
//        }
        List<String> candidateParams = FlowElementUtils.getCandidateParam(currentFlowElement);
        // 多人审批方式 BpmUserTaskApproveMethodEnum
        String approveMethod = FlowElementUtils.getApproveMethod(currentFlowElement);




        if((approveMethod.equals(BpmUserTaskApproveMethodEnum.RANDOM_SIGN.getMethod()))){
            // 随机一人
            List<String> userIds = FindProcessorUtils.find(candidateStrategy, candidateParams);
            assignee = userIds.get(RANDOM.nextInt(userIds.size()));


        }else if(approveMethod.equals(BpmUserTaskApproveMethodEnum.RATIO_SIGN.getMethod())){
            // 会签

            // 指定人员会签
            if(candidateStrategy.equals(BpmUserTaskStrategyEnum.USER.getStrategy())) {
                if (super.multiInstanceActivityBehavior != null) {
                    assignee =  execution.getVariable(super.multiInstanceActivityBehavior.getCollectionElementVariable(), String.class);
                }
            }
        }else if(approveMethod.equals(BpmUserTaskApproveMethodEnum.ANY_SIGN.getMethod())){
            // 或签

            // 指定人员或签
            if(candidateStrategy.equals(BpmUserTaskStrategyEnum.USER.getStrategy())) {
                if (super.multiInstanceActivityBehavior != null) {
                    assignee =  execution.getVariable(super.multiInstanceActivityBehavior.getCollectionElementVariable(), String.class);
                }
            }
        }


        super.handleAssignments(taskService, assignee, owner, candidateUsers, candidateGroups, task, expressionManager, execution, processEngineConfiguration);
    }




}
