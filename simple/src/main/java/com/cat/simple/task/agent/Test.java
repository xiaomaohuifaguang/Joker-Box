package com.cat.simple.task.agent;


import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.simple.ai.service.AiModelService;
import com.cat.simple.ai.service.LlmService;
import jakarta.annotation.Resource;
import org.redisson.api.RedissonClient;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Component
public class Test {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private AiModelService aiModelService;
    @Resource
    private LlmService llmService;
    @Resource
    private ToolCallbackProvider weatherToolsProvider;
    @Resource
    private ToolCallbackProvider systemToolsProvider;

    @Resource
    private DataSource dataSource;




//    @PostConstruct
    private void test() throws GraphRunnerException {

        CustomRedisSaver customRedisSaver = CustomRedisSaver.builder().namespace("joker-box").redisson(redissonClient).ttl(30 * 60 , TimeUnit.SECONDS).build();

        CustomMysqlSaver customMysqlSaver = CustomMysqlSaver.builder().dataSource(dataSource).build();


//        AiModel aiModel = aiModelService.defaultByTypeDecryptApiKey(ModelType.CHAT.getCode());
        AiModel aiModel = aiModelService.getOneWithRealApiKeyById("0090a48e076a58e337d3fb3a2dbdb6ca");
        OpenAiChatModel openAiChatModel = llmService.buildOpenAiChatModel(aiModel);

        ChatResponse chatResponse = openAiChatModel.call(Prompt.builder().content("几点了").build());

        List<Generation> results = chatResponse.getResults();

        AssistantMessage output = chatResponse.getResult().getOutput();

        System.out.println(output);


//        ReactAgent agent = ReactAgent.builder()
//                .name("test_agent")
//                .model(openAiChatModel)
//                .tools(weatherToolsProvider.getToolCallbacks())
//                .tools(systemToolsProvider.getToolCallbacks())
//                .systemPrompt("You are a helpful assistant")
//                .saver(customMysqlSaver)
//                .build();
//        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(UUIDUtils.randomUUID()).build();
////        AssistantMessage call = agent.call("几点了 北京天气怎么样", runnableConfig);
//
////        AssistantMessage call = agent.call("几点了");
//
//
//        Flux<NodeOutput> stream = agent.stream("几点了 北京天气怎么样", runnableConfig);
//
//        stream.subscribe(
//                output -> {
//                    // 检查是否为 StreamingOutput 类型
//                    if (output instanceof StreamingOutput streamingOutput) {
//                        OutputType type = streamingOutput.getOutputType();
//
//                        // 流式增量内容，逐步显示
//                        Message message = streamingOutput.message();
//                        if(Objects.nonNull(message)){
//                            Object reasoningContentObj = message.getMetadata().get("reasoningContent");
//                            if(Objects.nonNull(reasoningContentObj) && StringUtils.hasText(reasoningContentObj.toString())){
//                                System.out.print(reasoningContentObj);
//                            }
//                            if(StringUtils.hasText(message.getText())){
//                                System.out.print(message.getText());
//                            }
//                        }
//
//
//                        // 处理模型推理的流式输出
//                        if (type == OutputType.AGENT_MODEL_STREAMING) {
//
//
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
////        System.out.println(call);
//
//        // ✅ 从 saver 中取出该 thread 的完整消息历史
//
//        Collection<Checkpoint> checkpoints = customMysqlSaver.list(runnableConfig);
//
//        for (Checkpoint cp : checkpoints) {
//            System.out.println("-->\t"+cp+"\n\n");
//        }


    }


}
