# 动态表单前后端联调 — 待确认事项清单

> 生成日期：2026-05-21
> 基于前端代码（`src/components/dynamicForm/`、`src/views/console/dynamicForm/`、`src/views/main/dynamicForm/`）与后端文档（`docs/dynamic-form-api.md`）比对整理

---

## 优先级说明

- **[高]** 功能阻塞，必须确认后才能继续开发/联调
- **[中]** 影响数据一致性，建议本轮确认
- **[低]** 细节优化，可延后处理

---

## 1. [高] DATERANGE 字段类型缺失

### 问题描述

前端代码已实现 `DATERANGE`（日期区间）类型，但后端文档字段枚举（第4节）未列出该类型。

### 前端实现

- `src/components/dynamicForm/types/index.ts` 包含 `DATERANGE`
- `src/components/dynamicForm/FieldRenderer.vue` 使用 `el-date-picker type="daterange"`
- 值格式为字符串数组：`["2026-05-01", "2026-05-21"]`

### 待确认

1. 后端 `DynamicFormFieldType` 枚举是否包含 `DATERANGE`？
2. 若暂不支持，前端是否需要移除该类型？
3. 若支持，`DATERANGE` 的 `defaultValue` 和 `data` 中的值格式是否为字符串数组？

---

## 2. [高] 分页查询参数名不一致

### 问题描述

前端请求参数名与后端文档描述不一致，但响应格式一致。

| 来源 | 页码参数 | 每页条数参数 | 搜索关键字 |
|------|---------|-------------|-----------|
| 前端代码（`IndexView.vue:298-302`） | `current` | `size` | `search` |
| 后端文档（7.2.5） | `pageNum` | `pageSize` | 未提及 |

### 待确认

1. 实际接口接收的页码参数是 `current` 还是 `pageNum`？
2. 实际接口接收的每页条数参数是 `size` 还是 `pageSize`？
3. 搜索功能支持的字段范围（仅 `name`？还是 `name` + `description`？）

---

## 3. [高] 数值/开关类型字段的值类型

### 问题描述

前端组件原生输出类型与后端文档 5.2 节描述不一致。

| 字段类型 | 前端组件原生输出 | 后端文档要求 |
|----------|-----------------|-------------|
| `NUMBER` | `number` | `String`（如 `"123.45"`） |
| `RATE` | `number` | `String`（如 `"4"`） |
| `SLIDER` | `number` | `String`（如 `"50"`） |
| `SWITCH` | `boolean` | `String`（如 `"true"`/`"false"`） |

### 待确认

1. 后端 `submit` 接口是否接受原生类型（`number`/`boolean`）？
2. 若后端严格要求字符串，前端需要在提交前统一转换，请确认转换规则：
   - `number` → `String(number)`
   - `boolean` → `"true"` / `"false"`
   - `null`/`undefined` → `""` 或 `null`？
3. `FormData.data` 中各字段值是否统一按后端文档 5.2 节的类型要求处理？

---

## 4. [高] UPLOAD 字段数据格式字段名不一致

### 问题描述

文件字段在 `FormData.data` 中的对象结构，前后端字段名存在差异。

**前端使用的格式**（`FieldRenderer.vue:358-381`）：

```json
[
  {
    "id": "file-xxx",
    "filename": "a.pdf",
    "contentType": "application/pdf",
    "size": 1024
  }
]
```

**后端文档要求**（第9.1节）：

```json
[
  {
    "fileId": "file-xxx",
    "name": "a.pdf",
    "size": 1048576,
    "type": "application/pdf"
  }
]
```

### 字段名差异对照

| 含义 | 前端字段名 | 后端文档字段名 |
|------|-----------|---------------|
| 文件唯一标识 | `id` | `fileId` |
| 文件名 | `filename` | `name` |
| MIME 类型 | `contentType` | `type` |
| 文件大小 | `size` | `size` |

### 待确认

1. 后端 `submit` 接口实际解析 UPLOAD 字段时使用哪些字段名？
2. 文件下载时 `GET /file/downloadDynamicForm?fileId={fileId}` 中的 `fileId` 对应前端哪个字段？
3. 建议统一字段命名，推荐方案：
   - 方案A：前端改为 `fileId`/`name`/`type`，与后端文档一致
   - 方案B：后端改为 `id`/`filename`/`contentType`，与前端一致

---

## 5. [中] CASCADER / MULTICASCADER 值格式

### 问题描述

后端文档（5.2节）示例值为逗号分隔的路径字符串，但前端组件实际输出不同。

**后端文档示例：**

```json
// CASCADER
"zhejiang,hangzhou,xihu"
// MULTICASCADER
["zhejiang,hangzhou", "jiangsu,nanjing"]
```

**前端实现：** 使用 Element Plus `el-cascader`，配置 `emitPath: false`，选中后返回的是选中节点的**单个 value 值**（如 `"xihu"`），不是逗号分隔路径。

### 待确认

1. 后端实际期望 `CASCADER` 的值格式是：
   - 单值字符串（如 `"xihu"`）？
   - 逗号分隔路径字符串（如 `"zhejiang,hangzhou,xihu"`）？
   - 路径数组（如 `["zhejiang", "hangzhou", "xihu"]`）？
2. `MULTICASCADER` 的值格式是：
   - 单值字符串数组（如 `["xihu", "gulou"]`）？
   - 逗号分隔路径字符串数组（如 `["zhejiang,hangzhou", "jiangsu,nanjing"]`）？
   - 路径数组的数组（如 `[["zhejiang", "hangzhou"], ["jiangsu", "nanjing"]]`）？
3. 确认后前端需要调整 `el-cascader` 的 `emitPath` 和 `checkStrictly` 配置。

---

## 6. [中] 文件上传响应格式

### 问题描述

后端文档（7.4.1节）上传响应包含 `type` 字段，但前端代码未使用。

**后端文档响应示例：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "file-uuid",
    "filename": "合同.pdf",
    "contentType": "application/pdf",
    "type": "file",
    "size": 1048576
  }
}
```

**前端代码**（`FieldRenderer.vue:358-381`）只读取 `response.data.id` 和 `response.data.filename`。

### 待确认

1. 实际上传响应是否包含 `contentType` 字段？
2. `type` 字段（值为 `"file"`）的用途是什么？前端是否需要读取？
3. 建议精简响应字段，只保留前端需要的：`id`（或 `fileId`）、`filename`（或 `name`）、`contentType`（或 `type`）、`size`

---

## 7. [中] UPLOAD 字段的 minLength 支持

### 问题描述

后端数据库表 `cat_dynamic_form_field.min_length` 注释为"最小长度/最少上传数"，但前端 UI 中 `UPLOAD` 类型只配置了 `maxLength`（最多上传数量），没有 `minLength` 的配置入口。

**前端表现：**

- `FieldEditor.vue` 中 `UPLOAD` 类型仅显示"最多上传数量"（`maxLength`）
- 校验规则 `buildItemRules` 对 UPLOAD 只处理 `maxLength`，不处理 `minLength`

### 待确认

1. 后端是否支持 UPLOAD 字段的 `minLength` 校验（最少上传几个文件）？
2. 若支持，前端需补充 UI 和校验逻辑
3. 若不支持，建议后端数据库注释或校验逻辑中移除该语义，避免混淆

---

## 8. [低] 删除权限范围

### 问题描述

前端 UI 逻辑与后端文档描述的删除权限范围不一致。

| 状态 | 前端是否显示删除按钮 | 后端是否允许删除 |
|------|---------------------|-----------------|
| `0`（草稿） | 是 | 是 |
| `-1`（停用） | **否** | 是 |
| `1`（发布） | 否 | 否 |

**前端代码**（`IndexView.vue:157-163`）：`v-if="scope.row.status == '0'"`

**后端文档**（7.2.2节）："仅草稿（status=0）或停用（status=-1）状态可删除"

### 待确认

1. 是否需要在 `status == '-1'`（停用）状态也显示删除按钮？
2. 还是后端应限制仅草稿可删除，与前端保持一致？

---

## 9. [低] 表单添加/更新时的字段去重逻辑

### 问题描述

前端提交前对 `formFields` 进行过滤，有 `groupId` 的字段会被移除，只通过 `groups` 传递。

**前端代码**（`DynamicFormAddView.vue:125-130`、`DynamicFormInfoView.vue:216-221`）：

```js
const payload = {
  ...info.value,
  groups: info.value.groups,
  formFields: info.value.formFields.filter(f => !f.groupId),
}
```

### 待确认

1. 后端是否能正确识别这种结构（分组字段在 `groups` 里，未分组字段在 `formFields` 里）？
2. 后端是否期望所有字段都平铺在 `formFields` 中（每个字段带 `groupId`），`groups` 只保留分组元信息？
3. 建议统一数据提交格式，避免前端过滤逻辑与后端解析逻辑不匹配导致字段丢失

---

## 10. [低] info 接口的分组字段合并逻辑

### 问题描述

前端加载 `info` 响应时，将 `groups` 内的字段平铺后与 `formFields` 合并。

**前端代码**（`DynamicFormInfoView.vue:188-194`、`main/IndexView.vue:84-88`）：

```js
const groups = data.groups || []
const formFields = data.formFields || []
info.value = {
  ...data,
  formFields: groups.length > 0
    ? flattenGroups(groups).concat(formFields)
    : formFields,
  groups: groups.length > 0 ? groups : undefined,
}
```

### 待确认

1. 后端返回的 `formFields` 和 `groups.fields` 之间是否会有重复字段？
2. 若后端同时在 `formFields` 和 `groups.fields` 返回同一份字段数据，前端合并后会产生重复
3. 建议后端明确约定：分组字段只在 `groups` 中返回，`formFields` 仅包含未分组字段

---

## 附录：前端关键代码位置

| 功能 | 文件路径 |
|------|---------|
| 字段类型定义 | `src/components/dynamicForm/types/index.ts` |
| 字段渲染器 | `src/components/dynamicForm/FieldRenderer.vue` |
| 字段编辑器 | `src/components/dynamicForm/FieldEditor.vue` |
| 联动引擎 | `src/components/dynamicForm/linkage.ts` |
| 表单设计器 | `src/components/dynamicForm/FormMaker.vue` |
| 添加表单页 | `src/views/console/dynamicForm/DynamicFormAddView.vue` |
| 编辑/详情页 | `src/views/console/dynamicForm/DynamicFormInfoView.vue` |
| 表单列表页 | `src/views/console/dynamicForm/IndexView.vue` |
| 用户填表页 | `src/views/main/dynamicForm/IndexView.vue` |

---

> 建议优先处理 **[高]** 优先级事项（1-4项），这些会直接影响功能联调和数据正确性。
