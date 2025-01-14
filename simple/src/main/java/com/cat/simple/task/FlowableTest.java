package com.cat.simple.task;

import com.cat.simple.service.flowable.FlowableService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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


    @PostConstruct
    public void test() throws IOException {

        String path = "process/holiday-request.bpmn20.xml";
        String content = readFileAsString(path);


        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
//            repositoryService.deleteDeployment(deployment.getId(), true);
            repositoryService.deleteDeployment(deployment.getId());
        }

        String deploymentCategory = "请假申请部署";
        Deployment deployment = repositoryService.createDeployment()
                .category(deploymentCategory)
                .addString("process/holiday-request.bpmn20.xml",content)
                .deploy();


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
//        List<Deployment> list = repositoryService.createDeploymentQuery().list();
//        for (Deployment deployment1 : list) {
//            System.out.println(deployment1);
//        }
////        repositoryService.setProcessDefinitionCategory();
//
        List<ProcessDefinition> allProcessDefinitions1 = flowableService.getAllProcessDefinitions();
        for (ProcessDefinition processDefinition : allProcessDefinitions1) {
//            repositoryService.deleteDeployment(processDefinition.getDeploymentId());
            System.out.println(processDefinition);
        }

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




}
