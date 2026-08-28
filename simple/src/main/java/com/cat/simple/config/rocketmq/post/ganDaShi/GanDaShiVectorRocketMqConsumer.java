package com.cat.simple.config.rocketmq.post.ganDaShi;


import com.cat.common.entity.ganDaShi.GanDaShiPost;
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
        topic = "${rocketmq.custom.ganDaShi.topic}",
        consumerGroup = "${rocketmq.custom.ganDaShi.group}",
        consumeThreadNumber  = 2,
        consumeThreadMax = 2
)
@Slf4j
public class GanDaShiVectorRocketMqConsumer implements RocketMQListener<GanDaShiPost> {

    @Resource
    private OpensearchUtils opensearchUtils;
    @Resource
    private LlmService llmService;

    @Override
    public void onMessage(GanDaShiPost ganDaShiPost) {

        List<Float> vector = llmService.vector(ganDaShiPost.getText());

        ganDaShiPost.setTextEmbeddings(vector);

        boolean b = opensearchUtils.insertOrUpdate(GanDaShiPost.INDEX, String.valueOf(ganDaShiPost.getId()), ganDaShiPost);
        if(b){
            log.info("干大事向量化id:{}成功", ganDaShiPost.getId());
        }else {
            log.error("干大事向量化id:{}失败", ganDaShiPost.getId());
        }
    }

}
