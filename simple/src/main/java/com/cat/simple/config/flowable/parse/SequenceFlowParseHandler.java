package com.cat.simple.config.flowable.parse;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.parse.BpmnParseHandler;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

@Component
public class SequenceFlowParseHandler implements BpmnParseHandler {

    @Override
    public Collection<Class<? extends BaseElement>> getHandledTypes() {
        return Set.of(SequenceFlow.class);
    }

    @Override
    public void parse(BpmnParse bpmnParse, BaseElement element) {
        if (!(element instanceof SequenceFlow flow)) {
            return;
        }

        // Read extension element flowable:conditionType if present
        // For now this is a placeholder - actual CUSTOM conditions are injected
        // as standard JUEL expressions during deploy(), so Flowable sees them
        // as normal conditionExpression. This parser is reserved for future use
        // if we need to read custom attributes at parse time.
    }
}
