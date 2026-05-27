# 联动规则编辑器 — 前端实现建议

> 日期：2026-05-21
> 针对 `docs/linkage-editor-questions.md` 逐项回复

---

## 1. OPTION 动作：级联选项支持

### 问题
CASCADER / MULTICASCADER 的选项是树形结构（含 children 嵌套），但当前 OPTION 编辑器只支持简单平铺选项（label + value）。

### 建议方案

**方案A（推荐）：根据目标字段类型动态切换选项编辑器**

| 目标字段类型 | 选项编辑器 | actionValue 格式 |
|-------------|-----------|-----------------|
| SELECT / MULTISELECT / RADIO / CHECKBOX | 简单选项列表 | `[{label, value}, ...]` |
| CASCADER / MULTICASCADER | **树形选项编辑器**（支持 children 嵌套） | `[{label, value, children:[...]}, ...]` |

树形选项编辑器可直接复用字段编辑器里的级联选项组件，或者单独实现一个支持嵌套增删改的简易版。

**方案B（临时）：过滤不支持的目标字段类型**

如果短期内来不及实现树形选项编辑器，在联动规则编辑器中把 `CASCADER` / `MULTICASCADER` 从 `OPTION` 动作的目标字段下拉列表里过滤掉，避免用户误选后配置无效。

```js
// LinkageEditor.vue 中目标字段下拉过滤
const optionTargetTypes = ['SELECT', 'MULTISELECT', 'RADIO', 'CHECKBOX', 'CASCADER', 'MULTICASCADER']
// 临时改为
const optionTargetTypes = ['SELECT', 'MULTISELECT', 'RADIO', 'CHECKBOX']
// 等树形选项编辑器实现后再加回 CASCADER / MULTICASCADER
```

---

## 2. 不支持 clearable 的字段类型

### 问题
部分 Element Plus 组件本身不支持 `clearable`，联动 VALUE 动作设置空值时没有统一的重置方式。

### 建议方案

| 字段类型 | 组件 | 建议实现 |
|---------|------|---------|
| **RADIO** | `el-radio-group` | 在组件旁加一个**清空按钮**（小图标 ×），点击后把 `formData[fieldId]` 设为 `null`。或者更简洁：如果业务允许"不选"，字段本身的 options 里带一个 `{label: '无', value: ''}` |
| **COLOR** | `el-color-picker` | 同样加清空按钮，点击后设为 `null`。`el-color-picker` 本身没有 clearable，但可以通过外部按钮重置 |
| **NUMBER** | `el-input-number` | **改用 `el-input` + `type="number"`**。用户删除内容后值直接是 `null` 或空字符串，不存在 `0` 和 `null` 混淆的问题。如果必须用 `el-input-number`，在 `@blur` 时检测空值并设为 `null` |
| **SLIDER** | `el-slider` | 加**重置按钮**（不是"拖到最小值 = 清空"，因为最小值本身可能是合法业务值，如评分 0 分） |
| **RATE** | `el-rate` | **无需处理**。Element Plus 默认支持点击同一颗星清零 |

### 清空按钮通用实现

```vue
<template>
  <div class="field-with-clear">
    <el-radio-group v-model="formData[fieldId]">
      <el-radio v-for="opt in field.options" :key="opt.value" :label="opt.value">
        {{ opt.label }}
      </el-radio>
    </el-radio-group>
    <el-button
      v-if="formData[fieldId] !== null && formData[fieldId] !== undefined"
      link
      type="primary"
      size="small"
      @click="formData[fieldId] = null"
    >
      清空
    </el-button>
  </div>
</template>
```

---

## 3. VALUE 动作：值范围约束

### 问题
VALUE 动作的输入组件未根据目标字段的 `min` / `max` / `maxLength` / `pattern` 等约束做限制。

### 建议方案

**前端联动规则编辑器：做弱校验（即时提示）**

VALUE 动作的输入组件尽量复用目标字段的配置属性，减少配置错误：

| 目标字段类型 | VALUE 输入组件 | 复用的约束属性 |
|-------------|---------------|--------------|
| NUMBER | `el-input-number` | `:min="field.min" :max="field.max"` |
| RATE | `el-rate` | `:max="field.max ?? 5"` |
| SLIDER | `el-slider` | `:min="field.min" :max="field.max"` |
| INPUT / TEXTAREA | `el-input` | `:maxlength="field.maxLength"` + `show-word-limit` |
| DATE / DATETIME / TIME / DATERANGE | `el-date-picker` / `el-time-picker` | 使用 `value-format` 保持一致 |

**后端：强校验延后实现**

后端 `validateBeforeDeploy` 暂不校验 VALUE actionValue 的合法性，后续版本再补充。

---

## 4. 日期/时间 VALUE 的格式

### 问题
VALUE 动作对 DATE/DATETIME/TIME/DATERANGE 使用日期选择器，设置 `value-format` 后保存为字符串。不确定后端接受的格式。

### 答案

**后端完全按字符串接收和存储**，`cat_dynamic_form_field_instance.val` 是 `text` 类型，没有任何格式转换。

前端保持以下格式即可：

| 字段类型 | value-format | 示例值 |
|---------|-------------|--------|
| DATE | `YYYY-MM-DD` | `"2026-05-21"` |
| DATETIME | `YYYY-MM-DD HH:mm:ss` | `"2026-05-21 14:30:00"` |
| TIME | `HH:mm:ss` | `"14:30:00"` |
| DATERANGE | `YYYY-MM-DD` | `["2026-05-01", "2026-05-21"]` |

**前端只需保证：填表页的日期选择器 和 联动规则编辑器的日期选择器 使用相同的 `value-format`**，前后端就能正确对接。

---

## 附录：前端修改优先级

| 优先级 | 事项 | 涉及文件 |
|--------|------|---------|
| **高** | OPTION 动作补充 `CASCADER` / `MULTICASCADER` 的树形选项编辑器，或临时过滤 | `LinkageEditor.vue` |
| **高** | 补充 `OPTION` / `VALUE` 动作的运行时处理逻辑 | `linkage.ts` / `FormMaker.vue` |
| **中** | RADIO / COLOR / SLIDER 增加清空/重置按钮 | `FieldRenderer.vue` |
| **中** | NUMBER 改用 `el-input type="number"` 或处理空值 | `FieldRenderer.vue` |
| **低** | VALUE 编辑器复用目标字段的 min/max/maxLength 约束 | `LinkageEditor.vue` |

---

> 后端 VALUE actionValue 的发布前强校验暂不实现，后续版本再补充。