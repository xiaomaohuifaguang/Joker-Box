package com.cat.simple.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.workOrder.WorkOrder;
import com.cat.common.entity.workOrder.WorkOrderPageParam;

public interface WorkOrderService {

    WorkOrder info(WorkOrder workOrder);

    Page<WorkOrder> queryPage(WorkOrderPageParam pageParam);

    boolean draft(WorkOrder workOrder);

    boolean start(WorkOrder workOrder);
    
    boolean pass(WorkOrder workOrder);

    boolean transfer(WorkOrder workOrder, Integer userId);

    boolean reject(WorkOrder workOrder);

    WorkOrder selectOneByProcessInstanceId(Integer processInstanceId);


}