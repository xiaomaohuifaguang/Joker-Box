package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.DTO;
import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.process.ProcessDefinition;
import com.cat.common.entity.process.ProcessDefinitionBytearray;
import com.cat.common.utils.UUIDUtils;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.ProcessDefinitionBytearrayMapper;
import com.cat.simple.mapper.ProcessDefinitionMapper;
import com.cat.simple.mapper.UserMapper;
import com.cat.simple.service.ProcessDefinitionService;
import jakarta.annotation.Resource;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {


    @Resource
    private ProcessDefinitionMapper processDefinitionMapper;
    @Resource
    private ProcessDefinitionBytearrayMapper processDefinitionBytearrayMapper;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private IdentityService identityService;




    @Override
    @Transactional
    public boolean add(ProcessDefinition processDefinition) throws ParserConfigurationException, IOException, SAXException {

        String processKey = getValueByTag(processDefinition.getXmlStr(), "bpmn2:process", "id");
        String processName = getValueByTag(processDefinition.getXmlStr(), "bpmn2:process", "name");
        processDefinition.setProcessKey(processKey);
        processDefinition.setProcessName(processName);
        processDefinition.setProcessDescription(getDocumentation(processDefinition.getXmlStr(), processDefinition.getProcessKey()));

//        Deployment deployment = repositoryService.createDeployment()
//                .addString("cat/process/"+ UUIDUtils.randomUUID()+".bpmn20.xml",processDefinition.getXmlStr())
//                .deploy();

//        org.flowable.engine.repository.ProcessDefinition lastProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinition.getProcessKey()).latestVersion().singleResult();
//        processDefinition.setVersion(String.valueOf(lastProcessDefinition.getVersion()));


        processDefinition.setVersion("0");
        processDefinition.setStatus("0");
        processDefinition.setCreateBy(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
        processDefinition.setCreateTime(LocalDateTime.now());
        processDefinition.setUpdateTime(LocalDateTime.now());

        int insert = processDefinitionMapper.insert(processDefinition);

        int insertByte = processDefinitionBytearrayMapper.insert(new ProcessDefinitionBytearray(processDefinition.getId(), processDefinition.getXmlStr().getBytes()));

        return insert == 1 && insertByte == 1;
    }

    @Override
    @Transactional
    public boolean save(ProcessDefinition processDefinition) throws ParserConfigurationException, IOException, SAXException {

        ProcessDefinition processDefinitionOri = processDefinitionMapper.selectById(processDefinition.getId());

        if(ObjectUtils.isEmpty(processDefinitionOri) || processDefinitionOri.getStatus().equals("1")) {
            return false;
        }

        String processKey = getValueByTag(processDefinition.getXmlStr(), "bpmn2:process", "id");
        if(!processDefinitionOri.getProcessKey().equals(processKey)) {
            String s = modifyProcessId(processDefinition.getXmlStr(),processKey , processDefinitionOri.getProcessKey());
            processDefinition.setXmlStr(s);
        }
        processDefinitionOri.setXmlStr(processDefinition.getXmlStr());
        String processName = getValueByTag(processDefinition.getXmlStr(), "bpmn2:process", "name");
        processDefinitionOri.setProcessName(processName);
        String documentation = getDocumentation(processDefinition.getXmlStr(), processDefinition.getProcessKey());
        processDefinitionOri.setProcessDescription(documentation);
        processDefinitionOri.setUpdateTime(LocalDateTime.now());
        int update = processDefinitionMapper.updateById(processDefinitionOri);
        int updateByte = processDefinitionBytearrayMapper.updateById(new ProcessDefinitionBytearray(processDefinitionOri.getId(), processDefinitionOri.getXmlStr().getBytes()));


        return update == 1 && updateByte == 1;
    }

    @Override
    @Transactional
    public DTO<?> deploy(Integer id) {

        ProcessDefinition processDefinition = processDefinitionMapper.selectById(id);

        ProcessDefinitionBytearray processDefinitionBytearray = processDefinitionBytearrayMapper.selectById(id);

        processDefinition.setXmlStr(new String(processDefinitionBytearray.getXml()));

        DTO<?> dto = validateBpmnXml(processDefinition.getXmlStr());
        if(!dto.flag){
            return dto;
        }

        Deployment deployment = repositoryService.createDeployment()
                .addString("cat/process/"+ UUIDUtils.randomUUID()+".bpmn20.xml",processDefinition.getXmlStr())
                .deploy();
        org.flowable.engine.repository.ProcessDefinition lastProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinition.getProcessKey()).latestVersion().singleResult();
        processDefinition.setVersion(String.valueOf(lastProcessDefinition.getVersion()));

        processDefinition.setStatus("1");
        processDefinition.setUpdateTime(LocalDateTime.now());
        processDefinitionMapper.updateById(processDefinition);


        return DTO.success();
    }

    @Override
    public boolean delete(ProcessDefinition processDefinition){
        processDefinition = processDefinitionMapper.selectById(processDefinition.getId());
        if(processDefinition.getStatus().equals("-1") || processDefinition.getStatus().equals("1")) {
            return false;
        }
        return processDefinitionMapper.deleteById(processDefinition) == 1;
    }

    /**
     * 强制销毁
     * @param processDefinition 流程模板信息
     * @return 结果
     */
    @Override
    public boolean destroy(ProcessDefinition processDefinition) {
        processDefinition = processDefinitionMapper.selectById(processDefinition.getId());

        // 已部署过 会删除所有存入flowable信息 包括 实例 任务
        if(processDefinition.getStatus().equals("-1") || processDefinition.getStatus().equals("1")) {
            Deployment deployment = repositoryService.createDeploymentQuery().processDefinitionKey(processDefinition.getProcessKey()).singleResult();
            repositoryService.deleteDeployment(deployment.getId(), true);
        }

        return processDefinitionMapper.deleteById(processDefinition) == 1;
    }

    @Override
    public boolean stop(ProcessDefinition processDefinition){
        processDefinition = processDefinitionMapper.selectById(processDefinition.getId());
        if(ObjectUtils.isEmpty(processDefinition)) {
            return false;
        }
        processDefinition.setStatus("-1");
        processDefinition.setUpdateTime(LocalDateTime.now());
        return  processDefinitionMapper.updateById(processDefinition) == 1;
    }

    @Override
    public ProcessDefinition info(ProcessDefinition processDefinition){
        processDefinition = processDefinitionMapper.selectById(processDefinition.getId());
        ProcessDefinitionBytearray processDefinitionBytearray = processDefinitionBytearrayMapper.selectById(processDefinition.getId());
        processDefinition.setXmlStr(new String(processDefinitionBytearray.getXml()));
        return processDefinition;
    }

    @Override
    public Page<ProcessDefinition> queryPage(PageParam pageParam){
        Page<ProcessDefinition> page = new Page<>(pageParam);
        page = processDefinitionMapper.selectPage(page);

        List<ProcessDefinition> records = page.getRecords();
        for (ProcessDefinition record : records) {
            record.setCreateByName(userMapper.selectById(record.getCreateBy()).getNickname());
        }
        return page;
    }

    @Override
    public List<ProcessDefinition> deployList() {

        return processDefinitionMapper
                .selectList(new LambdaQueryWrapper<ProcessDefinition>()
                        .eq(ProcessDefinition::getStatus,"1"));
    }



//    @Override
//    public String test(String processId) {
//        String currentUserId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();
//        identityService.setAuthenticatedUserId(currentUserId);
//        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processId);
//        identityService.setAuthenticatedUserId(null);
//        String startUserId = processInstance.getStartUserId();
//        System.out.println(startUserId);
//        return processInstance.getId();
//    }
//
//    @Override
//    public boolean testGo(String processInstanceId) {
//        List<Task> list = taskService.createTaskQuery().active().processInstanceId(processInstanceId).list();
//
//        if(!list.isEmpty()){
//            taskService.complete(list.get(0).getId());
//
//        }
//
//        return false;
//    }
//
//    @Override
//    public boolean testBack(String processInstanceId) {
//
//        List<Task> taskList = taskService.createTaskQuery().active().processInstanceId(processInstanceId).list();
//
//
//        if(!taskList.isEmpty()){
//            Task task = taskList.get(0);
//            String processDefinitionId = task.getProcessDefinitionId();
//            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
//            Process process = bpmnModel.getMainProcess();
//
//            String taskDefinitionKey = task.getTaskDefinitionKey();
//            FlowElement flowElement = process.getFlowElement(taskDefinitionKey);
//
//
//            String returnTaskId = null;
//            String userTaskRejectHandlerType = FlowElementUtils.getExtensionElementTextByTagName(flowElement, "rejectHandlerType");
//            assert userTaskRejectHandlerType != null;
//            if (userTaskRejectHandlerType.equals(BpmUserTaskRejectHandlerTypeEnum.RETURN_USER_TASK.getType())) {
//                returnTaskId = FlowElementUtils.getExtensionElementTextByTagName(flowElement, "rejectReturnTaskId");
//
//
//                List<String> runTaskKeyList = taskList.stream().map(Task::getTaskDefinitionKey).toList();
//                List<UserTask> returnUserTaskList = BpmnModelUtils.iteratorFindChildUserTasks(flowElement, runTaskKeyList, null, null);
//
//                List<String> returnTaskKeyList = returnUserTaskList.stream().map(UserTask::getId).toList();
//
//                List<String> runExecutionIds = taskList.stream().map(Task::getExecutionId).toList();
//
//                runtimeService.createChangeActivityStateBuilder()
//                        .processInstanceId(processInstanceId)
//                        .moveExecutionsToSingleActivityId(runExecutionIds, returnTaskId)
//                        .changeState();
//
//            }
//
//
//
//
//
//        }
//
//
//        return false;
//    }


    private String getValueByTag(String xnlStr,String tag, String attr) throws ParserConfigurationException, IOException, SAXException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(xnlStr.getBytes());
        // 解析 XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // 启用命名空间支持
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        // System.out.println(doc.getElementsByTagName("bpmn2:process").item(0).getAttributes().getNamedItem("id").getTextContent());
        // System.out.println(doc.getElementsByTagName("bpmn2:process").item(0).getAttributes().getNamedItem("name").getTextContent());
        return doc.getElementsByTagName(tag).item(0).getAttributes().getNamedItem(attr).getTextContent();
    }



    private String getDocumentation(String xmlStr, String processKey){

        BpmnModel bpmnModel = getBpmnModelByXmlStr(xmlStr);
        // 2. 获取流程定义中的文档描述
        Process process = bpmnModel.getProcessById(processKey);
        return process.getDocumentation();
//        System.out.println("流程描述: " + documentation); // 输出: 流程描述: 描述
    }


    private DTO<?> validateBpmnXml(String bpmnXml) {
        BpmnXMLConverter converter = new BpmnXMLConverter();

        // 尝试将XML转换为BpmnModel
        BpmnModel model = getBpmnModelByXmlStr(bpmnXml);
        // 可选：执行额外自定义校验
        return performCustomValidation(model);
    }

    private DTO<?> performCustomValidation(BpmnModel model) {
        // 自定义校验逻辑，例如检查必要节点或业务规则
        // 示例：检查是否存在至少一个开始事件
        if (model.getMainProcess().getFlowElements().stream()
                .noneMatch(e -> e instanceof StartEvent)) {
//            throw new IllegalArgumentException("流程必须包含至少一个开始事件");
            return DTO.error("流程必须包含至少一个开始事件");
        }

        if (model.getMainProcess().getFlowElements().stream()
                .noneMatch(e -> e instanceof EndEvent)) {
            return DTO.error("流程必须包含至少一个结束事件");
        }

        return DTO.success();
    }

    private BpmnModel getBpmnModelByXmlStr(String xmlStr){
        // 将 XML 字符串转换为 InputStreamProvider
        InputStreamProvider provider = new InputStreamProvider() {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8)); // 指定编码
            }
        };

        // 调用转换方法
        BpmnXMLConverter converter = new BpmnXMLConverter();

        return converter.convertToBpmnModel(provider, true, true);
    }

    /**
     * 修改BPMN XML中的流程ID
     * @param originalXml 原始XML内容
     * @param newProcessId 新的流程ID
     * @return 修改后的XML
     */
    private String modifyProcessId(String originalXml,String oldProcessId ,String newProcessId) {
        return originalXml.replaceAll(oldProcessId, newProcessId);
//        // 1. 将XML解析为BpmnModel
//        BpmnModel model = getBpmnModelByXmlStr(originalXml);
//
//        // 2. 修改流程ID
//        model.getMainProcess().setId(newProcessId);
//
//        // 3. 生成新XML
//        return generateBpmnXml(model);
    }

//    private String generateBpmnXml(BpmnModel model) {
//        BpmnXMLConverter converter = new BpmnXMLConverter();
//        byte[] xmlBytes = converter.convertToXML(model);
//        return new String(xmlBytes);
//    }


}