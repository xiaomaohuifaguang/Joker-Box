package com.cat.simple.config.flowable.util;

import com.alibaba.fastjson.JSON;
import com.cat.common.entity.DTO;
import com.cat.common.entity.process.*;
import com.cat.common.entity.process.constants.ConditionTypeConstants;
import com.cat.common.entity.process.constants.FieldPermissionConstants;
import com.cat.common.entity.process.constants.FormBindType;import com.cat.common.entity.process.constants.ServiceTaskConstants;
import com.cat.common.entity.process.designer.Edge;
import com.cat.common.entity.process.designer.Node;
import com.cat.common.entity.process.designer.RawData;
import com.cat.common.utils.JSONUtils;
import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.candidate.CandidateResolver;
import com.cat.simple.config.flowable.enums.BackAssigneePolicyEnum;
import com.cat.simple.config.flowable.enums.BackTypeEnum;
import jakarta.annotation.Resource;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.cat.simple.config.flowable.enums.ExtensionElementEnum.*;

@Component
public class FlowableUtils {

    @Resource
    private RepositoryService repositoryService;
    @Resource
    private TaskService taskService;
    @Resource
    private HistoryService historyService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private CandidateResolver candidateResolver;

    /**
     * 校验 BPMN20 XML 字符串是否合规
     *
     * @param bpmnXml BPMN20 XML 字符串
     * @return 校验结果 DTO，flag=true 表示合规，flag=false 表示不合规并附带错误信息
     */
    public DTO<BpmnModel> validateBpmnXml(String bpmnXml) {
        if (bpmnXml == null || bpmnXml.isBlank()) {
            return DTO.error("BPMN XML 内容不能为空", null);
        }
        BpmnModel bpmnModel;
        try {
            bpmnModel = convertToBpmnModel(bpmnXml);
        } catch (Exception e) {
            return DTO.error("BPMN XML 格式错误: " + e.getMessage(), null);
        }
        DTO<?> dto = performCustomValidation(bpmnModel);
        return dto.flag ? DTO.back(bpmnModel) : DTO.error(dto.msg, null);
    }

    /**
     * 将 BPMN XML 字符串转换为 BpmnModel
     *
     * @param bpmnXml BPMN XML 字符串
     * @return BpmnModel
     */
    public BpmnModel convertToBpmnModel(String bpmnXml) {
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
    public DTO<?> performCustomValidation(BpmnModel model) {
        Process process = model.getMainProcess();
        if (process == null) {
            return DTO.error("BPMN 流程定义不能为空");
        }
        if (process.getId() == null || process.getId().isBlank()) {
            return DTO.error("流程定义 ID 不能为空");
        }

        List<FlowElement> flowElements = new ArrayList<>(process.getFlowElements());

        // 1. 必须存在开始事件和结束事件
        List<StartEvent> startEvents = flowElements.stream().filter(e -> e instanceof StartEvent).map(e -> (StartEvent) e).toList();
        List<EndEvent> endEvents = flowElements.stream().filter(e -> e instanceof EndEvent).map(e -> (EndEvent) e).toList();

        if (startEvents.isEmpty()) {
            return DTO.error("流程必须包含至少一个开始事件");
        }
        if (endEvents.isEmpty()) {
            return DTO.error("流程必须包含至少一个结束事件");
        }

        // 2. 节点连线完整性校验
        for (FlowElement element : flowElements) {
            if (element instanceof FlowNode node) {
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
        Map<String, FlowElement> elementMap = flowElements.stream().collect(Collectors.toMap(FlowElement::getId, e -> e));

        for (FlowElement element : flowElements) {
            if (element instanceof SequenceFlow sf) {
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

        List<String> unreachableNodes = flowElements.stream().filter(e -> !(e instanceof SequenceFlow)).map(FlowElement::getId).filter(id -> !reachableIds.contains(id)).toList();

        if (!unreachableNodes.isEmpty()) {
            return DTO.error("存在不可达的孤立节点: " + String.join(", ", unreachableNodes));
        }

        return DTO.success();
    }

    private String nodeName(FlowElement element) {
        String name = StringUtils.hasText(element.getName()) ? element.getName() : element.getClass().getSimpleName();
        return name + "(" + element.getId() + ")";
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    public String getStartEventName(BpmnModel bpmnModel) {
        Process process = bpmnModel.getMainProcess();
        if (process == null) {
            return null;
        }
        return process.getFlowElements().stream().filter(e -> e instanceof StartEvent).map(FlowElement::getName).filter(StringUtils::hasText).findFirst().orElse(null);
    }

    public ProcessDefinition build(ProcessDefinition processDefinition) {
        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();

        process.setId(processDefinition.getProcessKey());
        process.setName(processDefinition.getProcessName());
        process.setDocumentation(processDefinition.getProcessDescription());
        process.setExecutable(true);

        RawData rawData = processDefinition.getRawData();


        List<Node> nodes = rawData.getNodes();
        List<String> exclusiveGatewayIds = new ArrayList<>();
        List<String> inclusiveGatewayIds = new ArrayList<>();
        List<ProcessGatewayCondition> gatewayConditions = new ArrayList<>();
        List<ProcessDefinitionForm> nodeFormBindings = new ArrayList<>();
        List<ProcessNodeFieldPermission> processNodeFieldPermissionsAll = new ArrayList<>();
        if (!CollectionUtils.isEmpty(nodes)) {
            for (Node node : nodes) {
                switch (node.getType()) {
                    case "startEvent" -> {
                        StartEvent startEvent = new StartEvent();
                        startEvent.setId(node.getId());
                        Map<String, Object> data = node.getData();
                        startEvent.setName((String) data.get("label"));
                        startEvent.setDocumentation((String) data.get("description"));
                        process.addFlowElement(startEvent);
                        ProcessDefinitionForm nodeFormBinding = getProcessDefinitionForm(node.getId(),
                                processDefinition.getGlobalFormBinding(),
                                data
                        );
                        if(Objects.nonNull(nodeFormBinding)){
                            nodeFormBindings.add(nodeFormBinding);
                            List<ProcessNodeFieldPermission> processNodeFieldPermissions = getProcessNodeFieldPermissions(node.getId(), data);
                            processNodeFieldPermissionsAll.addAll(processNodeFieldPermissions);
                        }
                    }
                    case "userTask" -> {
                        UserTask userTask = new UserTask();
                        userTask.setId(node.getId());
                        Map<String, Object> data = node.getData();
                        userTask.setName((String) data.get("label"));
                        userTask.setDocumentation((String) data.get("description"));
                        setUserTaskExtensionElements(userTask, data);
                        process.addFlowElement(userTask);
                        ProcessDefinitionForm nodeFormBinding = getProcessDefinitionForm(node.getId(),
                                processDefinition.getGlobalFormBinding(),
                                data
                        );
                        if(Objects.nonNull(nodeFormBinding)){
                            nodeFormBindings.add(nodeFormBinding);
                            List<ProcessNodeFieldPermission> processNodeFieldPermissions = getProcessNodeFieldPermissions(node.getId(), data);
                            processNodeFieldPermissionsAll.addAll(processNodeFieldPermissions);
                        }

                    }
                    case "serviceTask" -> {
                        ServiceTask serviceTask = new ServiceTask();
                        serviceTask.setId(node.getId());
                        Map<String, Object> data = node.getData();
                        serviceTask.setName((String) data.get("label"));
                        serviceTask.setDocumentation((String) data.get("description"));
                        setServiceTaskExtensionElements(serviceTask, data);
                        process.addFlowElement(serviceTask);
                    }
                    case "exclusiveGateway" -> {
                        ExclusiveGateway gateway = new ExclusiveGateway();
                        gateway.setId(node.getId());
                        exclusiveGatewayIds.add(node.getId());
                        Map<String, Object> data = node.getData();
                        gateway.setName((String) data.get("label"));
                        gateway.setDocumentation((String) data.get("description"));
                        process.addFlowElement(gateway);
                    }
                    case "parallelGateway" -> {
                        ParallelGateway gateway = new ParallelGateway();
                        gateway.setId(node.getId());
                        Map<String, Object> data = node.getData();
                        gateway.setName((String) data.get("label"));
                        gateway.setDocumentation((String) data.get("description"));
                        process.addFlowElement(gateway);
                    }
                    case "inclusiveGateway" -> {
                        InclusiveGateway gateway = new InclusiveGateway();
                        gateway.setId(node.getId());
                        inclusiveGatewayIds.add(node.getId());
                        Map<String, Object> data = node.getData();
                        gateway.setName((String) data.get("label"));
                        gateway.setDocumentation((String) data.get("description"));
                        process.addFlowElement(gateway);
                    }
                    case "endEvent" -> {
                        EndEvent endEvent = new EndEvent();
                        endEvent.setId(node.getId());
                        Map<String, Object> data = node.getData();
                        endEvent.setName((String) data.get("label"));
                        endEvent.setDocumentation((String) data.get("description"));
                        process.addFlowElement(endEvent);
                    }
                }
            }
        }

        List<Edge> edges = rawData.getEdges();
        if (!CollectionUtils.isEmpty(edges)) {
            for (Edge edge : edges) {
                SequenceFlow sequenceFlow = new SequenceFlow(edge.getSource(), edge.getTarget());
                sequenceFlow.setId(edge.getId());
                Map<String, Object> data = edge.getData();
                if (Objects.nonNull(data)) {
                    sequenceFlow.setName((String) data.get("label"));
                    sequenceFlow.setDocumentation((String) data.get("description"));

                    Boolean isDefault = (Boolean) data.get("isDefault");
                    String conditionType = (String) data.get("conditionType");
                    String nativeExpression = (String) data.get("nativeExpression");

                    ProcessGatewayCondition processGatewayCondition = new ProcessGatewayCondition(processDefinition.getId(), processDefinition.getVersion(), edge.getId(), edge.getSource(), edge.getTarget());

                    // 统一处理排他网关和包容网关的连线逻辑
                    if (exclusiveGatewayIds.contains(edge.getSource())) {
                        handleGatewayFlow(process, edge, sequenceFlow, data, isDefault, conditionType, nativeExpression, gatewayConditions, processGatewayCondition, true);
                    } else if (inclusiveGatewayIds.contains(edge.getSource())) {
                        handleGatewayFlow(process, edge, sequenceFlow, data, isDefault, conditionType, nativeExpression, gatewayConditions, processGatewayCondition, false);
                    }
                }
                process.addFlowElement(sequenceFlow);
            }
        }

        bpmnModel.addProcess(process);

        processDefinition.setXmlStr(bpmnModelToXml(bpmnModel));
        processDefinition.setGatewayConditions(gatewayConditions);
        processDefinition.setNodeFormBindings(nodeFormBindings);
        processDefinition.setNodeFieldPermissions(processNodeFieldPermissionsAll);
        return processDefinition;
    }

    /**
     * 处理网关（排他/包容）的连线逻辑
     */
    private void handleGatewayFlow(Process process, Edge edge, SequenceFlow sequenceFlow, Map<String, Object> data, Boolean isDefault, String conditionType, String nativeExpression, List<ProcessGatewayCondition> gatewayConditions, ProcessGatewayCondition processGatewayCondition, boolean isExclusive) {
        if (Boolean.TRUE.equals(isDefault)) {
            processGatewayCondition.setIsDefault(true);
            if (isExclusive) {
                ExclusiveGateway gateway = getExclusiveGateway(process, edge.getSource());
                if (gateway != null) gateway.setDefaultFlow(edge.getId());
            } else {
                InclusiveGateway gateway = getInclusiveGateway(process, edge.getSource());
                if (gateway != null) gateway.setDefaultFlow(edge.getId());
            }
        } else {
            processGatewayCondition.setIsDefault(false);
            if (StringUtils.hasText(conditionType) && conditionType.equals(ConditionTypeConstants.NATIVE)) {
                processGatewayCondition.setConditionType(ConditionTypeConstants.NATIVE).setNativeExpression(nativeExpression);
                sequenceFlow.setConditionExpression(nativeExpression);
            } else {
                processGatewayCondition.setConditionType(ConditionTypeConstants.CUSTOM);
                sequenceFlow.setConditionExpression("${gatewayConditionEvaluator.evaluate(execution, '" + edge.getSource() + "', '" + edge.getId() + "')}");
                Object ruleTree = data.get("ruleTree");
                if(Objects.nonNull(ruleTree)){
                    List<ProcessGatewayConditionNode> processGatewayConditionNodes = JSONUtils.parseListByObject(ruleTree, ProcessGatewayConditionNode.class);
                    processGatewayCondition.setRuleTree(processGatewayConditionNodes);
                }
            }
        }
        gatewayConditions.add(processGatewayCondition);
    }

    public String bpmnModelToXml(BpmnModel bpmnModel) {
        BpmnXMLConverter converter = new BpmnXMLConverter();
        byte[] xmlBytes = converter.convertToXML(bpmnModel);
        return new String(xmlBytes, StandardCharsets.UTF_8);
    }

    private void setUserTaskExtensionElements(UserTask userTask, Map<String, Object> data) {
        initExtensionElementFilterNull(userTask, APPROVAL_TYPE.getCode(), String.valueOf(data.get(APPROVAL_TYPE.getCode())), APPROVAL_TYPE.getDescription());
        initExtensionElementFilterNull(userTask, AUTO_APPROVE_IF_SELF.getCode(), String.valueOf(data.get(AUTO_APPROVE_IF_SELF.getCode())), AUTO_APPROVE_IF_SELF.getDescription());
        initExtensionElementFilterNull(userTask, CANDIDATE_USERS.getCode(), String.valueOf(data.get(CANDIDATE_USERS.getCode())), CANDIDATE_USERS.getDescription());
        initExtensionElementFilterNull(userTask, CANDIDATE_ROLES.getCode(), String.valueOf(data.get(CANDIDATE_ROLES.getCode())), CANDIDATE_ROLES.getDescription());
        initExtensionElementFilterNull(userTask, CANDIDATE_GROUPS.getCode(), String.valueOf(data.get(CANDIDATE_GROUPS.getCode())), CANDIDATE_GROUPS.getDescription());
        initExtensionElementFilterNull(userTask, CANDIDATE_DEPTS.getCode(), String.valueOf(data.get(CANDIDATE_DEPTS.getCode())), CANDIDATE_DEPTS.getDescription());
        initExtensionElementFilterNull(userTask, PASS_RATE.getCode(), String.valueOf(data.get(PASS_RATE.getCode())), PASS_RATE.getDescription());
        initExtensionElementFilterNull(userTask, RANDOM_COUNT.getCode(), String.valueOf(data.get(RANDOM_COUNT.getCode())), RANDOM_COUNT.getDescription());
        initExtensionElementFilterNull(userTask, ACTION_BUTTONS.getCode(), String.valueOf(data.get(ACTION_BUTTONS.getCode())), ACTION_BUTTONS.getDescription());
        String backType = String.valueOf(data.get(BACK_TYPE.getCode()));
        initExtensionElementFilterNull(userTask, BACK_TYPE.getCode(), backType, BACK_TYPE.getDescription());
        initExtensionElementFilterNull(userTask, BACK_NODE_ID.getCode(), String.valueOf(data.get(BACK_NODE_ID.getCode())), BACK_NODE_ID.getDescription());
        String backAssigneePolicyEnum = String.valueOf(data.get(BACK_ASSIGNEE_POLICY.getCode()));
        if(StringUtils.hasText(backType) && !StringUtils.hasText(backAssigneePolicyEnum)){
            backAssigneePolicyEnum = BackAssigneePolicyEnum.AUTO.getCode();
        }
        initExtensionElementFilterNull(userTask, BACK_ASSIGNEE_POLICY.getCode(), backAssigneePolicyEnum, BACK_ASSIGNEE_POLICY.getDescription());
    }

    private void setServiceTaskExtensionElements(ServiceTask serviceTask, Map<String, Object> data) {
        // 1. 设置 flowable:delegateExpression="${delegateDemoService}"
        String delegateExpression = (String) data.get(ServiceTaskConstants.DELEGATE_EXPRESSION);
        Boolean async = (Boolean) data.get(ServiceTaskConstants.ASYNC);
        if(StringUtils.hasText(delegateExpression)){
            serviceTask.setImplementationType(ServiceTaskConstants.DELEGATE_EXPRESSION);
            serviceTask.setImplementation("${"+delegateExpression+"}"); // 这里填写 Spring Bean 的名称
        }
        if(async != null && async){
            serviceTask.setAsynchronous(true);
        }

    }

    private void initExtensionElementFilterNull(UserTask userTask, String name, String value, String desc) {
        if (StringUtils.hasText(value)) {
            ExtensionElement extensionElement = new ExtensionElement();
            extensionElement.setNamespacePrefix("flowable");
            extensionElement.setNamespace("http://flowable.org/bpmn");
            extensionElement.setName(name);
            extensionElement.setElementText(value);
            extensionElement.addAttribute(new ExtensionAttribute("desc", desc));
            userTask.addExtensionElement(extensionElement);
        }
    }

    private ExclusiveGateway getExclusiveGateway(Process process, String id) {
        FlowElement flowElement = process.getFlowElement(id);
        return (flowElement instanceof ExclusiveGateway) ? (ExclusiveGateway) flowElement : null;
    }

    private InclusiveGateway getInclusiveGateway(Process process, String id) {
        FlowElement flowElement = process.getFlowElement(id);
        return (flowElement instanceof InclusiveGateway) ? (InclusiveGateway) flowElement : null;
    }

    private ProcessDefinitionForm getProcessDefinitionForm(String nodeId, ProcessDefinitionForm global, Map<String, Object> data){
        ProcessDefinitionForm processDefinitionForm = new ProcessDefinitionForm();
        processDefinitionForm.setBindType(FormBindType.NODE);
        processDefinitionForm.setNodeId(nodeId);
        Boolean inheritMainForm = (Boolean) data.get("inheritMainForm");
        if(Objects.nonNull(global) && StringUtils.hasText(global.getFormId()) && StringUtils.hasText(global.getFormVersion()) && inheritMainForm != null && inheritMainForm){
            processDefinitionForm.setInheritMainForm("1");
            return processDefinitionForm;
        }

        return null;
    }

    private List<ProcessNodeFieldPermission> getProcessNodeFieldPermissions(String nodeId, Map<String, Object> data){
        List<ProcessNodeFieldPermission> result = new ArrayList<>();
        Object fieldPermissionsObj = data.get("fieldPermissions");
        if(Objects.isNull(fieldPermissionsObj)){
            return result;
        }

        List<ProcessNodeFieldPermission> processNodeFieldPermissions = JSON.parseArray(JSON.toJSONString(fieldPermissionsObj), ProcessNodeFieldPermission.class);


        for (ProcessNodeFieldPermission item : processNodeFieldPermissions) {
            if(FieldPermissionConstants.FieldPermissions.contains(item.getPermission()) && StringUtils.hasText(item.getFieldKey())){
                ProcessNodeFieldPermission processNodeFieldPermission = new ProcessNodeFieldPermission()
                        .setNodeId(nodeId).setFieldKey(item.getFieldKey()).setPermission(item.getPermission());
                result.add(processNodeFieldPermission);
            }
        }
        return result;
    }

    /** 判断任务是否为多实例节点。 */
    public boolean isMultiInstance(Task task) {
        return isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
    }

    /** 按流程定义 ID 与节点 Key 判断是否为多实例节点。 */
    public boolean isMultiInstance(String processDefinitionId, String taskDefinitionKey) {
        BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
        if (model == null) return false;
        if (!(model.getFlowElement(taskDefinitionKey) instanceof UserTask ut)) return false;
        return ut.getLoopCharacteristics() != null;
    }

    /** 从任务变量读取驳回配置。 */
    public BackConfig getBackConfig(String taskId) {
        ApprovalContext approvalContext = getApprovalContext(taskId);

        return getBackConfig(approvalContext);
    }

    public ApprovalContext getApprovalContext(String taskId){
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        Process mainProcess = bpmnModel.getMainProcess();

        // 通过 task.getTaskDefinitionKey() 获取 UserTask
        UserTask userTask = (UserTask) mainProcess.getFlowElement(task.getTaskDefinitionKey());

        // 完美衔接你之前的 ApprovalContext
        return ApprovalContext.from(userTask);
    }

    public BackConfig getBackConfig(ApprovalContext context) {

        String backType = context.backType();
        String backNodeId = context.backNodeId();
        String backAssigneePolicy = context.backAssigneePolicy();

        BackConfig config = new BackConfig();
        config.setAllowBack(backType != null && !backType.isBlank());
        config.setBackType(backType);
        config.setBackNodeId(backNodeId);
        config.setBackAssigneePolicy(backAssigneePolicy != null ? backAssigneePolicy : BackAssigneePolicyEnum.AUTO.getCode());
        return config;
    }


    /** 获取当前任务可回退的历史节点列表（去重）。 */
    public List<BackTargetNode> getAvailableBackTargets(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        List<HistoricActivityInstance> userTasks = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .orderByHistoricActivityInstanceEndTime().desc()
                .list();

        Map<String, BackTargetNode> targets = new LinkedHashMap<>();
        for (HistoricActivityInstance h : userTasks) {
            String nodeId = h.getActivityId();
            if (nodeId.equals(task.getTaskDefinitionKey())) continue;

            targets.putIfAbsent(nodeId, new BackTargetNode()
                    .setNodeId(nodeId)
                    .setNodeName(h.getActivityName()));
        }
        return new ArrayList<>(targets.values());
    }


    public StartEvent getStartEvent(String processKey, String version) {
        // 1. 通过 Key 和 Version 精确查询流程定义
        org.flowable.engine.repository.ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .processDefinitionVersion(Integer.valueOf(version))
                .singleResult();

        if (processDefinition == null) {
            throw new IllegalArgumentException("未找到对应的流程定义, Key: " + processKey + ", Version: " + version);
        }

        // 2. 通过流程定义ID获取 BpmnModel
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        if (bpmnModel == null || bpmnModel.getMainProcess() == null) {
            throw new IllegalStateException("获取流程模型失败或模型中无主流程");
        }

        // 3. 从主流程中获取开始节点
        Process process = bpmnModel.getMainProcess();
        FlowElement startElement = process.getInitialFlowElement();

        // 4. 校验并返回 StartEvent
        if (startElement instanceof StartEvent) {
            return (StartEvent) startElement;
        }

        // 兜底逻辑：如果初始节点不是 StartEvent，尝试在流程元素中遍历查找（防止流程设计不规范）
//        for (FlowElement flowElement : process.getFlowElements()) {
//            if (flowElement instanceof StartEvent) {
//                return (StartEvent) flowElement;
//            }
//        }

        throw new IllegalStateException("流程定义中未找到开始节点 (StartEvent)，请检查流程设计");
    }

}