package com.cat.simple.config.flowable.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.auth.Org;
import com.cat.common.entity.auth.Role;
import com.cat.common.entity.auth.User;
import com.cat.common.entity.dynamicForm.DynamicFormField;
import com.cat.common.entity.dynamicForm.DynamicFormFieldInstance;
import com.cat.common.entity.process.ProcessHandleInfo;
import com.cat.common.entity.process.ProcessInstanceForm;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.form.mapper.DynamicFormFieldInstanceMapper;
import com.cat.simple.form.mapper.DynamicFormFieldMapper;
import com.cat.simple.process.mapper.ProcessHandleInfoMapper;
import com.cat.simple.process.mapper.ProcessInstanceFormMapper;
import com.cat.simple.system.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GatewayEvaluationContext {

    @Resource
    private ProcessHandleInfoMapper processHandleInfoMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ProcessInstanceFormMapper processInstanceFormMapper;
    @Resource
    private DynamicFormFieldInstanceMapper dynamicFormFieldInstanceMapper;
    @Resource
    private DynamicFormFieldMapper dynamicFormFieldMapper;

    private Map<String, Object> formDataCache;
    private List<ProcessHandleInfo> prevHandlersCache;

    public void init(Integer processInstanceId, String nodeId) {
        this.formDataCache = loadFormData(processInstanceId, nodeId);
        this.prevHandlersCache = loadPrevHandlers(processInstanceId, nodeId);
    }

    public Object getValue(String category, String fieldKey) {
        if (category == null) return null;
        return switch (category) {
            case "FORM_FIELD" -> getFormFieldValue(fieldKey);
            case "HANDLER_DEPT" -> getHandlerDept(fieldKey);
            case "HANDLER_ROLE" -> getHandlerRole(fieldKey);
            case "PREV_HANDLER_DEPT" -> getPrevHandlerDept(fieldKey);
            case "PREV_HANDLER_ROLE" -> getPrevHandlerRole(fieldKey);
            default -> null;
        };
    }

    private Object getFormFieldValue(String fieldKey) {
        if (formDataCache == null || fieldKey == null) return null;
        return formDataCache.get(fieldKey);
    }

    private Object getHandlerDept(String fieldKey) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null || user.getOrgs() == null) return null;
        List<String> orgIds = user.getOrgs().stream()
                .map(Org::getId)
                .map(String::valueOf)
                .collect(Collectors.toList());
        return orgIds;
    }

    private Object getHandlerRole(String fieldKey) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null || user.getRoles() == null) return null;
        List<String> roleIds = user.getRoles().stream()
                .map(Role::getId)
                .map(String::valueOf)
                .collect(Collectors.toList());
        return roleIds;
    }

    private Object getPrevHandlerDept(String fieldKey) {
        if (CollectionUtils.isEmpty(prevHandlersCache)) return null;
        Set<String> orgIds = new HashSet<>();
        for (ProcessHandleInfo handler : prevHandlersCache) {
            if (handler.getHandleUser() == null) continue;
            List<Org> orgs = userMapper.selectOrgsByUserId(handler.getHandleUser());
            if (!CollectionUtils.isEmpty(orgs)) {
                for (Org org : orgs) {
                    if (org.getId() != null) orgIds.add(String.valueOf(org.getId()));
                }
            }
        }
        return orgIds.isEmpty() ? null : new ArrayList<>(orgIds);
    }

    private Object getPrevHandlerRole(String fieldKey) {
        if (CollectionUtils.isEmpty(prevHandlersCache)) return null;
        Set<String> roleIds = new HashSet<>();
        for (ProcessHandleInfo handler : prevHandlersCache) {
            if (handler.getHandleUser() == null) continue;
            List<Role> roles = userMapper.selectRolesByUserId(handler.getHandleUser());
            if (!CollectionUtils.isEmpty(roles)) {
                for (Role role : roles) {
                    if (role.getId() != null) roleIds.add(String.valueOf(role.getId()));
                }
            }
        }
        return roleIds.isEmpty() ? null : new ArrayList<>(roleIds);
    }

    private Map<String, Object> loadFormData(Integer processInstanceId, String nodeId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 节点表单实例
        ProcessInstanceForm nodeForm = processInstanceFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstanceForm>()
                        .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                        .eq(ProcessInstanceForm::getNodeId, nodeId));
        if (nodeForm != null) {
            loadFieldValues(nodeForm.getFormInstanceId(), result);
        }

        // 2. 全局表单实例
        ProcessInstanceForm globalForm = processInstanceFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstanceForm>()
                        .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                        .isNull(ProcessInstanceForm::getNodeId));
        if (globalForm != null) {
            loadFieldValues(globalForm.getFormInstanceId(), result);
        }

        return result;
    }

    private void loadFieldValues(String formInstanceId, Map<String, Object> result) {
        if (formInstanceId == null) return;

        List<DynamicFormFieldInstance> instances = dynamicFormFieldInstanceMapper.selectList(
                new LambdaQueryWrapper<DynamicFormFieldInstance>()
                        .eq(DynamicFormFieldInstance::getFormInstanceId, formInstanceId));
        if (CollectionUtils.isEmpty(instances)) return;

        for (DynamicFormFieldInstance instance : instances) {
            if (instance.getVal() != null) {
                DynamicFormField field = dynamicFormFieldMapper.selectById(instance.getFormFieldId());
                if (field != null && field.getFieldId() != null) {
                    result.put(field.getFieldId(), instance.getVal());
                }
            }
        }
    }

    /**
     * 加载上一节点的所有处理记录。
     * 支持并行/会签/或签场景：同一节点可能有多条处理记录。
     */
    private List<ProcessHandleInfo> loadPrevHandlers(Integer processInstanceId, String currentNodeId) {
        List<ProcessHandleInfo> list = processHandleInfoMapper.selectDetailListByProcessInstanceId(processInstanceId);
        if (CollectionUtils.isEmpty(list)) return Collections.emptyList();

        // 按处理时间倒序
        list.sort(Comparator.comparing(ProcessHandleInfo::getHandleTime,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // 跳过当前节点的所有记录
        int i = 0;
        while (i < list.size() && currentNodeId != null
                && currentNodeId.equals(list.get(i).getTaskDefinitionKey())) {
            i++;
        }

        if (i >= list.size()) return Collections.emptyList();

        // 找到上一节点的 nodeId
        String prevNodeId = list.get(i).getTaskDefinitionKey();

        // 收集上一节点的所有处理记录
        List<ProcessHandleInfo> result = new ArrayList<>();
        while (i < list.size() && prevNodeId != null
                && prevNodeId.equals(list.get(i).getTaskDefinitionKey())) {
            result.add(list.get(i));
            i++;
        }

        return result;
    }
}
