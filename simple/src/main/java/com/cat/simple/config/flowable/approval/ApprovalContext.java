package com.cat.simple.config.flowable.approval;

import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.UserTask;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.cat.simple.config.flowable.constant.ProcessConstants.*;

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
        List<String> actionButtons,
        String backType,
        String backNodeId
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
        ApprovalTypeEnum type = ApprovalTypeEnum.of(readText(map, EL_APPROVAL_TYPE));
        if (type == null) {
            return null;
        }
        return new ApprovalContext(
                type,
                splitCsv(readText(map, EL_CANDIDATE_USERS)),
                splitCsv(readText(map, EL_CANDIDATE_ROLES)),
                splitCsv(readText(map, EL_CANDIDATE_GROUPS)),
                splitCsv(readText(map, EL_CANDIDATE_DEPTS)),
                parseRate(readText(map, EL_PASS_RATE)),
                splitCsv(readText(map, EL_ACTION_BUTTONS)),
                readText(map, EL_BACK_TYPE),
                readText(map, EL_BACK_NODE_ID)
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
}