package com.cat.simple;

import com.cat.simple.ai.langchain4j.store.DBMemoryStore;
import com.cat.simple.ai.langchain4j.store.RedisMemoryStore;
import com.cat.simple.ai.tools.system.SystemTools;
import com.cat.simple.ai.tools.weather.WeatherTools;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.*;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.util.*;


@Slf4j
@SpringBootTest
//@Transactional
public class LangChain4jTest {

    @Resource
    private WeatherTools weatherTools;
    @Resource
    private SystemTools systemTools;
    @Resource
    private RedisMemoryStore redisMemoryStore;
    @Resource
    private DBMemoryStore dbMemoryStore;


    private static  final ChatModel chatModel = OpenAiChatModel.builder()
            .apiKey("")
            .baseUrl("")
            .modelName("")
            .returnThinking(true)
            .temperature(1D)
            .logRequests(true)
            .logResponses(true)
            .build();

    private static  final  StreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
            .apiKey("")
            .baseUrl("")
            .modelName("")
            .returnThinking(true)
            .temperature(1D)
            .logRequests(true)
            .logResponses(true)
            .build();


    @Test
    public void test(){

//        ChatResponse chatResponse = chatModel.chat(new UserMessage("你好"));
//        System.out.println(chatResponse);

//        ToolSpecification weatherTool = ToolSpecification.builder()
//                .name("get_weather")
//                .description("查询指定城市当前的天气情况，包括天气现象、气温、风力等信息")
//                .parameters(JsonObjectSchema.builder()
//                        .addStringProperty("city", "要查询的城市名称，例如：北京")
//                        .required("city")
//                        .build())
//                .build();
//
//        ToolSpecification local_address = ToolSpecification.builder()
//                .name("local_address")
//                .description("查询用户当前地理位置")
//                .parameters(JsonObjectSchema.builder()
//                        .build())
//                .build();

//        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(weatherTools);
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        for (Method method : weatherTools.getClass().getMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {          // langchain4j 的 @Tool
                ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(method);
                ToolExecutor executor = new DefaultToolExecutor(weatherTools, method);  // ⭐ 核心
                tools.put(spec, executor);
            }
        }

        List<ToolSpecification> specs = new ArrayList<>(tools.keySet());

//        UserMessage userMessage = UserMessage.from("北京今天天气怎么样？");
//        List<ChatMessage> messages = new ArrayList<>(List.of(userMessage));

//
//        while (true) {
//            ChatResponse resp = chatModel.chat(ChatRequest.builder()
//                    .messages(messages)
//                    .toolSpecifications(specs)      // ⭐ 每轮都要带
//                    .build());
//            AiMessage aiMessage = resp.aiMessage();
//            messages.add(aiMessage);
//
//            if (!aiMessage.hasToolExecutionRequests()) {
//                System.out.println(aiMessage.text());   // 最终答案
//                break;
//            }
//            for (ToolExecutionRequest req : aiMessage.toolExecutionRequests()) {
//                ToolExecutor executor = tools.entrySet().stream()
//                        .filter(e -> e.getKey().name().equals(req.name()))
//                        .findFirst().map(Map.Entry::getValue)
//                        .orElseThrow(() -> new RuntimeException("未知工具: " + req.name()));
//
//                String result = executor.execute(req, "default");     // ⭐ 真正打到 getWeather()
//                messages.add(ToolExecutionResultMessage.from(req, result));
//            }
//        }


        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from("北京今天天气怎么样？"));
        streamRound(messages, specs, tools);


    }

    private void streamRound(List<ChatMessage> messages, List<ToolSpecification> specs,  Map<ToolSpecification, ToolExecutor> tools) {
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(specs)      // ⭐ 每轮递归都要重挂，和非流式同坑
                .build();


        streamingChatModel.chat(request, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String token) {      // 打字机效果（纯文本时才触发）
                log.info(token);
            }

            @Override
            public void onPartialToolCall(PartialToolCall partial) {  // OpenAI 会逐 token 流出参数
                // 可选：实时展示"正在调用 getWeather，参数拼到一半..."
            }

            @Override
            public void onCompleteToolCall(CompleteToolCall complete) {
                // 可选：单个工具调用流完（arguments 已拼好）
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                AiMessage aiMessage = completeResponse.aiMessage();
                messages.add(aiMessage);       // ⭐ 必须先入列，下一轮要带上

                if (!aiMessage.hasToolExecutionRequests()) {
                    System.out.println("\n[完成] " + aiMessage.text());
                    return;                    // 没工具调用了 → 递归终止
                }
                for (ToolExecutionRequest req : aiMessage.toolExecutionRequests()) {
                    ToolExecutor executor = tools.entrySet().stream()
                            .filter(e -> e.getKey().name().equals(req.name()))
                            .findFirst().map(Map.Entry::getValue).orElseThrow();
                    String result = executor.execute(req, "default");   // ⭐ 真调用 getWeather()
                    messages.add(ToolExecutionResultMessage.from(req, result));
                }
                streamRound(messages, specs, tools);         // ⭐ 递归发起下一轮流式请求（携带工具结果）
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();       // 任何一轮失败都走这里，递归自然终止
            }
        });
    }



    @Test
    public void test2(){
        InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(MessageWindowChatMemory.builder().id("1").chatMemoryStore(dbMemoryStore).maxMessages(10).build())
                .tools(weatherTools, systemTools).build();

        Result<String> result = assistant.chatResult("北京天气怎么样");

        System.out.println(result);

//        Result<String> result1 = assistant.chatResult("我刚才问你什么问题了");
//
//        System.out.println(result1);

        ArrayList<Content> contents = new ArrayList<>();
//        ImageContent from = ImageContent.from("", "");
//        contents.add(from);

        Assistant assistant2 = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(MessageWindowChatMemory.builder().id("2").chatMemoryStore(dbMemoryStore).maxMessages(10).build())
                .tools(weatherTools, systemTools).build();


        TokenStream tokenStream = assistant2.chatStream("那你再重新回答下", contents);

        tokenStream.onPartialResponse(log::info)          // ① 逐 token 推给前端
                .onToolExecuted(exec -> log.info("工具执行: {}", exec))       // ② 工具执行过程可观测
                .onCompleteResponse(response -> {                            // ③ ⭐ 流式版的 Result
                    // ChatResponse ≈ Result 的元数据部分：
                    String fullText = response.aiMessage().text();           //   完整答案
                    TokenUsage usage  = response.tokenUsage();               //   token 用量（含工具轮次累计）
                    String thinking = response.aiMessage().thinking();
                    FinishReason finishReason = response.metadata().finishReason();//   结束原因
                    log.info(thinking);
                    log.info(fullText);
                    log.info(usage.toString());
                    log.info(finishReason.toString());
                })
                .onPartialThinkingWithContext((partialThinking, ctx) -> {
                    log.info(partialThinking.text());
                    // 也可以先检查状态：
                    if (ctx.streamingHandle().isCancelled()) {
                        return;  // 已经被取消，跳过本段
                    }
                })
                .onError(Throwable::printStackTrace)
                .start();


    }



}
