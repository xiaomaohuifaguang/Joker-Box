package com.cat.simple.config.flowable.approval;

import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.UserTask;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.cat.simple.config.flowable.enums.ExtensionElementEnum.*;

/**
 * 审批上下文，封装从 UserTask 扩展元素 {@code <flowable:*>} 解析出的配置。
 * 作为 record 在解析期创建，运行期只读。
 */
public record ApprovalContext(
        ApprovalTypeEnum type,
        List<String> candidateUsers,
        List<String> candidateRoles,
        List<String> candidateGroups,
        List<String> candidateDepts,
        BigDecimal passRate,
        BigDecimal randomCount,
        List<String> actionButtons,
        String backType,
        String backNodeId,
        String backAssigneePolicy
) {


    /**
     * 从 UserTask 的扩展元素中解析审批上下文。
     *
     * @param userTask BPMN 用户任务节点
     * @return 解析后的上下文；非审批节点返回 {@code null}
     */
    public static ApprovalContext from(UserTask userTask) {
        if (userTask == null) {
            return null;
        }
        Map<String, List<ExtensionElement>> map = userTask.getExtensionElements();
        if (map == null || map.isEmpty()) {
            return null;
        }
        ApprovalTypeEnum type = ApprovalTypeEnum.of(readText(map, APPROVAL_TYPE.getCode()));
        if (type == null) {
            return null;
        }
        return new ApprovalContext(
                type,
                splitCsv(readText(map, CANDIDATE_USERS.getCode())),
                splitCsv(readText(map, CANDIDATE_ROLES.getCode())),
                splitCsv(readText(map, CANDIDATE_GROUPS.getCode())),
                splitCsv(readText(map, CANDIDATE_DEPTS.getCode())),
                parseRate(readText(map, PASS_RATE.getCode())),
                randomCount(readText(map, RANDOM_COUNT.getCode())),
                splitCsv(readText(map, ACTION_BUTTONS.getCode())),
                readText(map, BACK_TYPE.getCode()),
                readText(map, BACK_NODE_ID.getCode()),
                readText(map, BACK_ASSIGNEE_POLICY.getCode())
        );
    }

    private static String readText(Map<String, List<ExtensionElement>> map, String name) {
        List<ExtensionElement> list = map.get(name);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0).getElementText();
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static BigDecimal parseRate(String text) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ONE;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ONE;
        }
    }

    private static BigDecimal randomCount(String text) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ONE;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ONE;
        }
    }
}