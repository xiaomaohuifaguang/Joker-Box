package com.cat.simple.config.process.core.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BpmUserTaskRejectHandlerTypeEnum {

    FINISH_PROCESS_INSTANCE("1", "终止流程"),
    RETURN_USER_TASK("2", "驳回到指定任务节点");


    private final String type;
    private final String name;



}
