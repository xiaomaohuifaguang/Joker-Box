package com.cat.simple.config.flowable.variable;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ProcessVariableStore 单元测试，验证流程变量与任务局部变量的存取删操作。
 */
@ExtendWith(MockitoExtension.class)
class ProcessVariableStoreTest {

    @Mock private RuntimeService runtimeService;
    @Mock private TaskService taskService;
    @InjectMocks private ProcessVariableStore store;

    /**
     * 测试设置并读取流程实例变量。
     */
    @Test
    void testSetAndGetProcessVariable() {
        store.set("pid-123", VariableNames.BACK_TYPE, "prev");
        verify(runtimeService).setVariable("pid-123", "backType", "prev");

        when(runtimeService.getVariable("pid-123", "backType")).thenReturn("prev");
        String value = store.get("pid-123", VariableNames.BACK_TYPE, String.class);
        assertEquals("prev", value);
    }

    /**
     * 测试读取不存在的流程变量时返回 null。
     */
    @Test
    void testGetProcessVariableReturnsNullWhenNotFound() {
        when(runtimeService.getVariable("pid-123", "backType")).thenReturn(null);
        String value = store.get("pid-123", VariableNames.BACK_TYPE, String.class);
        assertNull(value);
    }

    /**
     * 测试删除流程变量。
     */
    @Test
    void testRemoveProcessVariable() {
        store.remove("pid-123", VariableNames.BACK_TYPE);
        verify(runtimeService).removeVariable("pid-123", "backType");
    }

    /**
     * 测试设置并读取任务局部变量。
     */
    @Test
    void testSetAndGetLocalVariable() {
        store.setLocal("task-456", VariableNames.ACTION_BUTTONS, "pass,reject");
        verify(taskService).setVariableLocal("task-456", "actionButtons", "pass,reject");

        when(taskService.getVariableLocal("task-456", "actionButtons")).thenReturn("pass,reject");
        String value = store.getLocal("task-456", VariableNames.ACTION_BUTTONS, String.class);
        assertEquals("pass,reject", value);
    }
}
