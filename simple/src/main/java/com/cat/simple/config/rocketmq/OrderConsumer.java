package com.cat.simple.config.rocketmq;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "pro-order-topic",
        consumerGroup = "joker-box-dev-consumer",
        consumeMode = ConsumeMode.ORDERLY
)
public class OrderConsumer implements RocketMQListener<Object> {

    @Override
    public void onMessage(Object message) {
        System.out.println("收到消息: " + message);
        // 处理业务逻辑
        // 抛异常 = 消费失败，会自动重试
    }
}