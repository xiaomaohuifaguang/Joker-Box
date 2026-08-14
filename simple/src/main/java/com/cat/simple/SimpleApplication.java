package com.cat.simple;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.entity.ai.model.ModelType;
import com.cat.simple.ai.service.AiModelService;
import com.cat.simple.ai.service.LlmService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Flux;

import java.util.Objects;

@SpringBootApplication(proxyBeanMethods = false)
@EnableScheduling // 启用Spring的定时任务功能
@EnableAspectJAutoProxy // 启用Spring AOP的自动代理机制
@Slf4j
public class SimpleApplication {


    public static void main(String[] args) {
        SpringApplication.run(SimpleApplication.class, args);

    }


//    @Resource
//    private AiModelService aiModelService;
//    @Resource
//    private LlmService llmService;

//    @PostConstruct
//    private void test() throws GraphRunnerException {
//        AiModel aiModel = aiModelService.defaultByTypeDecryptApiKey(ModelType.CHAT.getCode());
//        OpenAiApi openAiApi = llmService.buildOpenAiApi(aiModel);
//
//        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder().openAiApi(openAiApi)
//                .defaultOptions(OpenAiChatOptions.builder()
//                        .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.TEXT).build())
//                        .temperature(1.0)
//                        .build())
//                .build();
//
//        ReactAgent agent = ReactAgent.builder()
//                .name("test")
//                .model(openAiChatModel)
//                .systemPrompt("You are a helpful assistant")
//                .saver(new MemorySaver())
//                .build();
//
////        AssistantMessage message = agent.call("你好");
////        System.out.println(message);
//
//        Flux<NodeOutput> stream = agent.stream("你好");
//        stream.subscribe(
//                output -> {
//                    // 检查是否为 StreamingOutput 类型
//                    if (output instanceof StreamingOutput streamingOutput) {
//                        OutputType type = streamingOutput.getOutputType();
//
//                        // 处理模型推理的流式输出
//                        if (type == OutputType.AGENT_MODEL_STREAMING) {
//                            // 流式增量内容，逐步显示
//                            System.out.print(streamingOutput.message().getText());
//                        } else if (type == OutputType.AGENT_MODEL_FINISHED) {
//                            // 模型推理完成，可获取完整响应
//                            System.out.println("\n模型输出完成");
//                        }
//
//                        // 处理工具调用完成（目前不支持 STREAMING）
//                        if (type == OutputType.AGENT_TOOL_FINISHED) {
//                            System.out.println("工具调用完成: " + output.node());
//                        }
//
//                        // 对于 Hook 节点，通常只关注完成事件（如果Hook没有有效输出可以忽略）
//                        if (type == OutputType.AGENT_HOOK_FINISHED) {
//                            System.out.println("Hook 执行完成: " + output.node());
//                        }
//                    }
//                },
//                error -> System.err.println("错误: " + error),
//                () -> System.out.println("Agent 执行完成")
//        );
//
//
//    }



}
