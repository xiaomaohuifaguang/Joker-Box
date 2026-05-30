package com.cat.simple.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.DTO;
import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.process.ProcessDefinition;
import com.cat.common.entity.process.ProcessDefinitionBytearray;
import com.cat.common.entity.process.ProcessDefinitionForm;
import com.cat.common.entity.process.ProcessNodeFieldPermission;
import com.cat.common.utils.flowable.FlowableUtils;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.process.mapper.ProcessDefinitionBytearrayMapper;
import com.cat.simple.process.mapper.ProcessDefinitionFormMapper;
import com.cat.simple.process.mapper.ProcessDefinitionMapper;
import com.cat.simple.process.mapper.ProcessNodeFieldPermissionMapper;
import com.cat.simple.system.mapper.UserMapper;
import com.cat.simple.process.service.ProcessDefinitionService;
import jakarta.annotation.Resource;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {

    @Resource
    private ProcessDefinitionMapper processDefinitionMapper;
    @Resource
    private ProcessDefinitionBytearrayMapper processDefinitionBytearrayMapper;
    @Resource
    private ProcessDefinitionFormMapper processDefinitionFormMapper;
    @Resource
    private ProcessNodeFieldPermissionMapper processNodeFieldPermissionMapper;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private com.cat.simple.process.service.ProcessFormService processFormService;


    @Override
    @Transactional
    public boolean add(ProcessDefinition processDefinition) throws ParserConfigurationException, IOException, SAXException {

        String processKey = getValueByTag(processDefinition.getXmlStr(), "bpmn:process", "id");
        String processName = getValueByTag(processDefinition.getXmlStr(), "bpmn:process", "name");
        processDefinition.setProcessKey(processKey);
        processDefinition.setProcessName(processName);

        processDefinition.setVersion("DRAFT");
        processDefinition.setStatus("0");
        processDefinition.setDeleted("0");
        processDefinition.setCreateBy(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
        LocalDateTime now = LocalDateTime.now();
        processDefinition.setCreateTime(now);
        processDefinition.setUpdateTime(now);

        int insert = processDefinitionMapper.insert(processDefinition);

        ProcessDefinitionBytearray bytearray = new ProcessDefinitionBytearray()
                .setProcessDefinitionId(processDefinition.getId())
                .setVersion("DRAFT")
                .setXml(processDefinition.getXmlStr().getBytes())
                .setRawData(processDefinition.getRawData())
                .setCreateBy(processDefinition.getCreateBy())
                .setCreateTime(now);
        int insertByte = processDefinitionBytearrayMapper.insert(bytearray) > 0 ? 1 : 0;

        // 保存 DRAFT 版本的表单绑定与字段权限
        processFormService.saveDraftBindings(processDefinition.getId(),
                processDefinition.getGlobalFormBinding(),
                processDefinition.getNodeFormBindings(),
                processDefinition.getNodeFieldPermissions());

        return insert == 1 && insertByte == 1;
    }

    @Override
    @Transactional
    public boolean save(ProcessDefinition processDefinition) throws ParserConfigurationException, IOException, SAXException {

        ProcessDefinition processDefinitionOri = processDefinitionMapper.selectById(processDefinition.getId());

        if (ObjectUtils.isEmpty(processDefinitionOri) || processDefinitionOri.getStatus().equals("1")) {
            return false;
        }

        String processKey = getValueByTag(processDefinition.getXmlStr(), "bpmn:process", "id");
        if (!processDefinitionOri.getProcessKey().equals(processKey)) {
            String s = modifyProcessId(processDefinition.getXmlStr(), processKey, processDefinitionOri.getProcessKey());
            processDefinition.setXmlStr(s);
        }
        processDefinitionOri.setXmlStr(processDefinition.getXmlStr());
        String processName = getValueByTag(processDefinition.getXmlStr(), "bpmn:process", "name");
        processDefinitionOri.setProcessName(processName);
        processDefinitionOri.setUpdateTime(LocalDateTime.now());
        processDefinitionOri.setProcessCategory(processDefinition.getProcessCategory());
        processDefinitionOri.setProcessDescription(processDefinition.getProcessDescription());
        int update = processDefinitionMapper.updateById(processDefinitionOri);

        // 更新 DRAFT 版本的 bytearray
        ProcessDefinitionBytearray draft = selectBytearray(processDefinitionOri.getId(), "DRAFT");
        if (draft == null) {
            // 停用后首次编辑，可能没有 DRAFT，需要新建
            draft = new ProcessDefinitionBytearray()
                    .setProcessDefinitionId(processDefinitionOri.getId())
                    .setVersion("DRAFT")
                    .setCreateBy(processDefinitionOri.getCreateBy())
                    .setCreateTime(LocalDateTime.now());
        }
        draft.setXml(processDefinitionOri.getXmlStr().getBytes());
        draft.setRawData(processDefinition.getRawData());
        if (draft.getId() == null) {
            processDefinitionBytearrayMapper.insert(draft);
        } else {
            processDefinitionBytearrayMapper.updateById(draft);
        }

        // 保存 DRAFT 版本的表单绑定与字段权限
        processFormService.saveDraftBindings(processDefinitionOri.getId(),
                processDefinition.getGlobalFormBinding(),
                processDefinition.getNodeFormBindings(),
                processDefinition.getNodeFieldPermissions());

        return update == 1;
    }

    @Override
    @Transactional
    public DTO<?> deploy(Integer id) {

        ProcessDefinition processDefinition = processDefinitionMapper.selectById(id);
        if (processDefinition == null || !"0".equals(processDefinition.getStatus()) && !"-1".equals(processDefinition.getStatus())) {
            return DTO.error("流程定义不可发布");
        }

        ProcessDefinitionBytearray draft = selectBytearray(id, "DRAFT");
        if (draft == null) {
            return DTO.error("DRAFT 版本数据不存在");
        }

        processDefinition.setXmlStr(new String(draft.getXml()));
        DTO<?> dto = FlowableUtils.validateBpmnXml(processDefinition.getXmlStr());
        if (!dto.flag) {
            return dto;
        }

        Deployment deployment = repositoryService.createDeployment()
                .addString("cat/process/" + processDefinition.getProcessKey() + ".bpmn20.xml", processDefinition.getXmlStr())
                .name(processDefinition.getProcessName())
                .category(processDefinition.getProcessCategory())
                .key(processDefinition.getProcessKey())
                .deploy();

        org.flowable.engine.repository.ProcessDefinition processDefinitionNew = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinition.getProcessKey())
                .latestVersion().singleResult();

        String newVersion = String.valueOf(processDefinitionNew.getVersion());

        // 复制 DRAFT → 版本号
        processDefinitionBytearrayMapper.copyVersion(id, "DRAFT", newVersion);
        processDefinitionFormMapper.copyVersion(id, "DRAFT", newVersion);
        processNodeFieldPermissionMapper.copyVersion(id, "DRAFT", newVersion);

        // 删除 DRAFT
        processDefinitionBytearrayMapper.deletePhysicsByDefAndVersion(id, "DRAFT");
        processDefinitionFormMapper.deletePhysicsByDefAndVersion(id, "DRAFT");
        processNodeFieldPermissionMapper.deletePhysicsByDefAndVersion(id, "DRAFT");

        processDefinition.setStatus("1");
        processDefinition.setUpdateTime(LocalDateTime.now());
        processDefinition.setVersion(newVersion);
        processDefinitionMapper.updateById(processDefinition);

        return DTO.success();
    }

    @Override
    public boolean delete(ProcessDefinition processDefinition) {
        processDefinition = processDefinitionMapper.selectById(processDefinition.getId());
        if (processDefinition == null) {
            return false;
        }
        // 已发布或已停用的流程不允许删除
        if ("1".equals(processDefinition.getStatus()) || "-1".equals(processDefinition.getStatus())) {
            return false;
        }
        // 曾经部署过的流程（有非DRAFT版本记录）不允许删除，防止运行中实例变成孤儿数据
        long publishedCount = processDefinitionBytearrayMapper.selectCount(
                new LambdaQueryWrapper<ProcessDefinitionBytearray>()
                        .eq(ProcessDefinitionBytearray::getProcessDefinitionId, processDefinition.getId())
                        .ne(ProcessDefinitionBytearray::getVersion, "DRAFT"));
        if (publishedCount > 0) {
            return false;
        }
        // 删除所有版本的 bytearray 和节点配置
        processDefinitionBytearrayMapper.deletePhysicsByDefAndVersion(processDefinition.getId(), null);
        processDefinitionFormMapper.deletePhysicsByDefAndVersion(processDefinition.getId(), null);
        processNodeFieldPermissionMapper.deletePhysicsByDefAndVersion(processDefinition.getId(), null);
        return processDefinitionMapper.deleteById(processDefinition) == 1;
    }

    @Override
    public boolean destroy(ProcessDefinition processDefinition) {
        processDefinition = processDefinitionMapper.selectById(processDefinition.getId());
        // 删除所有版本的 bytearray 和节点配置
        processDefinitionBytearrayMapper.deletePhysicsByDefAndVersion(processDefinition.getId(), null);
        processDefinitionFormMapper.deletePhysicsByDefAndVersion(processDefinition.getId(), null);
        processNodeFieldPermissionMapper.deletePhysicsByDefAndVersion(processDefinition.getId(), null);
        return processDefinitionMapper.deleteById(processDefinition) == 1;
    }

    @Override
    @Transactional
    public boolean stop(ProcessDefinition processDefinition) {
        processDefinition = processDefinitionMapper.selectById(processDefinition.getId());
        if (ObjectUtils.isEmpty(processDefinition) || !"1".equals(processDefinition.getStatus())) {
            return false;
        }

        String latestVersion = processDefinition.getVersion();

        // 删除旧 DRAFT（如果存在）
        processDefinitionBytearrayMapper.deletePhysicsByDefAndVersion(processDefinition.getId(), "DRAFT");
        processDefinitionFormMapper.deletePhysicsByDefAndVersion(processDefinition.getId(), "DRAFT");
        processNodeFieldPermissionMapper.deletePhysicsByDefAndVersion(processDefinition.getId(), "DRAFT");

        // 复制最新版本回 DRAFT
        if (StringUtils.hasText(latestVersion)) {
            processDefinitionBytearrayMapper.copyVersion(processDefinition.getId(), latestVersion, "DRAFT");
            processDefinitionFormMapper.copyVersion(processDefinition.getId(), latestVersion, "DRAFT");
            processNodeFieldPermissionMapper.copyVersion(processDefinition.getId(), latestVersion, "DRAFT");
        }

        processDefinition.setStatus("-1");
        processDefinition.setUpdateTime(LocalDateTime.now());
        return processDefinitionMapper.updateById(processDefinition) == 1;
    }

    @Override
    public ProcessDefinition info(ProcessDefinition processDefinition) {
        return info(processDefinition, null);
    }

    @Override
    public ProcessDefinition info(ProcessDefinition processDefinition, String version) {
        processDefinition = processDefinitionMapper.selectById(processDefinition.getId());
        if (processDefinition == null) {
            return null;
        }

        String queryVersion;
        if (StringUtils.hasText(version)) {
            queryVersion = version;
        } else if ("1".equals(processDefinition.getStatus())) {
            queryVersion = processDefinition.getVersion();
        } else {
            queryVersion = "DRAFT";
        }

        ProcessDefinitionBytearray bytearray = selectBytearray(processDefinition.getId(), queryVersion);
        if (bytearray != null) {
            processDefinition.setXmlStr(new String(bytearray.getXml()));
            processDefinition.setRawData(bytearray.getRawData());
        }

        // 查询全局表单绑定（同版本）
        ProcessDefinitionForm globalBinding = processDefinitionFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessDefinitionForm>()
                        .eq(ProcessDefinitionForm::getProcessDefinitionId, processDefinition.getId())
                        .eq(ProcessDefinitionForm::getVersion, queryVersion)
                        .eq(ProcessDefinitionForm::getBindType, "GLOBAL"));
        processDefinition.setGlobalFormBinding(globalBinding);

        // 查询节点表单绑定（同版本）
        List<ProcessDefinitionForm> nodeBindings = processDefinitionFormMapper.selectList(
                new LambdaQueryWrapper<ProcessDefinitionForm>()
                        .eq(ProcessDefinitionForm::getProcessDefinitionId, processDefinition.getId())
                        .eq(ProcessDefinitionForm::getVersion, queryVersion)
                        .eq(ProcessDefinitionForm::getBindType, "NODE"));
        processDefinition.setNodeFormBindings(nodeBindings);

        // 查询节点字段权限（同版本）
        List<ProcessNodeFieldPermission> fieldPermissions = processNodeFieldPermissionMapper.selectList(
                new LambdaQueryWrapper<ProcessNodeFieldPermission>()
                        .eq(ProcessNodeFieldPermission::getProcessDefinitionId, processDefinition.getId())
                        .eq(ProcessNodeFieldPermission::getVersion, queryVersion));
        processDefinition.setNodeFieldPermissions(fieldPermissions);

        processDefinition.setDeletable(isDeletable(processDefinition));
        return processDefinition;
    }

    @Override
    public Page<ProcessDefinition> queryPage(PageParam pageParam) {
        Page<ProcessDefinition> page = new Page<>(pageParam);
        page = processDefinitionMapper.selectPage(page, pageParam);

        List<ProcessDefinition> records = page.getRecords();
        for (ProcessDefinition record : records) {
            record.setCreateByName(userMapper.selectById(record.getCreateBy()).getNickname());
            record.setDeletable(isDeletable(record));
        }
        return page;
    }

    @Override
    public List<ProcessDefinition> deployList() {
        return processDefinitionMapper
                .selectList(new LambdaQueryWrapper<ProcessDefinition>()
                        .eq(ProcessDefinition::getStatus, "1"));
    }

    @Override
    public List<ProcessDefinitionBytearray> versionList(Integer processDefinitionId) {
        return processDefinitionBytearrayMapper.selectList(
                new LambdaQueryWrapper<ProcessDefinitionBytearray>()
                        .eq(ProcessDefinitionBytearray::getProcessDefinitionId, processDefinitionId)
                        .ne(ProcessDefinitionBytearray::getVersion, "DRAFT")
                        .orderByDesc(ProcessDefinitionBytearray::getCreateTime));
    }

    @Override
    @Transactional
    public boolean rollback(Integer processDefinitionId, String targetVersion) {
        ProcessDefinition processDefinition = processDefinitionMapper.selectById(processDefinitionId);
        if (processDefinition == null) {
            return false;
        }

        // 校验目标版本存在
        ProcessDefinitionBytearray target = selectBytearray(processDefinitionId, targetVersion);
        if (target == null) {
            return false;
        }

        // 删除旧 DRAFT
        processDefinitionBytearrayMapper.deletePhysicsByDefAndVersion(processDefinitionId, "DRAFT");
        processDefinitionFormMapper.deletePhysicsByDefAndVersion(processDefinitionId, "DRAFT");
        processNodeFieldPermissionMapper.deletePhysicsByDefAndVersion(processDefinitionId, "DRAFT");

        // 复制目标版本回 DRAFT
        processDefinitionBytearrayMapper.copyVersion(processDefinitionId, targetVersion, "DRAFT");
        processDefinitionFormMapper.copyVersion(processDefinitionId, targetVersion, "DRAFT");
        processNodeFieldPermissionMapper.copyVersion(processDefinitionId, targetVersion, "DRAFT");

        processDefinition.setStatus("0");
        processDefinition.setUpdateTime(LocalDateTime.now());
        processDefinitionMapper.updateById(processDefinition);

        return true;
    }

    // ========== private helpers ==========

    private ProcessDefinitionBytearray selectBytearray(Integer processDefinitionId, String version) {
        return processDefinitionBytearrayMapper.selectOne(
                new LambdaQueryWrapper<ProcessDefinitionBytearray>()
                        .eq(ProcessDefinitionBytearray::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessDefinitionBytearray::getVersion, version));
    }

    private String getValueByTag(String xmlStr, String tag, String attr) throws ParserConfigurationException, IOException, SAXException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlStr.getBytes());
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        return doc.getElementsByTagName(tag).item(0).getAttributes().getNamedItem(attr).getTextContent();
    }

    private boolean isDeletable(ProcessDefinition processDefinition) {
        if (processDefinition == null) {
            return false;
        }
        // 已发布或已停用的流程不可删除
        if ("1".equals(processDefinition.getStatus()) || "-1".equals(processDefinition.getStatus())) {
            return false;
        }
        // 曾经部署过的流程（有非DRAFT版本记录）不可删除
        long publishedCount = processDefinitionBytearrayMapper.selectCount(
                new LambdaQueryWrapper<ProcessDefinitionBytearray>()
                        .eq(ProcessDefinitionBytearray::getProcessDefinitionId, processDefinition.getId())
                        .ne(ProcessDefinitionBytearray::getVersion, "DRAFT"));
        return publishedCount == 0;
    }

    private String modifyProcessId(String originalXml, String oldProcessId, String newProcessId) {
        return originalXml.replaceAll(oldProcessId, newProcessId);
    }

    @Override
    public ProcessDefinition startInfo(Integer processDefinitionId) {
        ProcessDefinition definition = processDefinitionMapper.selectById(processDefinitionId);
        if (definition == null) {
            return null;
        }

        // 解析 BPMN 获取 startEvent 的 nodeId
        String startNodeId = resolveStartEventNodeId(definition.getId());
        com.cat.common.entity.process.TaskFormVO startForm =
                processFormService.buildStartForm(processDefinitionId, startNodeId);
        definition.setStartForm(startForm);

        return definition;
    }

    /**
     * 从 BPMN XML 中解析 startEvent 节点的 ID。
     * 优先匹配 bpmn:startEvent，其次 startEvent。
     */
    @Override
    public String resolveStartEventNodeId(Integer processDefinitionId) {
        ProcessDefinition processDefinition = processDefinitionMapper.selectById(processDefinitionId);
        if (processDefinition == null) {
            return null;
        }

        String effectiveVersion = "1".equals(processDefinition.getStatus()) && StringUtils.hasText(processDefinition.getVersion())
                ? processDefinition.getVersion()
                : "DRAFT";

        ProcessDefinitionBytearray bytearray = processDefinitionBytearrayMapper.selectOne(
                new LambdaQueryWrapper<ProcessDefinitionBytearray>()
                        .eq(ProcessDefinitionBytearray::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessDefinitionBytearray::getVersion, effectiveVersion)
                        .last("LIMIT 1"));
        if (bytearray == null || bytearray.getXml() == null) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(bytearray.getXml()));

            // 先尝试 bpmn:startEvent
            var startEvents = doc.getElementsByTagName("bpmn:startEvent");
            if (startEvents.getLength() == 0) {
                // 再尝试 startEvent
                startEvents = doc.getElementsByTagName("startEvent");
            }
            if (startEvents.getLength() > 0) {
                var idAttr = startEvents.item(0).getAttributes().getNamedItem("id");
                if (idAttr != null) {
                    return idAttr.getNodeValue();
                }
            }
        } catch (Exception e) {
            // 解析失败则返回 null
        }
        return null;
    }
}
