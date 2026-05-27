# 动态表单远程选项数据源设计方案

## 1. 背景

当前动态表单中，`SELECT`、`MULTISELECT`、`RADIO`、`CHECKBOX`、`CASCADER`、`MULTICASCADER` 等字段依赖字段自身的 `options` 存储候选项。

后续存在用户、部门、字典、项目、业务对象等候选项来自接口的场景。当前阶段不做用户/部门专用字段，也不做固定模板能力，先实现一套通用的远程选项数据源配置，供适用字段复用。

用户/部门后期可以作为字段模板预置一份远程数据源配置，而不是新增特殊字段类型。

## 2. 目标

- 为适用字段支持 API 远程获取候选项。
- 保持现有静态 `options` 能力不变。
- 第一版不做用户/部门专用逻辑。
- 第一版不做后端代理请求远程 API。
- 第一版不让后端表单发布和提交强依赖远程接口可用性。
- 前端根据统一配置加载 API 数据，并映射为现有 `DynamicFormOption[]`。
- 后端负责发布前配置校验、默认值结构校验和提交值结构校验。

## 3. 不做范围

第一版不做以下能力：

- 用户/部门专用字段类型。
- 用户/部门专用模板。
- 后端代理请求远程 API。
- 后端缓存远程候选项。
- 后端校验默认值或提交值是否存在于远程 API 返回值中。
- 远程接口鉴权配置。
- 自定义请求头配置。
- URL 白名单。
- 定时同步远程 options。
- 动态参数、字段值引用、表达式参数。

这些能力可以在远程选项数据源稳定后按需扩展。

## 4. 适用字段

远程选项数据源仅适用于有候选项概念的字段：

- `SELECT`
- `MULTISELECT`
- `RADIO`
- `CHECKBOX`
- `CASCADER`
- `MULTICASCADER`

不适用于：

- `INPUT`
- `NUMBER`
- `TEXTAREA`
- `DATE`
- `DATETIME`
- `TIME`
- `DATERANGE`
- `UPLOAD`
- `TABLE`
- `SWITCH`
- `RATE`
- `SLIDER`
- `COLOR`

## 5. 推荐数据模型

建议在 `DynamicFormField` 上新增独立字段 `optionSource`，不要放入 `props`。

原因：

- `props` 更适合组件行为配置，例如级联字段的 `checkStrictly`。
- `optionSource` 表达的是候选项数据来源，语义更清晰。
- 后端发布校验、提交校验、版本复制更容易独立处理。
- 后期字段模板可以直接复用该配置。

### 5.1 DynamicFormOptionSource

```java
public class DynamicFormOptionSource {
    private String type; // STATIC / API
    private String url;
    private String method; // GET / POST
    private Map<String, Object> params;
    private DynamicFormOptionMapping mapping;
}
```

第一版不设计 `headers` 字段。鉴权统一复用当前前端登录态和项目 HTTP 客户端，不允许在表单配置中保存或透传 `Authorization`、`Cookie` 等敏感请求头。

### 5.2 DynamicFormOptionMapping

```java
public class DynamicFormOptionMapping {
    private String listPath;
    private String labelPath;
    private String valuePath;
    private String childrenPath;
}
```

字段说明：

| 字段 | 说明 | 是否必填 |
| --- | --- | --- |
| `type` | 选项来源类型，`STATIC` 或 `API` | 否，空值按 `STATIC` 处理 |
| `url` | 同源相对 API 地址 | `API` 模式必填 |
| `method` | 请求方式，`GET` 或 `POST` | `API` 模式必填 |
| `params` | 静态请求参数 | 否 |
| `mapping` | API 返回数据到选项结构的映射规则 | `API` 模式必填 |
| `listPath` | 候选项数组在响应中的路径 | 必填 |
| `labelPath` | 选项显示文本字段路径 | 必填 |
| `valuePath` | 选项值字段路径 | 必填 |
| `childrenPath` | 子选项字段路径 | 级联字段建议填写 |

## 6. API URL 规则

第一版只允许同源相对路径，例如：

```text
/api/common/options/users
/api/common/options/project-types
```

不允许：

```text
http://example.com/options
https://example.com/options
//example.com/options
```

前端请求应统一走当前项目 HTTP 工具，以复用：

- baseURL
- token
- 请求拦截器
- 响应拦截器
- 统一错误处理

## 7. params 请求语义

`params` 第一版只支持静态参数。

- `method = GET` 时，`params` 作为 query 参数。
- `method = POST` 时，`params` 作为 JSON body。
- 不支持引用其他字段值。
- 不支持表达式。
- 不支持运行时动态拼接。

`params` 的 value 允许 JSON 基础类型：

- string
- number
- boolean
- array
- object
- null

后端只做 JSON 结构保存和基础类型校验，不做业务含义校验。

## 8. mapping 路径语法

第一版只支持简单点路径。

支持：

```text
data
data.records
name
id
children
```

不支持：

```text
data[0].children
$.data[*]
JSONPath 表达式
函数表达式
```

约定：

- `listPath` 指向响应中的数组。
- 如果响应本身就是数组，`listPath = "$"`。
- `labelPath`、`valuePath`、`childrenPath` 均相对于每个选项节点。
- 子节点递归时继续使用同一套 `labelPath`、`valuePath`、`childrenPath`。

映射后的 `value` 只能是 string 或 number。若前端加载到 boolean、object、array、null，应提示远程选项数据格式错误，不自动转字符串。

## 9. 配置示例

### 9.1 静态选项

```json
{
  "type": "SELECT",
  "title": "性别",
  "options": [
    {
      "label": "男",
      "value": "male"
    },
    {
      "label": "女",
      "value": "female"
    }
  ]
}
```

静态模式下 `optionSource` 可以不传。空值等价于 `STATIC`，后端不强制补 `{ "type": "STATIC" }`。

### 9.2 普通远程选项

```json
{
  "type": "SELECT",
  "title": "项目类型",
  "optionSource": {
    "type": "API",
    "url": "/api/common/options/project-types",
    "method": "GET",
    "params": {
      "enabled": true
    },
    "mapping": {
      "listPath": "data",
      "labelPath": "name",
      "valuePath": "code"
    }
  },
  "options": []
}
```

API 返回示例：

```json
{
  "code": 200,
  "data": [
    {
      "name": "研发项目",
      "code": "dev"
    },
    {
      "name": "实施项目",
      "code": "delivery"
    }
  ]
}
```

前端根据 `mapping` 转换为：

```json
[
  {
    "label": "研发项目",
    "value": "dev"
  },
  {
    "label": "实施项目",
    "value": "delivery"
  }
]
```

### 9.3 根数组响应

当接口响应本身就是数组时：

```json
[
  {
    "name": "研发项目",
    "code": "dev"
  },
  {
    "name": "实施项目",
    "code": "delivery"
  }
]
```

配置：

```json
{
  "optionSource": {
    "type": "API",
    "url": "/api/common/options/project-types",
    "method": "GET",
    "mapping": {
      "listPath": "$",
      "labelPath": "name",
      "valuePath": "code"
    }
  }
}
```

### 9.4 级联远程选项

```json
{
  "type": "CASCADER",
  "title": "组织",
  "optionSource": {
    "type": "API",
    "url": "/api/common/options/org-tree",
    "method": "GET",
    "mapping": {
      "listPath": "data",
      "labelPath": "name",
      "valuePath": "id",
      "childrenPath": "children"
    }
  },
  "options": [],
  "props": {
    "checkStrictly": false
  }
}
```

前端映射叶子节点时可以不输出 `children`。只有存在子节点时才输出 `children`，避免部分级联组件把空 `children: []` 识别为可展开但无数据。

## 10. 静态 options 与远程 optionSource 的关系

### 10.1 默认行为

`optionSource` 为空时，按现有静态 `options` 逻辑处理。

### 10.2 `STATIC` 模式

- `options` 必须满足现有校验。
- 默认值必须存在于 `options` 中。
- 提交值必须存在于 `options` 中。

### 10.3 `API` 模式

- `options` 可以为空。
- 前端根据 `optionSource` 拉取候选项。
- 后端发布时校验 `optionSource` 配置是否合法。
- 后端发布时只校验 `defaultValue` 结构，不校验默认值是否存在于远程 options 中。
- 后端提交时只校验值结构，不校验值是否存在于远程 options 中。

## 11. 后端发布前校验规则

### 11.1 通用校验

如果字段类型不在适用范围内，但配置了：

```json
{
  "optionSource": {
    "type": "API"
  }
}
```

发布失败：

```text
字段 "xxx" 不支持远程选项数据源
```

### 11.2 `type` 校验

`optionSource.type` 只允许：

- `STATIC`
- `API`

为空时按 `STATIC` 处理。

### 11.3 `STATIC` 校验

保持现有校验：

- `SELECT` / `RADIO` 必须配置有效 `options`。
- `MULTISELECT` / `CHECKBOX` 必须配置有效 `options`。
- `CASCADER` / `MULTICASCADER` 必须配置有效树形 `options`。
- 默认值必须存在于 `options` 或级联树中。

### 11.4 `API` 校验

`API` 模式发布时校验：

- `url` 不能为空。
- `url` 必须是同源相对路径。
- `url` 不允许以 `http://`、`https://`、`//` 开头。
- `method` 只能是 `GET` 或 `POST`。
- `params` 必须是合法 JSON 对象。
- `mapping` 不能为空。
- `mapping.listPath` 不能为空。
- `mapping.labelPath` 不能为空。
- `mapping.valuePath` 不能为空。
- `mapping` 路径只能使用简单点路径，`listPath` 可使用 `$` 表示根数组。
- `CASCADER` / `MULTICASCADER` 建议配置 `mapping.childrenPath`。

第一版不在发布时真实请求 API。

## 12. API 模式默认值校验

`optionSource.type = API` 时，默认值只校验结构，不校验是否存在于远程 options。

- `SELECT` / `RADIO`：默认值非空时必须是单值。
- `MULTISELECT` / `CHECKBOX`：默认值非空时必须是数组。
- `CASCADER`：默认值非空时必须是一维数组路径。
- `MULTICASCADER`：默认值非空时必须是二维数组路径。

## 13. 后端提交实例校验规则

### 13.1 `STATIC` 模式

保持现有逻辑：

- `SELECT` / `RADIO`：值必须存在于 `options`。
- `MULTISELECT` / `CHECKBOX`：数组内每个值都必须存在于 `options`。
- `CASCADER`：路径必须存在于树形 `options`。
- `MULTICASCADER`：每条路径都必须存在于树形 `options`。

### 13.2 `API` 模式

提交时只校验值结构。

- `SELECT` / `RADIO`：非空时必须是单值。
- `MULTISELECT` / `CHECKBOX`：非空时必须是数组。
- `CASCADER`：非空时必须是一维数组路径。
- `MULTICASCADER`：非空时必须是二维数组路径。
- `required`、`min`、`max` 等现有约束继续生效。

不校验提交值是否存在于远程 API 返回值中。

原因：

- 远程候选项可能随时间变化。
- 历史提交值不应因为远程数据变化变成非法。
- 表单提交不应因为远程接口超时或不可用而失败。
- 第一版目标是稳定接入远程候选项，不引入强依赖。

## 14. 级联字段提交结构

级联字段统一按数组结构提交和校验。

`CASCADER` 固定为一维数组路径：

```json
["beijing", "chaoyang"]
```

`MULTICASCADER` 固定为二维数组路径：

```json
[
  ["beijing", "chaoyang"],
  ["shanghai", "pudong"]
]
```

不再支持逗号字符串，例如：

```json
"beijing,chaoyang"
```

## 15. 前端处理约定

### 15.1 加载逻辑

前端渲染字段时：

1. 如果 `optionSource` 为空或 `optionSource.type = STATIC`：
   - 使用字段自身 `options`。

2. 如果 `optionSource.type = API`：
   - 根据 `url`、`method`、`params` 请求接口。
   - 根据 `mapping` 将接口结果转换成 `DynamicFormOption[]`。
   - 将转换后的 options 作为运行时基础选项缓存。
   - 使用运行时基础选项渲染组件。
   - 不将远程加载结果回写到字段自身 `options`，避免污染表单模板配置。

### 15.2 运行时基础选项缓存

远程 options 加载结果属于运行态数据，不属于字段配置数据。

前端需要维护字段运行时基础选项缓存：

```text
fieldId -> baseOptions
```

约定：

- `STATIC` 模式下，基础选项来自字段自身 `options`。
- `API` 模式下，基础选项来自远程加载并映射后的结果。
- 远程加载结果只写入运行时缓存，不回写 `field.options`。
- 表单保存、发布、复制版本时只保存 `optionSource` 配置，不保存远程加载结果。
- 设计器预览和运行态填表都应基于运行时基础选项缓存渲染。

### 15.3 加载失败

远程 options 加载失败时，前端行为约定：

- 加载中禁用字段。
- 加载失败时显示错误提示和重试入口。
- 不静默降级为空数组。
- 必填字段加载失败时，用户无法选择值，正常触发表单必填校验。
- 非必填字段加载失败时，用户可以保持空值提交。
- 编辑历史数据时，如果远程 options 加载失败，保留原始值，但提示选项加载失败。

### 15.4 映射后的统一结构

前端最终应统一转换成现有结构：

```json
{
  "label": "显示文本",
  "value": "提交值",
  "visible": true,
  "children": []
}
```

其中：

- `visible` 可选，默认 `true`。
- `children` 仅在存在子节点时输出。

## 16. 联动规则影响

`OPTION` 动作不新增、不替换候选项，只对运行时基础选项做显示/隐藏过滤。

远程选项场景下推荐顺序：

1. 先通过 `optionSource` 加载并映射远程 options。
2. 将远程加载结果写入运行时基础选项缓存。
3. 基于运行时基础选项缓存应用默认显隐和联动显隐过滤。

即：

```text
optionSource/options 提供基础选项来源
运行时缓存保存基础选项
联动 OPTION 基于运行时基础选项递归做显隐过滤
```

远程 options 是异步加载的，因此前端运行态约定：

- 表单初次渲染时字段处于加载态。
- 远程 options 加载完成后，前端更新运行时基础选项缓存。
- 前端基于运行时基础选项缓存重新计算字段运行态。
- 联动 `OPTION` 过滤应用在远程加载后的运行时基础选项上。
- 级联字段的 `OPTION` 过滤需要基于运行时基础选项树递归处理显隐。
- 如果远程 options 重新加载，运行时基础选项缓存更新，联动过滤也重新应用。

`API` 模式下，字段自身 `options` 不是远程加载结果的存储位置，不作为 `OPTION` 过滤的基础数据。

## 17. 前端第一版实现范围

第一版前端同步支持：

- 类型增加 `optionSource`。
- 字段编辑器支持配置选项来源：静态 / API。
- API 模式支持配置 `url`、`method`、`params`、`mapping`。
- 字段渲染器支持远程加载 options。
- 设计预览和运行态填表共用远程 options 加载逻辑。
- 加载失败显示错误和重试入口。
- 联动 `OPTION` 可以叠加过滤远程 options。

## 18. 接口字段命名

数据库字段建议为：

```text
option_source
```

Java 字段为：

```text
optionSource
```

接口 JSON 统一使用 camelCase：

```json
{
  "optionSource": {
    "type": "API"
  }
}
```

前端只按 `optionSource` 读取和提交。

## 19. 数据库变更建议

由于当前 `options`、`columns` 等 JSON 配置字段使用 `text + Fastjson2TypeHandler`，建议新增字段也使用 `text`，保持风格一致。

```sql
ALTER TABLE `cat_dynamic_form_field`
ADD COLUMN `option_source` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '选项远程数据源配置' AFTER `options`;
```

如果后续统一改造 JSON 字段，也可以再考虑使用 MySQL `json` 类型。

## 20. 版本复制要求

新增 `option_source` 后，必须同步修改字段版本复制逻辑。

`DynamicFormFieldMapper.copyVersion` 中需要同时复制：

- insert 字段列表中的 `option_source`
- select 字段列表中的 `option_source`

否则发布、停用、重新生成草稿时，远程数据源配置会丢失。

## 21. 用户/部门模板后续接入方式

用户/部门后续不需要成为特殊字段类型，可以作为字段模板。

### 21.1 用户选择模板

```json
{
  "type": "SELECT",
  "title": "用户",
  "optionSource": {
    "type": "API",
    "url": "/api/common/options/users",
    "method": "GET",
    "mapping": {
      "listPath": "data",
      "labelPath": "nickname",
      "valuePath": "id"
    }
  },
  "options": []
}
```

### 21.2 部门选择模板

```json
{
  "type": "CASCADER",
  "title": "部门",
  "optionSource": {
    "type": "API",
    "url": "/api/common/options/departments",
    "method": "GET",
    "mapping": {
      "listPath": "data",
      "labelPath": "name",
      "valuePath": "id",
      "childrenPath": "children"
    }
  },
  "options": []
}
```

模板只是预设字段配置，不影响动态表单核心字段类型。

## 22. 第一版推荐实现范围

第一版后端建议实现：

- 新增 `DynamicFormOptionSource`。
- 新增 `DynamicFormOptionMapping`。
- `DynamicFormField` 新增 `optionSource`。
- 数据库新增 `option_source` 字段。
- 字段版本复制时复制 `option_source`。
- 发布前校验 `optionSource` 配置。
- `API` 模式下允许适用字段的 `options` 为空。
- `API` 模式下发布时只校验默认值结构。
- `API` 模式下提交时只校验值结构。
- 文档约定前端如何加载和映射远程 options。

## 23. 后续可扩展能力

远程选项能力稳定后，可以继续扩展：

- 后端代理远程 options 请求。
- URL 白名单。
- 后端统一鉴权。
- 远程 options 缓存。
- `validateRemoteValue` 强校验开关。
- 字段模板市场，例如用户、部门、角色、岗位、字典、项目等。
- 远程 options 依赖其他字段值动态传参。

## 24. 结论

当前阶段推荐方案：

```text
字段级 optionSource 配置 + 前端加载远程 options + 后端校验配置、默认值结构和提交值结构
```

该方案改动小、风险低，并且不污染字段类型体系。用户、部门等业务对象后续可以作为字段模板复用这套通用配置。