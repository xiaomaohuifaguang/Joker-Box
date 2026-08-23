package com.cat.simple.ai.service;


import com.cat.common.entity.ai.chat.ChatMessage;
import com.cat.common.entity.ai.chat.ChatRequestParam;
import com.cat.common.entity.ai.chat.ChatSession;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.entity.file.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AiChatService {

    Object chat(ChatRequestParam chatRequestPram) throws IOException;


    List<ChatSession> sessions();

    List<ChatMessage> messages(String sessionId);

    List<AiModel> chatModels();

    FileInfo fileUpload(MultipartFile file) throws IOException;

    void fileDownload(String fileId) throws IOException;

}
