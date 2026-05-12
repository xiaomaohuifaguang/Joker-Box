package com.cat.simple.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class KafkaConfig {

//    @Bean
//    public NewTopic topic() {
//        return TopicBuilder.name("joker")
//                .partitions(10)
//                .replicas(1)
//                .build();
//    }
//
//    @KafkaListener(id = "myId", topics = "joker")
//    public void listen(String in) {
//        System.out.println("收到消息："+in);
//    }
//    @KafkaListener(id = "myId1", topics = "joker")
//    public void listen1(String in) {
//        System.out.println("收到消息："+in);
//    }
//    @KafkaListener(id = "myId2", topics = "joker")
//    public void listen2(String in) {
//        System.out.println("收到消息："+in);
//    }
//
//    @Bean
//    public ApplicationRunner runner(KafkaTemplate<String, String> template) {
//        return args -> {
//            template.send("joker", "test");
//        };
//    }

}
