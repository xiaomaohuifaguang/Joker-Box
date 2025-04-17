package com.cat.simple.task;

import com.cat.common.utils.UUIDUtils;
import com.cat.simple.service.flowable.FlowableService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class FlowableTest {

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private ProcessEngine processEngine;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    FlowableService flowableService;

    @Resource
    HistoryService historyService;


//    @PostConstruct
    private void run(){


        List<ProcessInstance> list = runtimeService.createProcessInstanceQuery().list();
        for (ProcessInstance instance : list) {
            runtimeService.deleteProcessInstance(instance.getProcessInstanceId(), "我就删除了怎么滴");
        }
        List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().list();
        for (HistoricDetail detail : historicDetails) {
            historyService.deleteHistoricProcessInstance(detail.getProcessInstanceId());
        }

//        //  Process_1741141307858
//        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process_1740824393070");
//
////        runtimeService.setAssignee(processInstance.getProcessInstanceId(), "1234");
//
//        String processDefinitionId = processInstance.getProcessDefinitionId();
//
//        List<Task> tasks = taskService.createTaskQuery().caseInstanceId(processInstance.getId()).active().list();
//        List<Task> tasks1 = taskService.createTaskQuery().caseInstanceId(processInstance.getId()).list();
//
//        for (Task task : tasks) {
////            task.setAssignee("1");
////            taskService.saveTask(task);
//            taskService.complete(task.getId());
////            System.out.println(task);
//        }

    }





//    @PostConstruct
    private void clearAll(){
        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
        deployments.forEach(deployment -> {
            repositoryService.deleteDeployment(deployment.getId(), true);
        });
    }


//    @PostConstruct
    public void test() throws IOException, XMLStreamException, ParserConfigurationException, SAXException {


//        String xml = new String("");

//        String path = "process/holiday-request.bpmn20.xml";
        String path = "process/holiday-request-bpmn.bpmn20.xml";
        String content = readFileAsString(path);

        // 将字符串转换为输入流
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());

        // 解析 XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // 启用命名空间支持
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        System.out.println(doc.getElementsByTagName("bpmn2:process").item(0).getAttributes().getNamedItem("id").getTextContent());
        System.out.println(doc.getElementsByTagName("bpmn2:process").item(0).getAttributes().getNamedItem("name").getTextContent());

        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();

        List<ProcessDefinition> list1 = repositoryService.createProcessDefinitionQuery().list();
        for (ProcessDefinition processDefinition : list1) {
            System.out.println(processDefinition);
        }

        for (Deployment deployment : deployments) {
//            repositoryService.deleteDeployment(deployment.getId(), true);
            // 获取流程定义的 BPMN 文件内容 (字节数组)
            // 获取部署的流程定义
//            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
//                    .deploymentId(deployment.getId())
//                    .singleResult();


//            byte[] bpmnBytes = repositoryService.getResourceAsStream(deployment.getId(), processDefinition.getResourceName()).readAllBytes();
//            // 将字节数组转换成 XML 字符串
//            String bpmnXml = new String(bpmnBytes, StandardCharsets.UTF_8);
//            System.out.println(bpmnXml);

            repositoryService.deleteDeployment(deployment.getId());
        }


//        convert();


        // 创建 BPMN 模型
//        BpmnModel bpmnModel = new BpmnModel();
//        Process process = new Process();
//        process.setId("myProcess");
//        process.setName("My Process");
//        bpmnModel.addProcess(process);


//        String deploymentCategory = "请假申请部署category";
//        Deployment deployment = repositoryService.createDeployment()
////                .category(deploymentCategory)
////                .name("请假申请name")
//                .addString("cat/process/"+ UUIDUtils.randomUUID()+".bpmn20.xml",content)
//                .deploy();



//        List<ProcessDefinition> allProcessDefinitions = flowableService.getAllProcessDefinitions();
//        for (ProcessDefinition processDefinition : allProcessDefinitions) {
//            repositoryService.deleteDeployment(processDefinition.getDeploymentId());
////            System.out.println(processDefinition);
//        }
//
//        RepositoryService repositoryService = processEngine.getRepositoryService();
//        Deployment deployment = repositoryService.createDeployment()
//                .addString("process/holiday-request.bpmn20.xml",content)
//                .addClasspathResource("process/holiday-request.bpmn20.xml")
//                .addClasspathResource("process/money-request.bpmn20.xml")
//                .addString("process/"+ UUID.randomUUID() +".xml",content)
//                .deploy();



//        repositoryService.setDeploymentCategory(deployment.getParentDeploymentId(), "人力资源");
//
        List<Deployment> list = repositoryService.createDeploymentQuery().list();
        for (Deployment deployment1 : list) {
            System.out.println(deployment1);
        }
//        repositoryService.setProcessDefinitionCategory();
//
//        List<ProcessDefinition> allProcessDefinitions1 = flowableService.getAllProcessDefinitions();
//        for (ProcessDefinition processDefinition : allProcessDefinitions1) {
////            repositoryService.deleteDeployment(processDefinition.getDeploymentId());
//            System.out.println(processDefinition.getCategory());
//            System.out.println(processDefinition);
//        }

    }



    public static String readFileAsString(String path) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(path);
        try (InputStream inputStream = classPathResource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }


    public static void convert() throws IOException, XMLStreamException {
//        String path = "process/holiday-request.bpmn20.xml";
        String path = "process/bpmn-js.bpmn";
        String content = readFileAsString(path);
        convertBpmnToFlowableXml(content);
        String path1 = "process/holiday-request.bpmn20.xml";
        String content1 = readFileAsString(path1);
        convertFlowableXmlToBpmn(content1);

    }

    public static void convertBpmnToFlowableXml(String content) throws XMLStreamException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        // 创建 XMLStreamReader
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(byteArrayInputStream);

        // 使用 BpmnXMLConverter 解析
        BpmnXMLConverter converter = new BpmnXMLConverter();
        BpmnModel bpmnModel = converter.convertToBpmnModel(reader);
        // 将转换后的模型转换为 Flowable 格式的 XML
        byte[] flowableXml = converter.convertToXML(bpmnModel);
        // 打印转换后的 XML 内容
        String flowableXmlString = new String(flowableXml, StandardCharsets.UTF_8);
        System.out.println("Flowable BPMN XML:\n" + flowableXmlString);
    }

    public static void convertFlowableXmlToBpmn(String content) throws XMLStreamException {

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        // 创建 XMLStreamReader
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(byteArrayInputStream);

        BpmnXMLConverter converter = new BpmnXMLConverter();

        BpmnModel bpmnModel = converter.convertToBpmnModel(reader);

        // 将其转换为标准 BPMN 2.0 格式的 XML
        byte[] bpmn2Xml = converter.convertToXML(bpmnModel);

        String flowableXmlString = new String(bpmn2Xml, StandardCharsets.UTF_8);

        System.out.println("Flowable XML BPMN:\n" + flowableXmlString);

    }




}
