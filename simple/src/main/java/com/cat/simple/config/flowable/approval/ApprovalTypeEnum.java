package com.cat.simple.config.flowable.approval;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 审批类型枚举，对应 BPMN 扩展元素 {@code <flowable:approvalType>} 的值。
 */
@AllArgsConstructor
@Getter
public enum ApprovalTypeEnum {

    /** 会签：多实例并行，按 passRate 比例通过 */
    COUNTERSIGN(1, "会签"),
    /** 或签：多实例并行，任一完成即流转 */
    OR_SIGN(2, "或签"),
    /** 随机1人：从候选池随机抽取 1 人指派 */
    RANDOM(3, "随机1人"),
    /** 认领：候选人均可 claim，不预先指派 */
    CLAIM(4, "认领");

    /** 数据库存储值 */
    private final int code;
    /** 前端展示文案 */
    private final String description;

    /**
     * 按数值编码解析枚举。
     *
     * @param code 编码值
     * @return 对应枚举，不存在返回 {@code null}
     */
    public static ApprovalTypeEnum of(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElse(null);
    }

    /**
     * 按字符串编码解析枚举，自动去除空白并转整数。
     *
     * @param code 字符串编码，可为 {@code null}
     * @return 对应枚举，无法解析返回 {@code null}
     */
    public static ApprovalTypeEnum of(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return of(Integer.parseInt(code.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
