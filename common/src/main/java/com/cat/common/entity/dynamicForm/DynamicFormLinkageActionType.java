package com.cat.common.entity.dynamicForm;

/**
 * 动态表单字段联动动作类型
 */
public enum DynamicFormLinkageActionType {
    SHOW,       // 显示
    HIDE,       // 隐藏
    REQUIRED,   // 必填
    DISABLED,   // 禁用
    ENABLED,    // 启用
    SET_PATTERN,// 设置正则
    SET_SPAN,   // 设置宽度
    OPTION,     // 设置选项
    VALUE       // 设置值
}