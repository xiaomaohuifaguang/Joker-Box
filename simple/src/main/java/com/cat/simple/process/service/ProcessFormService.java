package com.cat.simple.process.service;

import com.cat.common.entity.process.ProcessInstanceForm;
import com.cat.common.entity.process.TaskFormVO;

import java.util.Map;

/**
 * 流程表单服务 — 封装表单实例与流程绑定、表单数据读写、任务表单渲染数据组装。
 */
public interface ProcessFormService {

    /**
     * 创建表单实例并初始化字段实例，写入关联表。
     * 如果关联表已存在该节点的记录，则复用已有的表单实例（驳回场景）。
     *
     * @param processInstanceId  流程实例ID
     * @param processDefinitionId 流程定义ID
     * @param nodeId             BPMN节点ID
     * @return 关联记录，无绑定配置时返回 null
     */
    ProcessInstanceForm createFormInstanceIfNeeded(Integer processInstanceId,
                                                   Integer processDefinitionId,
                                                   String nodeId);

    /**
     * 写入表单数据，按字段权限过滤。
     * start/saveDraft 场景：skipRequired=true（不校验必填）。
     * pass 场景：skipRequired=false（校验必填）。
     *
     * @param processInstanceId  流程实例ID
     * @param processDefinitionId 流程定义ID
     * @param nodeId             BPMN节点ID
     * @param formData           前端提交的表单数据 key-value
     * @param skipRequired       是否跳过必填校验
     */
    void writeFormData(Integer processInstanceId, Integer processDefinitionId,
                       String nodeId, Map<String, Object> formData, boolean skipRequired);

    /**
     * 组装任务表单渲染数据（含字段定义、权限、已填值、分组、继承）。
     *
     * @param processInstanceId  流程实例ID
     * @param processDefinitionId 流程定义ID
     * @param nodeId             BPMN节点ID
     * @param editable           当前用户是否可编辑
     * @return 任务表单渲染数据，无绑定配置时返回 null
     */
    TaskFormVO buildTaskForm(Integer processInstanceId, Integer processDefinitionId,
                             String nodeId, boolean editable);

    /**
     * 组装发起流程时的表单模板配置（无已填值）。
     * 供前端点击"新建/申请"时调用，根据 startEvent 节点配置解析表单来源。
     *
     * @param processDefinitionId 流程定义ID
     * @param startNodeId         startEvent 节点ID
     * @return 表单模板配置，无绑定表单时返回 null
     */
    TaskFormVO buildStartForm(Integer processDefinitionId, String startNodeId);
}