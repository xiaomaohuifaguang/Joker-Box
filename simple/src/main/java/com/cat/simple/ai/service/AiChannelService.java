package com.cat.simple.ai.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.channel.AiChannel;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.entity.ai.model.ModelType;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.util.List;

public interface AiChannelService {

    boolean add(AiChannel aiChannel);

    boolean delete(AiChannel aiChannel);

    boolean update(AiChannel aiChannel);

    AiChannel info(AiChannel aiChannel);

    Page<AiChannel> queryPage(PageParam pageParam);

    boolean test(AiChannel aiChannel) throws IOException, ParseException;

    List<AiModel> models(AiChannel aiChannel) throws IOException, ParseException;

    List<AiChannel> listByType(ModelType modelType);

}