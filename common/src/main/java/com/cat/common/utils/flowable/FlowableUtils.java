package com.cat.common.utils.flowable;

import com.cat.common.entity.DTO;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.io.InputStreamProvider;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class FlowableUtils {

    /**
     * 校验 BPMN20 XML 字符串是否合规
     *
     * @param bpmnXml BPMN20 XML 字符串
     * @return 校验结果 DTO，flag=true 表示合规，flag=false 表示不合规并附带错误信息
     */
    public static DTO<?> validateBpmnXml(String bpmnXml) {
        if (bpmnXml == null || bpmnXml.isBlank()) {
            return DTO.error("BPMN XML 内容不能为空");
        }

        BpmnModel bpmnModel;
        try {
            bpmnModel = convertToBpmnModel(bpmnXml);
        } catch (Exception e) {
            return DTO.error("BPMN XML 格式错误: " + e.getMessage());
        }

        return performCustomValidation(bpmnModel);
    }

    /**
     * 将 BPMN XML 字符串转换为 BpmnModel
     *
     * @param bpmnXml BPMN XML 字符串
     * @return BpmnModel
     */
    public static BpmnModel convertToBpmnModel(String bpmnXml) {
        InputStreamProvider provider = () -> new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8));
        BpmnXMLConverter converter = new BpmnXMLConverter();
        return converter.convertToBpmnModel(provider, true, true);
    }

    /**
     * 执行 BPMN 自定义合规校验
     *
     * @param model BpmnModel
     * @return 校验结果 DTO
     */
    private static DTO<?> performCustomValidation(BpmnModel model) {
        Process process = model.getMainProcess();
        if (process == null) {
            return DTO.error("BPMN 流程定义不能为空");
        }

        if (process.getId() == null || process.getId().isBlank()) {
            return DTO.error("流程定义 ID 不能为空");
        }

        List<FlowElement> flowElements = new ArrayList<>(process.getFlowElements());

        // 1. 必须存在开始事件和结束事件
        List<StartEvent> startEvents = flowElements.stream()
                .filter(e -> e instanceof StartEvent)
                .map(e -> (StartEvent) e)
                .toList();
        List<EndEvent> endEvents = flowElements.stream()
                .filter(e -> e instanceof EndEvent)
                .map(e -> (EndEvent) e)
                .toList();

        if (startEvents.isEmpty()) {
            return DTO.error("流程必须包含至少一个开始事件");
        }
        if (endEvents.isEmpty()) {
            return DTO.error("流程必须包含至少一个结束事件");
        }

        // 2. 节点连线完整性校验
        for (FlowElement element : flowElements) {
            if (element instanceof FlowNode) {
                FlowNode node = (FlowNode) element;

                // 开始事件必须有出口连线
                if (element instanceof StartEvent) {
                    if (isEmpty(node.getOutgoingFlows())) {
                        return DTO.error("开始事件 [" + nodeName(element) + "] 必须有出口连线");
                    }
                }
                // 结束事件必须有入口连线
                else if (element instanceof EndEvent) {
                    if (isEmpty(node.getIncomingFlows())) {
                        return DTO.error("结束事件 [" + nodeName(element) + "] 必须有入口连线");
                    }
                }
                // 其他节点（任务、网关等）必须同时有入口和出口连线
                else {
                    if (isEmpty(node.getIncomingFlows())) {
                        return DTO.error("节点 [" + nodeName(element) + "] 缺少入口连线");
                    }
                    if (isEmpty(node.getOutgoingFlows())) {
                        return DTO.error("节点 [" + nodeName(element) + "] 缺少出口连线");
                    }
                }
            }
        }

        // 3. SequenceFlow 有效性校验：sourceRef / targetRef 必须指向真实存在的节点
        Map<String, FlowElement> elementMap = flowElements.stream()
                .collect(Collectors.toMap(FlowElement::getId, e -> e));

        for (FlowElement element : flowElements) {
            if (element instanceof SequenceFlow) {
                SequenceFlow sf = (SequenceFlow) element;
                if (!elementMap.containsKey(sf.getSourceRef())) {
                    return DTO.error("连线 [" + sf.getId() + "] 的源节点 [" + sf.getSourceRef() + "] 不存在");
                }
                if (!elementMap.containsKey(sf.getTargetRef())) {
                    return DTO.error("连线 [" + sf.getId() + "] 的目标节点 [" + sf.getTargetRef() + "] 不存在");
                }
            }
        }

        // 4. 流程连通性校验：所有非连线节点必须能从某个开始事件到达
        Set<String> reachableIds = new HashSet<>();
        Queue<FlowNode> queue = new LinkedList<>();
        for (StartEvent startEvent : startEvents) {
            reachableIds.add(startEvent.getId());
            queue.offer(startEvent);
        }

        while (!queue.isEmpty()) {
            FlowNode current = queue.poll();
            List<SequenceFlow> outFlows = current.getOutgoingFlows();
            if (outFlows != null) {
                for (SequenceFlow sf : outFlows) {
                    FlowElement target = elementMap.get(sf.getTargetRef());
                    if (target instanceof FlowNode && !reachableIds.contains(target.getId())) {
                        reachableIds.add(target.getId());
                        queue.offer((FlowNode) target);
                    }
                }
            }
        }

        List<String> unreachableNodes = flowElements.stream()
                .filter(e -> !(e instanceof SequenceFlow))
                .filter(e -> !reachableIds.contains(e.getId()))
                .map(FlowElement::getId)
                .toList();

        if (!unreachableNodes.isEmpty()) {
            return DTO.error("存在不可达的孤立节点: " + String.join(", ", unreachableNodes));
        }

        return DTO.success();
    }

    private static String nodeName(FlowElement element) {
        String name = element.getName();
        if (name == null || name.isBlank()) {
            name = element.getClass().getSimpleName();
        }
        return name + "(" + element.getId() + ")";
    }

    private static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }


    public static String getStartEventName(BpmnModel bpmnModel) {
        Process process = bpmnModel.getMainProcess();
        if (process == null) {
            return null;
        }
        return process.getFlowElements().stream()
                .filter(e -> e instanceof StartEvent)
                .map(FlowElement::getName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
    }


}