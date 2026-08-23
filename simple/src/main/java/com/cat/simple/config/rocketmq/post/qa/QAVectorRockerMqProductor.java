package com.cat.simple.config.rocketmq.post.qa;

import com.cat.common.entity.ai.chat.QAMessage;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QAVectorRockerMqProductor {

    @Value("${custom.rocket.qa.topic}")
    private String topic;


    @Resource
    private RocketMQTemplate rocketMQTemplate;


    public void send(QAMessage qaMessage){
        rocketMQTemplate.syncSend(topic, qaMessage);
    }


}
