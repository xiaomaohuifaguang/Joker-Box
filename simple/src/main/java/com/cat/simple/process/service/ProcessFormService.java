package com.cat.simple.process.service;

import com.cat.common.entity.process.*;
import org.flowable.task.api.Task;

import java.util.List;
import java.util.Map;

/**
 * 流程表单服务 — 封装表单实例与流程绑定、表单数据读写、任务表单渲染数据组装。
 */
public interface ProcessFormService {

    /**
     * 保存流程定义的 DRAFT 版本表单绑定与字段权限配置。
     * 全量覆盖：null 或空数组表示清空对应配置。
     *
     * @param processDefinitionId   流程定义ID
     * @param globalFormBinding     全局表单绑定，null 表示清空
     * @param nodeFormBindings      节点表单绑定列表，null/空 表示清空
     * @param nodeFieldPermissions  节点字段权限列表，null/空 表示清空
     */
    void saveDraftBindings(Integer processDefinitionId,
                           ProcessDefinitionForm globalFormBinding,
                           List<ProcessDefinitionForm> nodeFormBindings,
                           List<ProcessNodeFieldPermission> nodeFieldPermissions);



    TaskFormVO buildTaskFormByNodeId(Integer processDefinitionId, String processVersion, String nodeId);

    TaskFormVO buildTaskFormByNodeIdWithData(Integer processDefinitionId, String processVersion, Integer processInstanceId, String nodeId);

    void writeFormData(ProcessInstance instance, Map<String, Object> formData);


}