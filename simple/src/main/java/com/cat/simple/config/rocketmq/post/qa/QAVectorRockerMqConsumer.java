package com.cat.simple.config.rocketmq.post.qa;


import com.cat.common.entity.ai.chat.QAMessage;
import com.cat.simple.ai.service.LlmService;
import com.cat.simple.config.opensearch.OpensearchUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RocketMQMessageListener(
        topic = "${rocketmq.custom.qa.topic}",
        consumerGroup = "${rocketmq.custom.qa.group}",
        consumeThreadNumber  = 2,
        consumeThreadMax = 2
)
@Slf4j
public class QAVectorRockerMqConsumer implements RocketMQListener<QAMessage> {


    @Resource
    private OpensearchUtils opensearchUtils;
    @Resource
    private LlmService llmService;

    @Override
    public void onMessage(QAMessage message) {
        List<Float> questionEmbeddings = llmService.vector(message.getQuestion());
        message.setQuestionEmbeddings(questionEmbeddings);
        List<Float> answerEmbeddings = llmService.vector(message.getAnswer());
        message.setAnswerEmbeddings(answerEmbeddings);

        boolean b = opensearchUtils.insertOrUpdate(QAMessage.INDEX, String.valueOf(message.getId()), message);
        if(b){
            log.info("qa向量化SESSION id:{}成功: {} questionMessageId {} answerMessageId", message.getSessionId(), message.getQuestionMessageId(), message.getAnswerMessageId());
        }else {
            log.info("qa向量化SESSION id:{}失败: {} questionMessageId {} answerMessageId", message.getSessionId(), message.getQuestionMessageId(), message.getAnswerMessageId());
        }
    }
}
