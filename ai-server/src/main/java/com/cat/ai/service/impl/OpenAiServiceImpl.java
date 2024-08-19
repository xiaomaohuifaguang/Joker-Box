package com.cat.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.ai.config.security.SecurityUtils;
import com.cat.ai.mapper.AiDialogMapper;
import com.cat.ai.mapper.AiMessageMapper;
import com.cat.ai.service.OpenAiService;
import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.common.entity.ai.chat.*;
import com.cat.common.entity.ai.chat.res.ChatResponse;
import com.cat.common.utils.CatUUID;
import com.cat.common.utils.JSONUtils;
import com.cat.common.utils.ServletUtils;
import com.cat.common.utils.http.HttpUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/***
 * <TODO description class purpose>
 * @title OpenAiServiceImpl
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/2 21:36
 **/
@Service
@Slf4j
public class OpenAiServiceImpl implements OpenAiService {
    private static final String AuthorizationType = "Bearer";
    private static final String OPENAI_PROXY_URL = "https://3c5044638330424dc894875f3d0fe43d.api-forwards.com";
    private static final String API_KEY = "";

    @Resource
    private AiDialogMapper aiDialogMapper;
    @Resource
    private AiMessageMapper aiMessageMapper;


    @Override
    public void chat(ChatRequestPram chatRequestPram) throws IOException {
        if(!StringUtils.hasText(chatRequestPram.getContent())){
            ServletUtils.back(HttpResult.back(HttpResultStatus.ERROR).setMsg("请输入对话内容"));
            return;
        }
        AiDialog aiDialog = aiDialogMapper.selectById(chatRequestPram.getDialogId());
        if(StringUtils.hasText(chatRequestPram.getDialogId()) && ObjectUtils.isEmpty(aiDialog)){
            ServletUtils.back(HttpResult.back(HttpResultStatus.ERROR).setMsg("会话不存在"));
            return;
        }
        if(ObjectUtils.isEmpty(aiDialog)){
            aiDialog = new AiDialog().setId(CatUUID.randomUUID()).setName("").setUserId(SecurityUtils.getUserId()).setName(digest(chatRequestPram.getContent()));
            int insert = aiDialogMapper.insert(aiDialog);
            if(insert != 1){
                ServletUtils.back(HttpResult.back(HttpResultStatus.ERROR).setMsg("创建会话失败"));
                return;
            }
        }
        AiMessage nowAiMessage = new AiMessage().setId(CatUUID.randomUUID()).setDialogId( aiDialog.getId()).setRole(RoleEnum.USER.value()).setContent(chatRequestPram.getContent());
        int insert = aiMessageMapper.insert(nowAiMessage);
        if(insert != 1){
            ServletUtils.back(HttpResult.back(HttpResultStatus.ERROR).setMsg("创建消息失败"));
            return;
        }
        List<AiMessage> aiMessages = aiMessageMapper.selectList(new LambdaQueryWrapper<AiMessage>().eq(AiMessage::getDialogId, aiDialog.getId()));
        ChatParam chatParam = new ChatParam().setModel("gpt-3.5-turbo-0125").setMessages(aiMessages).setStream(chatRequestPram.getStream());

        String requestBody = JSONUtils.toJSONString(chatParam);

        aiMessageMapper.update(new LambdaUpdateWrapper<AiMessage>().set(AiMessage::getOriginal, requestBody).eq(AiMessage::getId, nowAiMessage.getId()));

        String result;
        // 创建一个连接管理器并设置自定义的 SSLConnectionSocketFactory
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(new SSLConnectionSocketFactory((HttpUtils.NotSSLContext())))
                .build();


        try(CloseableHttpClient build = HttpClients.custom().setConnectionManager(connectionManager).build()){
            ClassicHttpRequest httpPost = ClassicRequestBuilder.post(OPENAI_PROXY_URL+"/v1/chat/completions")
                    .build();
            httpPost.setHeader("Authorization",AuthorizationType+" "+API_KEY);
            httpPost.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

            if(chatParam.getStream() != null && chatParam.getStream()){
                HttpServletResponse httpServletResponse = ServletUtils.getHttpServletResponse();
                httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httpServletResponse.setCharacterEncoding(String.valueOf(StandardCharsets.UTF_8));
                PrintWriter writer = httpServletResponse.getWriter();
                AiMessage answer = new AiMessage().setDialogId(aiDialog.getId()).setId(CatUUID.randomUUID());
                result = build.execute(httpPost, classicHttpResponse -> {
                    StringBuilder finalResult = new StringBuilder();
                    HttpEntity entity = classicHttpResponse.getEntity();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info(line);
                        finalResult.append(line).append("\n");
                        if(line.startsWith("data: [DONE]")){
                            writer.write("\n");
                        } else if(line.startsWith("data:")){
                            String tmpLine = line.substring("data: ".length());
                            ChatResponse chatResponse = JSONUtils.parseObject(tmpLine, ChatResponse.class);
                            if(StringUtils.hasText(chatResponse.getChoices().get(0).getFinish_reason()) && chatResponse.getChoices().get(0).getFinish_reason().equals("stop")){
                                writer.write(JSONUtils.toJSONString(HttpResult.back(HttpResultStatus.AI_STREAM_STOP)));
                            }else {
                                if(StringUtils.hasText(chatResponse.getChoices().get(0).getDelta().getRole())){
                                    answer.setRole(chatResponse.getChoices().get(0).getDelta().getRole());
                                }
                                answer.appendContent(chatResponse.getChoices().get(0).getDelta().getContent());
                                writer.write(JSONUtils.toJSONString(HttpResult.back(new AiMessage(chatResponse))) + "\n");
                            }

                        }else {

                            writer.write("\n");
                        }

                        writer.flush();
                    }
                    writer.close();
                    reader.close();
                    return finalResult.toString();
                });
                answer.setOriginal(result);
                int insertAnswer = aiMessageMapper.insert(answer);
                if(insertAnswer != 1){
                    ServletUtils.back(HttpResult.back(HttpResultStatus.ERROR).setMsg("响应保存失败"));
                }
            }else {
                result =  build.execute(httpPost, classicHttpResponse -> {
                    HttpEntity entity = classicHttpResponse.getEntity();
                    return EntityUtils.toString(entity);
                });
                ChatResponse chatResponse = JSONUtils.parseObject(result, ChatResponse.class);
                AiMessage answer = new AiMessage(chatResponse).setDialogId(aiDialog.getId()).setId(CatUUID.randomUUID());
                answer.setOriginal(result);
                int insertAnswer = aiMessageMapper.insert(answer);
                if(insertAnswer != 1){
                    ServletUtils.back(HttpResult.back(HttpResultStatus.ERROR).setMsg("响应保存失败"));
                    return;
                }
                ServletUtils.back(HttpResult.back(answer));
            }

        }

    }





    private String digest(String content){
        return content.length() > 10 ? content.substring(0,10) : content;
    }




}
