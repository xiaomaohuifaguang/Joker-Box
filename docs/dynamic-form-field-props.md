# 动态表单字段 props 规范

## 1. 概述

`FormField` 新增 `props` 字段（`Record<string, any>`），用于存储字段级别的组件配置。不同字段类型的 `props` 内容不同，后端按类型做差异化校验。

```json
{
  "fieldId": "xxx",
  "type": "CASCADER",
  "props": {
    "checkStrictly": false
  }
}
```

### 通用规则

| 规则 | 说明 |
|------|------|
| `props` 可选 | 允许为 `null`、`undefined` 或 `{}`，此时全部走默认值 |
| 未知字段 | 建议忽略并记录警告，不要直接抛错（保证前端扩展时向后兼容） |
| 类型校验 | 若字段存在，必须校验类型；类型不符时强制使用默认值或抛错 |

---

## 2. CASCADER / MULTICASCADER

### 2.1 支持的 props

| 字段 | 类型 | 默认值 | 说明 |
|------|------|-------|------|
| `checkStrictly` | `boolean` | `false` | `false`=只能选叶子节点；`true`=任意层级可选 |

> **emitPath 已废弃**，不再开放配置，固定为 `true`（Element Plus 默认值），即值始终返回**完整路径数组**。

### 2.2 后端校验规则

```java
// 伪代码示例
if (props != null && props.checkStrictly != null) {
    if (!(props.checkStrictly instanceof Boolean)) {
        throw new IllegalArgumentException("checkStrictly 必须是 boolean");
    }
}
```

### 2.3 提交值的结构（前端保证，后端可抽检）

| 字段类型 | 值的结构 | 示例 |
|---------|---------|------|
| `CASCADER` | 一维数组（完整路径） | `["guangdong", "guangzhou", "tianhe"]` |
| `MULTICASCADER` | 二维数组（每组一个完整路径） | `[["guangdong", "guangzhou", "tianhe"], ["beijing", "chaoyang"]]` |

### 2.4 联动规则条件值写法

由于 `emitPath=true` 固定，条件值需写**完整路径**（逗号拼接）：

| 条件 | 示例值 | 含义 |
|------|--------|------|
| `EQ` | `guangdong,guangzhou,tianhe` | 选中"天河区"时触发 |
| `IN` | `guangdong,guangzhou,tianhe,beijing,chaoyang` | 选中"天河区"或"朝阳区"时触发 |
| `EMPTY` | `""` | 未选择时触发 |
| `NOT_EMPTY` | `""` | 已选择时触发 |

---

## 3. 预留扩展（其他字段类型的 props）

以下字段类型未来可能通过 `props` 扩展配置，当前统一默认 `{}`：

| 字段类型 | 未来可能支持的 props |
|---------|-------------------|
| `SELECT` / `MULTISELECT` | `filterable`（可搜索）、`collapseTags`（折叠标签） |
| `NUMBER` | `step`（步长）、`precision`（精度） |
| `RATE` | `allowHalf`（半星）、`showScore`（显示分数） |
| `UPLOAD` | `accept`（文件类型限制） |
| `SLIDER` | `step`（步长）、`showStops`（显示间断点） |

后端校验时，对上述字段的 `props` 可暂时只校验为对象类型，不深入校验内部字段。
