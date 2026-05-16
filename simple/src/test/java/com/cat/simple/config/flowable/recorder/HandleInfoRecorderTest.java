package com.cat.simple.config.flowable.recorder;

import com.cat.common.entity.process.ProcessHandleInfo;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.HandleTypeEnum;
import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.mapper.ProcessHandleInfoMapper;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleInfoRecorderTest {

    @Mock private ProcessHandleInfoMapper processHandleInfoMapper;
    @Mock private ProcessGuard guard;
    @InjectMocks private HandleInfoRecorder recorder;

    @Test
    void testRecordApply() {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(1);

        recorder.recordApply(instance, "user-1");

        ArgumentCaptor<ProcessHandleInfo> captor = ArgumentCaptor.forClass(ProcessHandleInfo.class);
        verify(processHandleInfoMapper).insert(captor.capture());
        ProcessHandleInfo info = captor.getValue();
        assertEquals(1, info.getProcessInstanceId());
        assertEquals(HandleTypeEnum.APPLY.getCode(), info.getHandleType());
        assertEquals("user-1", info.getHandleUser());
        assertEquals(1, info.getRound());
    }

    @Test
    void testRecordPass() {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(1);
        when(guard.getInstance(1)).thenReturn(instance);
        when(processHandleInfoMapper.selectMaxRound(1, "task-def-1")).thenReturn(2);

        ProcessHandleParam param = new ProcessHandleParam();
        param.setProcessInstanceId(1);
        param.setRemark("同意");

        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-1");
        when(task.getName()).thenReturn("审批节点");
        when(task.getTaskDefinitionKey()).thenReturn("task-def-1");

        recorder.recordPass(param, task);

        ArgumentCaptor<ProcessHandleInfo> captor = ArgumentCaptor.forClass(ProcessHandleInfo.class);
        verify(processHandleInfoMapper).insert(captor.capture());
        ProcessHandleInfo info = captor.getValue();
        assertEquals("task-1", info.getTaskId());
        assertEquals("同意", info.getRemark());
        assertEquals(2, info.getRound());
        assertEquals(HandleTypeEnum.PASS.getCode(), info.getHandleType());
    }

    @Test
    void testRecordBack() {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(1);
        when(guard.getInstance(1)).thenReturn(instance);
        when(processHandleInfoMapper.selectMaxRound(1, "target-node")).thenReturn(3);

        ProcessHandleParam param = new ProcessHandleParam();
        param.setProcessInstanceId(1);

        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-1");
        when(task.getName()).thenReturn("审批节点");

        recorder.recordBack(param, task, "target-node", "目标节点");

        ArgumentCaptor<ProcessHandleInfo> captor = ArgumentCaptor.forClass(ProcessHandleInfo.class);
        verify(processHandleInfoMapper).insert(captor.capture());
        ProcessHandleInfo info = captor.getValue();
        assertEquals(4, info.getRound());
        assertEquals("target-node", info.getTaskDefinitionKey());
        assertTrue(info.getExtra().contains("targetNodeId"));
        assertTrue(info.getExtra().contains("目标节点"));
    }
}
