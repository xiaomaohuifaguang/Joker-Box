package com.cat.simple.config.flowable.guard;

import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.process.ProcessDefinition;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.ProcessDefinitionMapper;
import com.cat.simple.mapper.ProcessInstanceMapper;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessGuardTest {

    @Mock private ProcessInstanceMapper processInstanceMapper;
    @Mock private ProcessDefinitionMapper processDefinitionMapper;
    @Mock private TaskService taskService;
    @InjectMocks private ProcessGuard guard;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        LoginUser user = new LoginUser();
        user.setUserId("user-1");
        securityUtilsMock.when(SecurityUtils::getLoginUser).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void testGetCurrentUserId() {
        assertEquals("user-1", guard.getCurrentUserId());
    }

    @Test
    void testAssertInstanceActiveReturnsInstance() {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(1);
        instance.setProcessStatus("1");
        when(processInstanceMapper.selectById(1)).thenReturn(instance);

        ProcessInstance result = guard.assertInstanceActive(1);
        assertEquals(1, result.getId());
    }

    @Test
    void testAssertInstanceActiveThrowsWhenNotFound() {
        when(processInstanceMapper.selectById(1)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> guard.assertInstanceActive(1));
    }

    @Test
    void testAssertInstanceActiveThrowsWhenNotActive() {
        ProcessInstance instance = new ProcessInstance();
        instance.setProcessStatus("0");
        when(processInstanceMapper.selectById(1)).thenReturn(instance);
        assertThrows(IllegalStateException.class, () -> guard.assertInstanceActive(1));
    }

    @Test
    void testAssertTaskAssigneeReturnsTask() {
        Task task = mock(Task.class);
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskId("task-1")).thenReturn(query);
        when(query.taskAssignee("user-1")).thenReturn(query);
        when(query.singleResult()).thenReturn(task);

        Task result = guard.assertTaskAssignee("task-1");
        assertSame(task, result);
    }

    @Test
    void testAssertDefinitionPublishedReturnsDefinition() {
        ProcessDefinition def = new ProcessDefinition();
        def.setId(1);
        def.setStatus("1");
        when(processDefinitionMapper.selectById(1)).thenReturn(def);

        ProcessDefinition result = guard.assertDefinitionPublished(1);
        assertEquals(1, result.getId());
    }
}
