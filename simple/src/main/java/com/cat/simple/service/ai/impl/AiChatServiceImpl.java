package com.cat.simple.service.ai.impl;

import com.alibaba.fastjson2.JSON;
import com.cat.common.entity.HttpResult;
import com.cat.common.entity.ai.chat.ChatParam;
import com.cat.common.entity.ai.chat.res.ChatResponse;
import com.cat.common.utils.ServletUtils;
import com.cat.common.utils.http.OkHttpUtils;
import com.cat.simple.service.ai.AiChatService;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import okhttp3.*;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;

@Service
public class AiChatServiceImpl implements AiChatService {


    private static final String API_URL = "http://localhost:11434/v1/chat/completions";


    @Override
    public void chat(ChatParam chatParam) throws IOException {

        boolean stream = chatParam.isStream();

        if(!stream){

            String apiResult = OkHttpUtils.getInstance().postJson(API_URL, chatParam);

            ChatResponse chatResponse = JSON.parseObject(apiResult, ChatResponse.class);
            ServletUtils.back(chatResponse);
            return;
        }

        HttpServletResponse httpServletResponse = ServletUtils.getHttpServletResponse();
        httpServletResponse.setContentType("text/event-stream");
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setHeader("Cache-Control", "no-cache");
        httpServletResponse.setHeader("Connection", "keep-alive");
        HttpServletRequest httpServletRequest = ServletUtils.getHttpServletRequest();
        AsyncContext asyncContext = httpServletRequest.startAsync();
        asyncContext.setTimeout(0); // 禁用超时

        OkHttpUtils.getInstance().postJsonStreaming(API_URL, chatParam, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                try {
                    PrintWriter writer = asyncContext.getResponse().getWriter();
                    writer.write("event: error\ndata: " + e.getMessage() + "\n\n");
                    writer.flush();
                } catch (IOException ex) {
                } finally {
                    asyncContext.complete();
                }
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (BufferedSource source = response.body().source();
                     PrintWriter writer = asyncContext.getResponse().getWriter()) {

                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line != null && line.startsWith("data:")) {
                            String jsonData = line.substring(5).trim();
                            if (!"[DONE]".equals(jsonData)) {
                                ChatResponse data = JSON.parseObject(jsonData, ChatResponse.class);
                                writer.write("data: " + JSON.toJSONString(HttpResult.back(data)) + "\n\n");
                                writer.flush();
                            }
                        }
                    }
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } catch (Exception e) {

                } finally {
                    asyncContext.complete();
                }
            }
        });

    }





}
