package com.cat.simple.task;

import com.cat.simple.process.mapper.ProcessDefinitionMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class ProcessTask {

    @Resource private RuntimeService runtimeService;
    @Resource private HistoryService historyService;
    @Resource private RepositoryService repositoryService;
    @Resource private ProcessDefinitionMapper processDefinitionMapper;

    /** 清空数据（保留自增计数，可回滚） */
    @Transactional(rollbackFor = Exception.class)
    public int clearData() {
        resetEngine();
        return deleteAllCustomTables();
    }

    /** 重置模块数据（自增归 1，不可回滚） */
    public void resetData() {
        resetEngine();
        truncateAllCustomTables();
    }

    private int deleteAllCustomTables() {
        int total = 0;
        total += processDefinitionMapper.deleteInstanceForm();
        total += processDefinitionMapper.deleteNodeFieldPermission();
        total += processDefinitionMapper.deleteHandleInfo();
        total += processDefinitionMapper.deleteGatewayConditionNode();
        total += processDefinitionMapper.deleteGatewayCondition();
        total += processDefinitionMapper.deleteDefinitionForm();
        total += processDefinitionMapper.deleteDefinitionBytearray();
        total += processDefinitionMapper.deleteDefinition();
        total += processDefinitionMapper.deleteInstance();
        return total;
    }

    private void truncateAllCustomTables() {
        processDefinitionMapper.truncateInstanceForm();
        processDefinitionMapper.truncateNodeFieldPermission();
        processDefinitionMapper.truncateHandleInfo();
        processDefinitionMapper.truncateGatewayConditionNode();
        processDefinitionMapper.truncateGatewayCondition();
        processDefinitionMapper.truncateDefinitionForm();
        processDefinitionMapper.truncateDefinitionBytearray();
        processDefinitionMapper.truncateDefinition();
        processDefinitionMapper.truncateInstance();
    }

    /** 先清引擎数据：级联删除部署会连带实例和历史 */
    private void resetEngine() {
        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            // cascade=true：连带删除运行实例 + 历史数据
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
        // 兜底：清理可能的孤儿历史（定义已不存在的）
        historyService.createHistoricProcessInstanceQuery().list()
                .forEach(h -> historyService.deleteHistoricProcessInstance(h.getId()));
    }
}

