package com.cat.simple.config.flowable.variable;

import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

/**
 * 流程变量存储器，封装 Flowable 运行时变量与任务本地变量的读写操作。
 */
@Component
public class ProcessVariableStore {

    @Resource private RuntimeService runtimeService;
    @Resource private TaskService taskService;

    /** 设置流程实例全局变量。 */
    public void set(String processInstanceId, VariableNames name, Object value) {
        runtimeService.setVariable(processInstanceId, name.getKey(), value);
    }

    /** 读取流程实例全局变量。 */
    @SuppressWarnings("unchecked")
    public <T> T get(String processInstanceId, VariableNames name, Class<T> type) {
        Object value = runtimeService.getVariable(processInstanceId, name.getKey());
        return value != null && type.isInstance(value) ? (T) value : null;
    }

    /** 删除流程实例全局变量。 */
    public void remove(String processInstanceId, VariableNames name) {
        runtimeService.removeVariable(processInstanceId, name.getKey());
    }

    /** 设置任务本地变量。 */
    public void setLocal(String taskId, VariableNames name, Object value) {
        taskService.setVariableLocal(taskId, name.getKey(), value);
    }

    /** 读取任务本地变量。 */
    @SuppressWarnings("unchecked")
    public <T> T getLocal(String taskId, VariableNames name, Class<T> type) {
        Object value = taskService.getVariableLocal(taskId, name.getKey());
        return value != null && type.isInstance(value) ? (T) value : null;
    }

    public void setLocal(Task task, VariableNames name, Object value) {
        setLocal(task.getId(), name, value);
    }

    public void setLocal(org.flowable.task.service.delegate.DelegateTask delegateTask, VariableNames name, Object value) {
        setLocal(delegateTask.getId(), name, value);
    }

    public <T> T getLocal(Task task, VariableNames name, Class<T> type) {
        return getLocal(task.getId(), name, type);
    }

    /** 按原始 key 设置流程实例变量。 */
    public void setRaw(String processInstanceId, String key, Object value) {
        runtimeService.setVariable(processInstanceId, key, value);
    }

    /** 按原始 key 删除流程实例变量。 */
    public void removeRaw(String processInstanceId, String key) {
        runtimeService.removeVariable(processInstanceId, key);
    }
}
