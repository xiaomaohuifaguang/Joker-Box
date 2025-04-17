package com.cat.simple.config.process.core.serviceTask.excute;


import com.cat.common.entity.auth.User;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.common.entity.workOrder.WorkOrder;
import com.cat.simple.service.MailService;
import com.cat.simple.service.ProcessInstanceService;
import com.cat.simple.service.UserService;
import com.cat.simple.service.WorkOrderService;
import freemarker.template.TemplateException;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Objects;

@Component
public class SendEmailToApplyUser implements JavaDelegate {


    @Resource
    private MailService mailService;
    @Resource
    private UserService userService;
    @Resource
    private ProcessInstanceService processInstanceService;
    @Resource
    private WorkOrderService workOrderService;



    @Override
    public void execute(DelegateExecution execution) {
        /**
         * 发送邮件代码
         */
        ProcessInstance processInstance = processInstanceService.selectOneByFlowableProcessInstanceId(execution.getProcessInstanceId());
        User userInfo = userService.getUserInfo(processInstance.getCreateBy());
        WorkOrder workOrder = workOrderService.selectOneByProcessInstanceId(processInstance.getId());
        try {
            if(Objects.nonNull(userInfo.getUserExtend()) && StringUtils.hasText(userInfo.getUserExtend().getMail())){
                mailService.notification(userInfo.getUserExtend().getMail(), userInfo.getNickname(), "你的工单："+workOrder.getOrderNo()+", 已提交成功。");
            }

        } catch (IOException | TemplateException | MessagingException e) {
            throw new RuntimeException(e);
        }


    }
    
}
