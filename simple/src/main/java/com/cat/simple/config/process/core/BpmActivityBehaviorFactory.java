package com.cat.simple.config.process.core;

import com.cat.simple.config.process.core.userTask.BpmParallelMultiInstanceBehavior;
import com.cat.simple.config.process.core.userTask.BpmUserTaskActivityBehavior;
import lombok.Setter;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.NoneStartEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;

@Setter
public class BpmActivityBehaviorFactory extends DefaultActivityBehaviorFactory {


    @Override
    public UserTaskActivityBehavior createUserTaskActivityBehavior(UserTask userTask) {
        return new BpmUserTaskActivityBehavior(userTask);
    }

    @Override
    public ParallelMultiInstanceBehavior createParallelMultiInstanceBehavior(Activity activity,
                                                                             AbstractBpmnActivityBehavior behavior) {
        return new BpmParallelMultiInstanceBehavior(activity, behavior);
    }


    @Override
    public NoneStartEventActivityBehavior createNoneStartEventActivityBehavior(StartEvent startEvent) {
        return super.createNoneStartEventActivityBehavior(startEvent);
    }




}
