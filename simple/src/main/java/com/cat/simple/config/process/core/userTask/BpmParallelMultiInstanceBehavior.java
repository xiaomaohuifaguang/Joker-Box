package com.cat.simple.config.process.core.userTask;

import com.cat.simple.config.process.core.utils.FindProcessorUtils;
import com.cat.simple.config.process.core.utils.FlowElementUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;

import java.util.List;

public class BpmParallelMultiInstanceBehavior extends ParallelMultiInstanceBehavior {

    public BpmParallelMultiInstanceBehavior(Activity activity,
                                            AbstractBpmnActivityBehavior innerActivityBehavior) {
        super(activity, innerActivityBehavior);
    }


    @Override
    protected int resolveNrOfInstances(DelegateExecution execution) {
        // 第一步，设置 collectionVariable 和 CollectionVariable
        // 从  execution.getVariable() 读取所有任务处理人的 key
        super.collectionExpression = null; // collectionExpression 和 collectionVariable 是互斥的
        super.collectionVariable = execution.getCurrentActivityId()+"_assignees";
        // 从 execution.getVariable() 读取当前所有任务处理的人的 key
        super.collectionElementVariable = execution.getCurrentActivityId()+"_assignees";


//        String variable = execution.getVariable(super.collectionVariable, String.class);
        List<String> assigneeUserIds;
        try{
            assigneeUserIds = ( List<String> ) execution.getVariable(super.collectionVariable, List.class);
        }catch (Exception e){
            assigneeUserIds = null;
        }


        if (assigneeUserIds == null) {

            String candidateStrategy = FlowElementUtils.getCandidateStrategy(execution.getCurrentFlowElement());
            List<String> candidateParams = FlowElementUtils.getCandidateParam(execution.getCurrentFlowElement());

            assigneeUserIds = FindProcessorUtils.find(candidateStrategy, candidateParams);


            execution.setVariableLocal(super.collectionVariable, assigneeUserIds);
        }
        return assigneeUserIds.size();
    }


}
