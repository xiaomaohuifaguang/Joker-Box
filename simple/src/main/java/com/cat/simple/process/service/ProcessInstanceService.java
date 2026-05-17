package com.cat.simple.process.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.process.BackConfig;
import com.cat.common.entity.process.BackTargetNode;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.common.entity.process.ProcessInstancePageParam;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;

import java.util.List;



public interface ProcessInstanceService {

    /**
     * 更新流程状态
     * @param flowableProcessInstanceId Flowable对应的实例id
     * @param processStatusEnum 状态
     */
    void updateStatus(String flowableProcessInstanceId, ProcessStatusEnum processStatusEnum);


    /**
     * 启动流程
     * 根据自定义流程定义id启动一个流程实例,
     * 并把申请动作落到 {@link com.cat.common.entity.process.ProcessHandleInfo} 中,
     * 便于后续业务扩展(审批轨迹/转办/抄送等)。
     *
     * @param processDefinitionId 自建表 cat_process_definition 主键
     * @param title 流程标题，可为空
     * @return 自建 {@link ProcessInstance}
     */
    ProcessInstance start(Integer processDefinitionId, String title);


    /**
     * 流程实例分页
     * 入参中的用户id将被强制设置为当前登录人, 避免越权查询其它用户数据。
     *
     * @param pageParam 分页参数
     * @return 分页结果
     */
    Page<ProcessInstance> queryPage(ProcessInstancePageParam pageParam);


    /**
     * 流程实例详情
     *
     * @param id 自建 cat_process_instance 主键
     * @return 实例详情, 不存在返回 {@code null}
     */
    ProcessInstance info(Integer id, String taskId);


    /**
     * 认领任务
     * 验证当前用户是否为指定任务的候选人, 若是则认领并记录到 {@link com.cat.common.entity.process.ProcessHandleInfo}。
     *
     * @param param 流程任务操作参数
     */
    void claim(ProcessHandleParam param);


    /**
     * 审批通过
     * 验证当前用户是否为指定任务的办理人, 若是则完成任务并记录到 {@link com.cat.common.entity.process.ProcessHandleInfo}。
     *
     * @param param 流程任务操作参数
     */
    void pass(ProcessHandleParam param);

    /**
     * 审批拒绝
     * 验证当前用户是否为指定任务的办理人, 若是则终止流程并记录到 {@link com.cat.common.entity.process.ProcessHandleInfo}。
     *
     * @param param 流程任务操作参数
     */
    void reject(ProcessHandleParam param);


    /**
     * 保存草稿
     * 新建或更新草稿流程实例，不启动 Flowable 引擎。
     *
     * @param id                  草稿流程实例id，传则更新，不传则新建
     * @param processDefinitionId 自建流程定义id
     * @param title               流程标题，可为空
     * @return 保存后的流程实例
     */
    ProcessInstance saveDraft(Integer id, Integer processDefinitionId, String title);

    /**
     * 执行驳回
     * @param param 驳回参数
     */
    void back(ProcessHandleParam param);

    /**
     * 查询当前任务可驳回的目标节点列表
     * @param taskId Flowable任务id
     * @return 可供选择的回退目标节点
     */
    List<BackTargetNode> getAvailableBackTargets(String taskId);

    /**
     * 读取指定任务的驳回配置
     * @param taskId Flowable任务id
     * @return 驳回配置
     */
    BackConfig getBackConfig(String taskId);


}