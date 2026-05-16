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

@ExtendWith(MockitoExtension.class)
class ProcessVariableStoreTest {

    @Mock private RuntimeService runtimeService;
    @Mock private TaskService taskService;
    @InjectMocks private ProcessVariableStore store;

    @Test
    void testSetAndGetProcessVariable() {
        store.set("pid-123", VariableNames.BACK_TYPE, "prev");
        verify(runtimeService).setVariable("pid-123", "backType", "prev");

        when(runtimeService.getVariable("pid-123", "backType")).thenReturn("prev");
        String value = store.get("pid-123", VariableNames.BACK_TYPE, String.class);
        assertEquals("prev", value);
    }

    @Test
    void testGetProcessVariableReturnsNullWhenNotFound() {
        when(runtimeService.getVariable("pid-123", "backType")).thenReturn(null);
        String value = store.get("pid-123", VariableNames.BACK_TYPE, String.class);
        assertNull(value);
    }

    @Test
    void testRemoveProcessVariable() {
        store.remove("pid-123", VariableNames.BACK_TYPE);
        verify(runtimeService).removeVariable("pid-123", "backType");
    }

    @Test
    void testSetAndGetLocalVariable() {
        store.setLocal("task-456", VariableNames.ACTION_BUTTONS, "pass,reject");
        verify(taskService).setVariableLocal("task-456", "actionButtons", "pass,reject");

        when(taskService.getVariableLocal("task-456", "actionButtons")).thenReturn("pass,reject");
        String value = store.getLocal("task-456", VariableNames.ACTION_BUTTONS, String.class);
        assertEquals("pass,reject", value);
    }
}
