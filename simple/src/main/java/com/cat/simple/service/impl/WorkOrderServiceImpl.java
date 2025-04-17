package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.process.ProcessInfo;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.common.entity.workOrder.WorkOrder;
import com.cat.common.entity.workOrder.WorkOrderPageParam;
import com.cat.common.utils.UUIDUtils;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.WorkOrderMapper;
import com.cat.simple.service.ProcessInstanceService;
import com.cat.simple.service.WorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {


    @Resource
    private WorkOrderMapper workOrderMapper;

    @Resource
    private ProcessInstanceService processInstanceService;


    @Override
    public WorkOrder info(WorkOrder workOrder){
        workOrder = workOrderMapper.selectDetailById(workOrder.getId());
        if(Objects.nonNull(workOrder.getProcessInstanceId())){
            ProcessInfo processInfo = processInstanceService.handleInfo(workOrder.getProcessInstanceId());
            workOrder.setProcessInfo(processInfo);
        }
        return workOrder;
    }

    @Override
    public Page<WorkOrder> queryPage(WorkOrderPageParam pageParam){
        pageParam.init();
        pageParam.setUserId(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
        Page<WorkOrder> page = new Page<>(pageParam);
        page = workOrderMapper.selectPage(page, pageParam);
        page.getRecords().forEach(workOrder -> {
            List<String> taskNames = new ArrayList<>();
            if(workOrder.getStatus().equals("0")){
                taskNames.add("申请");
            }else if(workOrder.getStatus().equals("1")
                    && ( workOrder.getProcessStatus().equals("10") || workOrder.getProcessStatus().equals("11") || workOrder.getProcessStatus().equals("20") )){
                taskNames.add("结束");
            }else{
                taskNames.addAll(new ArrayList<>(new HashSet<>(processInstanceService.taskNames(workOrder.getProcessInstanceId()))));
            }
            workOrder.setTaskNames(taskNames);
        });
        return page;
    }

    @Override
    public boolean draft(WorkOrder workOrder) {
        boolean exist = false;
        if(!ObjectUtils.isEmpty(workOrder.getId())){
            WorkOrder workOrderWithCurrentUser = getWorkOrderWithCurrentUser(workOrder.getId());
            if(Objects.nonNull(workOrderWithCurrentUser)){
                exist = true;
                workOrderWithCurrentUser.setUpdateTime(LocalDateTime.now());
                workOrderWithCurrentUser.setRemark(workOrder.getRemark());
                workOrder = workOrderWithCurrentUser;
            }
        }
        if(!exist){
            workOrder.setStatus("0");
            workOrder.setProcessInstanceId(null);
            workOrder.setCreateBy(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
            workOrder.setCreateTime(LocalDateTime.now());
            workOrder.setUpdateTime(workOrder.getCreateTime());
            workOrder.setOrderNo(UUIDUtils.orderNo());
        }
        return exist ? workOrderMapper.updateById(workOrder) == 1 : workOrderMapper.insert(workOrder) == 1;
    }

    @Override
    @Transactional
    public boolean start(WorkOrder workOrder) {

        boolean exist = false;
        if(!ObjectUtils.isEmpty(workOrder.getId())){
            WorkOrder workOrderWithCurrentUser = getWorkOrderWithCurrentUser(workOrder.getId());
            if(Objects.nonNull(workOrderWithCurrentUser)){
                exist = true;
                workOrderWithCurrentUser.setUpdateTime(LocalDateTime.now());
                workOrderWithCurrentUser.setRemark(workOrder.getRemark());
                workOrderWithCurrentUser.setStatus("1");
                workOrder = workOrderWithCurrentUser;
            }
        }

        if(!exist){
            workOrder.setStatus("1");
            workOrder.setProcessInstanceId(null);
            workOrder.setCreateBy(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
            workOrder.setCreateTime(LocalDateTime.now());
            workOrder.setUpdateTime(workOrder.getCreateTime());
            workOrder.setOrderNo(UUIDUtils.orderNo());
        }

        ProcessInstance processInstance = processInstanceService.start(workOrder.getProcessDefinitionId());
        workOrder.setProcessInstanceId(processInstance.getId());



        return exist ? workOrderMapper.updateById(workOrder) == 1 : workOrderMapper.insert(workOrder) == 1;
    }

    @Override
    @Transactional
    public boolean pass(WorkOrder workOrder) {

        WorkOrder workOrderWithSubmitById = getWorkOrderWithSubmitById(workOrder.getId());
        if(Objects.isNull(workOrderWithSubmitById)){
            return false;
        }

        boolean pass = processInstanceService.pass(workOrderWithSubmitById.getProcessInstanceId());

        if(pass){
            updateTime(workOrder.getId());
        }

        return pass;
    }

    @Override
    @Transactional
    public boolean transfer(WorkOrder workOrder, Integer userId) {
        WorkOrder workOrderWithSubmitById = getWorkOrderWithSubmitById(workOrder.getId());
        if(Objects.isNull(workOrderWithSubmitById)){
            return false;
        }

        boolean transfer = processInstanceService.transfer(workOrderWithSubmitById.getProcessInstanceId(), userId);
        if(transfer){
            updateTime(workOrder.getId());
        }

        return transfer;
    }

    @Override
    @Transactional
    public boolean reject(WorkOrder workOrder) {
        WorkOrder workOrderWithSubmitById = getWorkOrderWithSubmitById(workOrder.getId());
        if(Objects.isNull(workOrderWithSubmitById)){
            return false;
        }

        boolean reject = processInstanceService.reject(workOrderWithSubmitById.getProcessInstanceId());

        if(reject){
            updateTime(workOrder.getId());
        }

        return reject;
    }

    @Override
    public WorkOrder selectOneByProcessInstanceId(Integer processInstanceId) {
        return workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getProcessInstanceId, processInstanceId));
    }


    private WorkOrder getWorkOrderWithCurrentUser(int id){
        return workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getId, id)
                .eq(WorkOrder::getCreateBy, Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId()));
    }

    
    private boolean exist(Integer id){
        if(Objects.isNull(id)){
            return false;
        }
        return workOrderMapper.exists(new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getId, id));
    }

    private WorkOrder getWorkOrderWithSubmitById(Integer workOrderId){
        if(Objects.isNull(workOrderId)){
            return null;
        }
        return workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getId, workOrderId).eq(WorkOrder::getStatus, "1"));
    }
    
    private void updateTime(Integer workOrderId){
        workOrderMapper.update(new LambdaUpdateWrapper<WorkOrder>().eq(WorkOrder::getId, workOrderId).set(WorkOrder::getUpdateTime, LocalDateTime.now()));
    }
    
    

}