package com.cat.simple.config.process.core.utils;


import cn.hutool.core.collection.CollUtil;
import com.cat.common.entity.process.ProcessConstants;
import com.cat.simple.config.process.core.enums.BpmUserTaskApproveMethodEnum;
import com.cat.simple.config.process.core.enums.BpmUserTaskStrategyEnum;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;

import java.util.*;

public class FlowElementUtils {




    public static String getExtensionElementTextByTagName(FlowElement flowElement, String tagName) {
        List<ExtensionElement> elementList = flowElement.getExtensionElements().get(tagName);

        if(elementList != null && !elementList.isEmpty()) {
            return elementList.get(0).getElementText();
        }else {
            return null;
        }
    }

    public static List<ExtensionElement> getExtensionElementsByTagName(FlowElement flowElement, String tagName) {
        return flowElement.getExtensionElements().get(tagName);
    }

    public static String getRejectHandlerType(FlowElement flowElement){
        return getExtensionElementTextByTagName(flowElement, "rejectHandlerType");
    }

    public static String getRejectReturnTaskId(FlowElement flowElement){
        return getExtensionElementTextByTagName(flowElement, "rejectReturnTaskId");
    }

    public static List<String> getButtonsSettings(FlowElement flowElement) {
        List<String> buttonsSettingValues = new ArrayList<>();
        List<ExtensionElement> buttonsSetting = getExtensionElementsByTagName(flowElement, "buttonsSetting");
        for (ExtensionElement extensionElement : buttonsSetting) {
            Map<String, List<ExtensionAttribute>> attributes = extensionElement.getAttributes();
            String id = getAttributeValue(attributes, "id");
            String enable = getAttributeValue(attributes, "enable");
            String displayName = getAttributeValue(attributes, "displayName");
            // 5. 如果enable=true，则收集id
            if (id != null && "true".equalsIgnoreCase(enable) && ProcessConstants.handleButtons.contains(id)) {
                buttonsSettingValues.add(id);
            }
        }

        return buttonsSettingValues;
    }

    // 辅助方法：从属性Map中获取指定属性的值
    private static String getAttributeValue(Map<String, List<ExtensionAttribute>> attributes, String attributeName) {
        if (attributes == null || !attributes.containsKey(attributeName)) {
            return null;
        }

        List<ExtensionAttribute> attributeList = attributes.get(attributeName);
        if (attributeList != null && !attributeList.isEmpty()) {
            return attributeList.get(0).getValue();
        }

        return null;
    }




    public static List<String> getCandidateParam(FlowElement flowElement){
        String candidateParam = getExtensionElementTextByTagName(flowElement, "candidateParam");
        List<String> candidateParams = new ArrayList<>();
        if (candidateParam != null) {
            List<String> list = Arrays.stream(candidateParam.split(",")).toList();
            candidateParams.addAll(list);
        }
        return candidateParams;
    }

    public static String getApproveMethod(FlowElement flowElement){
        String approveMethod = getExtensionElementTextByTagName(flowElement, "approveMethod");
        return approveMethod == null ? BpmUserTaskApproveMethodEnum.RANDOM_SIGN.getMethod() : approveMethod;
    }


    public static String getCandidateStrategy(FlowElement flowElement){
        // 候选人策略
        String candidateStrategy = FlowElementUtils.getExtensionElementTextByTagName(flowElement, "candidateStrategy");
        return candidateStrategy == null ? BpmUserTaskStrategyEnum.USER.getStrategy() : candidateStrategy;
    }


    public static EndEvent getEndEvent(BpmnModel model) {
        Process process = model.getMainProcess();
        Collection<FlowElement> flowElements = process.getFlowElements();
        for (FlowElement flowElement : flowElements) {
            if(flowElement instanceof EndEvent){
                return (EndEvent) flowElement;
            }
        }
        return null;
    }


    public static FlowElement getFlowElementById(BpmnModel model, String flowElementId) {
        Process process = model.getMainProcess();
        return process.getFlowElement(flowElementId);
    }

    /**
     * 判断 源节点 到 目标节点 是否可达
     * @param source    起始节点
     * @param target    目标节点
     * @param visitedElements 已经经过的连线的 ID，用于判断线路是否重复
     * @return  结果
     */
    public static boolean isSequentialReachable(FlowElement source, FlowElement target, Set<String> visitedElements) {
        visitedElements = visitedElements == null ? new HashSet<>() : visitedElements;
        // 不能是开始事件和子流程
        if (source instanceof StartEvent && isInEventSubprocess(source)) {
            return false;
        }

        // 根据类型，获取入口连线
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);
        if (CollUtil.isEmpty(sequenceFlows)) {
            return true;
        }
        // 循环找到目标元素
        for (SequenceFlow sequenceFlow : sequenceFlows) {
            // 如果发现连线重复，说明循环了，跳过这个循环
            if (visitedElements.contains(sequenceFlow.getId())) {
                continue;
            }
            // 添加已经走过的连线
            visitedElements.add(sequenceFlow.getId());
            // 这条线路存在目标节点，这条线路完成，进入下个线路
            FlowElement sourceFlowElement = sequenceFlow.getSourceFlowElement();
            if (target.getId().equals(sourceFlowElement.getId())) {
                continue;
            }
            // 如果目标节点为并行网关，则不继续
            if (sourceFlowElement instanceof ParallelGateway) {
                return false;
            }
            // 否则就继续迭代
            if (!isSequentialReachable(sourceFlowElement, target, visitedElements)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据正在运行的任务节点，迭代获取子级任务节点列表，向后找
     *
     * @param source          起始节点
     * @param runTaskKeyList  正在运行的任务 Key，用于校验任务节点是否是正在运行的节点
     * @param hasSequenceFlow 已经经过的连线的 ID，用于判断线路是否重复
     * @param userTaskList    需要撤回的用户任务列表
     * @return 子级任务节点列表
     */
    public static List<UserTask> iteratorFindChildUserTasks(FlowElement source, List<String> runTaskKeyList,
                                                            Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
        userTaskList = userTaskList == null ? new ArrayList<>() : userTaskList;
        // 如果该节点为开始节点，且存在上级子节点，则顺着上级子节点继续迭代
        if (source instanceof StartEvent && source.getSubProcess() != null) {
            userTaskList = iteratorFindChildUserTasks(source.getSubProcess(), runTaskKeyList, hasSequenceFlow, userTaskList);
        }

        // 根据类型，获取出口连线
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);
        if (sequenceFlows == null) {
            return userTaskList;
        }
        // 循环找到目标元素
        for (SequenceFlow sequenceFlow : sequenceFlows) {
            // 如果发现连线重复，说明循环了，跳过这个循环
            if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                continue;
            }
            // 添加已经走过的连线
            hasSequenceFlow.add(sequenceFlow.getId());
            // 如果为用户任务类型，且任务节点的 Key 正在运行的任务中存在，添加
            if (sequenceFlow.getTargetFlowElement() instanceof UserTask && runTaskKeyList.contains((sequenceFlow.getTargetFlowElement()).getId())) {
                userTaskList.add((UserTask) sequenceFlow.getTargetFlowElement());
                continue;
            }
            // 如果节点为子流程节点情况，则从节点中的第一个节点开始获取
            if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
                List<UserTask> childUserTaskList = iteratorFindChildUserTasks((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), runTaskKeyList, hasSequenceFlow, null);
                // 如果找到节点，则说明该线路找到节点，不继续向下找，反之继续
                if (CollUtil.isNotEmpty(childUserTaskList)) {
                    userTaskList.addAll(childUserTaskList);
                    continue;
                }
            }
            // 继续迭代
            userTaskList = iteratorFindChildUserTasks(sequenceFlow.getTargetFlowElement(), runTaskKeyList, hasSequenceFlow, userTaskList);
        }
        return userTaskList;
    }



    /**
     * 判断当前节点是否属于不同的子流程
     *
     * @param flowElement 被判断的节点
     * @return true 表示属于子流程
     */
    private static boolean isInEventSubprocess(FlowElement flowElement) {
        FlowElementsContainer flowElementsContainer = flowElement.getParentContainer();
        while (flowElementsContainer != null) {
            if (flowElementsContainer instanceof EventSubProcess) {
                return true;
            }

            if (flowElementsContainer instanceof FlowElement) {
                flowElementsContainer = ((FlowElement) flowElementsContainer).getParentContainer();
            } else {
                flowElementsContainer = null;
            }
        }
        return false;
    }

    /**
     * 根据节点，获取入口连线
     *
     * @param source 起始节点
     * @return 入口连线列表
     */
    public static List<SequenceFlow> getElementIncomingFlows(FlowElement source) {
        if (source instanceof FlowNode) {
            return ((FlowNode) source).getIncomingFlows();
        }
        return new ArrayList<>();
    }

    /**
     * 根据节点，获取出口连线
     *
     * @param source 起始节点
     * @return 出口连线列表
     */
    public static List<SequenceFlow> getElementOutgoingFlows(FlowElement source) {
        if (source instanceof FlowNode) {
            return ((FlowNode) source).getOutgoingFlows();
        }
        return new ArrayList<>();
    }


}
