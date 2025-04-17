package com.cat.simple.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.process.ProcessInfo;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.common.entity.process.ProcessInstancePageParam;

import java.time.LocalDateTime;
import java.util.List;

public interface ProcessInstanceService {

    Page<ProcessInstance> queryPage(ProcessInstancePageParam pageParam);

    ProcessInfo info(Integer processInstanceId);

    ProcessInfo handleInfo(Integer processInstanceId);

    ProcessInstance start(Integer processDefinitionId);
    // 处理方式
    boolean pass(Integer processInstanceId);

    boolean transfer(Integer processInstanceId, Integer userId);

    boolean reject(Integer processInstanceId);

    List<String> taskNames(Integer processInstanceId);

    void updateStatus(String flowableProcessInstanceId, String status, LocalDateTime now);

    void saveHandleInfo(Integer processInstanceId,String taskId, String taskName,String handleUser , String handleType, String remark, LocalDateTime updateTime);

    void saveHandleInfo(String flowableProcessInstanceId,String taskId, String taskName,String handleUser , String handleType, String remark, LocalDateTime updateTime);

    ProcessInstance selectOneByFlowableProcessInstanceId(String flowableProcessInstanceId);




}