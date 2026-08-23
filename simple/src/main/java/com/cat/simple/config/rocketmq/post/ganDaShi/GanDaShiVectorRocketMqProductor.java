package com.cat.simple.config.rocketmq.post.ganDaShi;

import com.cat.common.entity.ganDaShi.GanDaShiPost;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GanDaShiVectorRocketMqProductor {

    @Value("${custom.rocket.ganDaShi.topic}")
    private String topic;

    @Resource
    private RocketMQTemplate rocketMQTemplate;


    public void send(GanDaShiPost ganDaShiPost){
        rocketMQTemplate.syncSend(topic, ganDaShiPost);
    }



}
