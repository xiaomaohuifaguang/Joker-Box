package com.cat.simple.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.channel.AiChannel;
import com.cat.common.entity.ai.provider.AiProvider;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.entity.ai.model.ModelType;
import com.cat.common.exception.JokerBoxException;
import com.cat.common.utils.crypto.AESUtils;
import com.cat.common.utils.http.HttpClientUtils;
import com.cat.simple.ai.mapper.AiChannelMapper;
import com.cat.simple.ai.mapper.AiModelMapper;
import com.cat.simple.ai.mapper.AiProviderMapper;
import com.cat.simple.ai.service.AiChannelService;
import com.cat.simple.ai.service.AiModelService;
import com.cat.simple.config.security.SecurityUtils;
import jakarta.annotation.Resource;
import org.apache.hc.core5.http.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiChannelServiceImpl implements AiChannelService {


    @Resource
    private AiChannelMapper aiChannelMapper;
    @Resource
    private AiProviderMapper aiProviderMapper;
    @Resource
    private AiModelMapper aiModelMapper;
    @Resource
    private AiModelService aiModelService;

    @Value("${custom.aes.key}")
    private String aesKey;

    @Override
    public boolean add(AiChannel aiChannel) {
        String apiKey = aiChannel.getApiKey();
        if(StringUtils.hasText(apiKey)){
            aiChannel.setApiKey(AESUtils.encrypt(apiKey, aesKey));
        }
        int insert = aiChannelMapper.insert(aiChannel);

        List<AiModel> models = aiChannel.getModels();
        models = (models == null ? List.<AiModel>of() : models).stream()
                .filter(m -> StringUtils.hasText(m.getModel()))
                .peek(m -> m.setChannelId(aiChannel.getId()))
                .toList();
        if (!models.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            String userId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();

            for (AiModel aiModel : models) {
                aiModel.setUserId(userId);
                aiModel.setCreateTime(now);
                aiModel.setType(ModelType.CHAT.getCode());
                aiModelMapper.insert(aiModel);
            }

        }

        return insert == 1;
    }

    @Override
    @Transactional
    public boolean delete(AiChannel aiChannel) {

        List<AiModel> aiModels = aiModelMapper.selectList(new LambdaQueryWrapper<AiModel>().eq(AiModel::getChannelId, aiChannel.getId()));
        for (AiModel aiModel : aiModels) {
            aiModelService.delete(aiModel);
        }
        return aiChannelMapper.deleteById(aiChannel) == 1;
    }

    @Override
    @Transactional
    public boolean update(AiChannel aiChannel) {
        String apiKey = aiChannel.getApiKey();
        if(StringUtils.hasText(apiKey)){
            aiChannel.setApiKey(AESUtils.encrypt(apiKey, aesKey));
        }
        int i = aiChannelMapper.updateById(aiChannel);

        Integer channelId = aiChannel.getId();


        List<AiModel> incoming = aiChannel.getModels();

        List<AiModel> dbList = aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>().eq(AiModel::getChannelId, channelId));
        Map<String, AiModel> dbMap = dbList.stream()
                .collect(Collectors.toMap(AiModel::getModel, Function.identity(), (a, b) -> a));

        // 前端传来的列表,同样建索引(防重复 model)
        Map<String, AiModel> inMap = (incoming == null ? List.<AiModel>of() : incoming).stream()
                .filter(m -> StringUtils.hasText(m.getModel()))
                .collect(Collectors.toMap(AiModel::getModel, Function.identity(), (a, b) -> a));

        List<String> deleteIds = dbList.stream()
                .filter(m -> !inMap.containsKey(m.getModel()))
                .map(AiModel::getId)
                .toList();
        if (!deleteIds.isEmpty()) {
            for (String deleteId : deleteIds) {
                aiModelService.delete(new AiModel().setId(deleteId));
            }
        }

        List<AiModel> toInsert = inMap.values().stream()
                .filter(m -> !dbMap.containsKey(m.getModel()))
                .peek(m -> m.setChannelId(channelId))
                .toList();
        if (!toInsert.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            String userId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();

            for (AiModel aiModel : toInsert) {
                aiModel.setUserId(userId);
                aiModel.setCreateTime(now);
                aiModel.setType(ModelType.CHAT.getCode());
                aiModelMapper.insert(aiModel);
            }

        }

        // 修改:两边都有,但 name 变了(或 wantDelete 等其他可编辑字段变了)
        List<AiModel> toUpdate = inMap.values().stream()
                .filter(m -> {
                    AiModel db = dbMap.get(m.getModel());
                    return db != null && ( !Objects.equals(db.getName(), m.getName()) || !Objects.equals(db.getVision(), m.getVision()) );
                })
                .peek(m -> m.setId(dbMap.get(m.getModel()).getId()))  // 用库里主键
                .peek(m -> m.setChannelId(channelId))
                .toList();
        toUpdate.forEach(aiModelMapper::updateById);


        return i == 1;
    }

    @Override
    public AiChannel info(AiChannel param) {
        AiChannel aiChannel = aiChannelMapper.selectById(param.getId());
        if(Objects.isNull(aiChannel)){
            throw  new JokerBoxException("渠道不存在");
        }
        String decrypted = AESUtils.decrypt(aiChannel.getApiKey(), aesKey);
        aiChannel.setApiKey(maskApiKey(decrypted));

        List<AiModel> aiModels = aiModelMapper.selectList(new LambdaQueryWrapper<AiModel>().eq(AiModel::getChannelId, param.getId()));
        aiModels.forEach(m->{
            if (m != null) {
                String decryptedModelApiKey = AESUtils.decrypt(m.getApiKey(), aesKey);
                m.setApiKey(maskApiKey(decryptedModelApiKey));
            }
        });
        aiChannel.setModels(aiModels);

        return aiChannel;
    }

    @Override
    public Page<AiChannel> queryPage(PageParam pageParam) {
        Page<AiChannel> page = new Page<>(pageParam);
        page = aiChannelMapper.selectPage(page, pageParam);
        List<AiChannel> records = page.getRecords();
        if (records != null) {
            records.forEach(record -> {
                String decrypted = AESUtils.decrypt(record.getApiKey(), aesKey);
                record.setApiKey(maskApiKey(decrypted));
            });
        }
        return page;
    }

    @Override
    public boolean test(AiChannel aiChannel) throws IOException, ParseException {
        return Objects.nonNull(getModels(aiChannel));
    }

    @Override
    public List<AiModel> models(AiChannel aiChannel) throws IOException, ParseException {

        List<AiModel> aiModels = new ArrayList<>();
        JSONArray jsonArray = getModels(aiChannel);
        for (int i = 0; i < Objects.requireNonNull(jsonArray).size(); i++) {

            AiModel aiModel = new AiModel();

            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String id = jsonObject.getString("id");
            String name = jsonObject.getString("name");
            name = StringUtils.hasText(name) ? name : jsonObject.getString("display_name");
            name = StringUtils.hasText(name) ? name : id;

            aiModel.setModel(id).setName(name);

            JSONArray modalities_input;

            // DeepSeek
            modalities_input = jsonObject.getJSONArray("input_modalities");


            // Kimi
            JSONObject modalities = jsonObject.getJSONObject("modalities");
            if (Objects.nonNull(modalities) && Objects.nonNull(modalities.getJSONArray("input"))) {
                modalities_input = modalities.getJSONArray("input");
            }

            if (Objects.nonNull(modalities_input)) {
                for (int i1 = 0; i1 < modalities_input.size(); i1++) {
                    String modalities_input_type = modalities_input.getString(i1);
                    if (StringUtils.hasText(modalities_input_type) && modalities_input_type.equals("image")) {
                        aiModel.setVision(true);
                    }
                }
            }


            aiModels.add(aiModel);

        }

        return aiModels;
    }

    @Override
    public List<AiChannel> listByType(ModelType modelType) {

        List<AiChannel> channels = aiChannelMapper.selectList(new LambdaQueryWrapper<>());
        List<AiModel> models = aiModelService.list(modelType);

        // 一分为二:有渠道的进分组,自定义模型单独收
        Map<Boolean, List<AiModel>> partition = models.stream()
                .collect(Collectors.partitioningBy(m -> m.getChannelId() != null));

        // 渠道模型按 channelId 分组挂载
        Map<Integer, List<AiModel>> modelMap = partition.get(true).stream()
                .collect(Collectors.groupingBy(AiModel::getChannelId));

        channels.forEach(ch -> ch.setModels(
                modelMap.getOrDefault(ch.getId(), Collections.emptyList())));

        // 自定义模型挂到虚拟渠道
        List<AiModel> customModels = partition.get(false);
        if (!customModels.isEmpty()) {
            AiChannel virtualChannel = new AiChannel();
            virtualChannel.setId(-1);                    // 哨兵 id,不与真实渠道冲突
            virtualChannel.setName("自定义模型");
            virtualChannel.setModels(customModels);
            channels.add(virtualChannel);
        }

        return channels;

    }


    private JSONArray getModels(AiChannel aiChannel) throws IOException, ParseException {
        HttpClientUtils instance = HttpClientUtils.getInstance();
        String baseUrl = aiChannel.getBaseUrl();
        if (Objects.nonNull(aiChannel.getProviderId())) {
            AiProvider aiProvider = aiProviderMapper.selectById(aiChannel.getProviderId());
            baseUrl = aiProvider.getBaseUrl();
        }


        String str = instance.get(baseUrl + "/models", null, new HashMap<String, String>() {{
            put("Authorization", "Bearer " + aiChannel.getApiKey());
        }});

        JSONObject jsonObject = JSON.parseObject(str);
        JSONObject error = jsonObject.getJSONObject("error");
        JSONArray data = jsonObject.getJSONArray("data");
        if (Objects.nonNull(error) || Objects.isNull(data)) {
            return null;
        }
        return data;
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return apiKey;
        }
        return apiKey.substring(0, 2) + "*".repeat(apiKey.length() - 4) + apiKey.substring(apiKey.length() - 2);
    }


}