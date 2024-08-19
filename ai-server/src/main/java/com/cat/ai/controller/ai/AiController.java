package com.cat.ai.controller.ai;

import com.cat.ai.service.OpenAiService;
import com.cat.common.entity.ai.chat.ChatRequestPram;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/***
 * <TODO description class purpose>
 * @title AiController
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/3 0:02
 **/
@RestController
@RequestMapping("/catai")
@Tag(name = "ai接口")
public class AiController {

    @Resource
    private OpenAiService openAiService;

    @RequestMapping(value = "/chat",method = RequestMethod.POST)
    @Operation(summary = "ai对话")
    public void openAiChat(@RequestBody ChatRequestPram chatRequestPram) throws IOException {
        openAiService.chat(chatRequestPram);
    }



}
