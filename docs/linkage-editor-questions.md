# 联动规则编辑器 — 实现问题确认

## 1. OPTION 动作：级联选项支持

当前 OPTION 动作的选项编辑器仅支持简单选项（label + value），不支持 `children` 嵌套。

| 目标字段类型 | 当前 OPTION 编辑器 | 是否需要支持 children |
|-------------|-------------------|---------------------|
| SELECT / MULTISELECT / RADIO / CHECKBOX | 简单选项 | 否 |
| CASCADER / MULTICASCADER | 简单选项（**不匹配**） | **需要确认** |

**建议**：如果 OPTION 动作需要用于级联字段，选项编辑器应支持嵌套 children 配置（类似字段编辑器中的级联选项）。否则，CASCADER 类型字段不应出现在 OPTION 动作的目标字段列表中。

---

## 2. 不支持 clearable 的字段类型

以下 Element Plus 组件不支持 `clearable` 属性，当前未提供替代清空方式：

| 字段类型 | 组件 | 现状 | 建议方案 |
|---------|------|------|---------|
| RADIO | el-radio-group | 选中后无法取消 | 在组件旁添加"清空"按钮，或保留一个"无"选项 |
| COLOR | el-color-picker | 选中后无法清空 | 在组件旁添加"清空"按钮 |
| NUMBER | el-input-number | 删除内容可清空，但值为 0 时无法区分"未输入"和"输入了 0" | 使用 `controls-position="right"` 配合手动删除 |
| SLIDER | el-slider | 不支持 clearable | 拖动到最小值视为清空，或添加"重置"按钮 |
| RATE | el-rate | 不支持 clearable | 点击同一颗星可清零（Element Plus 默认支持） |

---

## 3. VALUE 动作：值范围约束

当前 VALUE 动作的输入组件未根据目标字段的 `min` / `max` / `maxLength` 等约束做限制。例如：

- NUMBER 字段设置了 `min: 0, max: 100`，VALUE 动作仍可通过 `el-input-number` 输入任意数值
- RATE 字段设置了 `max: 5`，VALUE 动作仍可通过 `el-rate` 选择超过 5 的星数（实际上 el-rate 会根据 max 限制）

**问题**：VALUE 动作的值是否需要遵守目标字段的 min/max/pattern 约束？

---

## 4. 日期/时间 VALUE 的格式

当前 VALUE 动作对 DATE/DATETIME/TIME/DATERANGE 使用 Element Plus 的日期选择器，并设置了 `value-format`。但联动规则保存到后端时，`actionValue` 是组件返回的字符串格式（如 `"2024-01-01"`）。

**问题**：后端是否接受这种字符串格式的日期值？还是需要时间戳或其他格式？
