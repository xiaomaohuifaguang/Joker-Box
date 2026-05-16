package com.cat.simple.service;

import com.cat.common.entity.process.BackConfig;
import com.cat.common.entity.process.BackTargetNode;
import com.cat.common.entity.process.ProcessBackParam;

import java.util.List;

public interface ProcessBackService {

    /**
     * 执行驳回
     * @param param 驳回参数
     */
    void back(ProcessBackParam param);

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
