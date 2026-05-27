# 动态表单远程选项数据源问题答复

本文档针对 `docs/dynamic-form-remote-option-source-questions.md` 中前端提出的问题进行后端确认。

## 1. OPTION 联动动作语义

确认采用前端当前联动设计：

- `OPTION` 不新增、不替换候选项。
- `OPTION` 只对运行时基础选项做显示/隐藏过滤。
- 级联字段也只对运行时基础选项树做递归显隐过滤。

远程选项场景下处理优先级为：

1. 先通过 `optionSource` 加载并映射远程 options。
2. 将远程加载结果作为运行时基础选项缓存。
3. 最后基于运行时基础选项缓存应用联动 `OPTION` 动作产生的显隐过滤。

即：

```text
optionSource/options 提供基础选项来源
运行时缓存保存基础选项
联动 OPTION 基于运行时基础选项递归做显隐过滤
```

远程 options 加载结果不回写字段自身 `options`，避免污染表单模板配置。`API` 模式下，字段自身 `options` 不是 `OPTION` 过滤的基础数据。

## 2. API URL 是否只允许同源相对路径

确认第一版只允许同源相对路径，例如：

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

前端请求统一走当前项目 HTTP 工具，复用 baseURL、token、拦截器和错误处理。

## 3. 第一版是否保留 headers 字段

第一版不保留 `headers` 字段。

后端不接收、不保存、不透传 `headers`。

鉴权统一使用当前前端登录态和项目 HTTP 客户端，不允许在表单配置中保存：

- `Authorization`
- `Cookie`
- 自定义 token
- 其他敏感请求头

## 4. params 请求语义

确认第一版 `params` 为静态参数。

- `method = GET` 时，`params` 作为 query 参数。
- `method = POST` 时，`params` 作为 JSON body。
- 第一版只支持静态参数。
- 第一版不支持引用其他字段值。
- 第一版不支持表达式。
- 第一版不支持运行时动态拼接。

`params` 的 value 允许 JSON 基础类型：

- string
- number
- boolean
- array
- object
- null

后端只做 JSON 结构保存和基础类型校验，不做业务含义校验。

## 5. mapping 路径语法

确认第一版只支持简单点路径。

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
- 如果接口响应本身就是数组，`listPath = "$"`。
- `labelPath`、`valuePath`、`childrenPath` 均相对于每个选项节点。
- 级联 children 递归映射时，子节点继续使用同一套 `labelPath` / `valuePath` / `childrenPath`。

## 6. valuePath 映射结果类型

确认映射后的 `value` 只允许：

- string
- number

如果远程接口返回：

- boolean
- object
- array
- null

前端应判定为远程选项数据格式错误，不自动转字符串。

后端发布前不真实请求远程 API，因此只校验 mapping 配置，不校验接口返回值类型。

## 7. API 模式下默认值校验

确认 `optionSource.type = API` 时，默认值和提交值保持一致：只校验结构，不校验是否存在于远程 options。

具体规则：

- `SELECT` / `RADIO`：默认值非空时必须是单值。
- `MULTISELECT` / `CHECKBOX`：默认值非空时必须是数组。
- `CASCADER`：默认值非空时必须是一维数组路径。
- `MULTICASCADER`：默认值非空时必须是二维数组路径。

## 8. 远程 options 加载失败时的前端行为

接受前端建议：

- 加载中禁用字段。
- 加载失败时显示错误提示和重试入口。
- 不静默降级为空数组。
- 必填字段加载失败时，用户无法选择值，正常触发表单必填校验。
- 非必填字段加载失败时，用户可以保持空值提交。
- 编辑历史数据时，如果远程 options 加载失败，保留原始值，但提示选项加载失败。

## 9. 远程 options 与联动运行时的异步关系

接受前端处理：

- 表单初次渲染时字段处于加载态。
- 远程 options 加载完成后，前端将映射结果写入运行时基础选项缓存。
- 远程 options 加载结果不回写字段自身 `options`。
- 前端基于运行时基础选项缓存重新计算字段运行态。
- 联动 `OPTION` 过滤应用在远程加载后的运行时基础选项上。
- 级联字段的 `OPTION` 过滤基于运行时基础选项树递归处理显隐。
- 如果远程 options 重新加载，运行时基础选项缓存更新，联动过滤也重新应用。

## 10. 级联字段提交结构

确认按数组结构提交和校验。

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

## 11. STATIC 模式下 optionSource 是否可以不保存

确认静态选项字段可以不传 `optionSource`。

- 后端不强制补 `{ "type": "STATIC" }`。
- 后端返回历史数据时，`optionSource = null` 或缺失，前端按 `STATIC` 处理。
- 这样可以减少数据噪音，并兼容历史数据。

## 12. 映射后的 children 是否只在有子节点时输出

确认叶子节点可以不输出 `children`。

建议前端只有在存在子节点时才输出 `children`，避免部分级联组件把空 `children: []` 识别为可展开但无数据。

## 13. 第一版前端实现范围

确认第一版前端同步支持：

- 前端类型增加 `optionSource`。
- 字段编辑器支持配置选项来源：静态 / API。
- API 模式下配置 `url`、`method`、`params`、`mapping`。
- 字段渲染器支持远程加载 options。
- 设计预览和运行态填表共用远程 options 加载逻辑。
- 加载失败显示错误和重试入口。
- 联动 `OPTION` 可以叠加过滤远程 options。

## 14. 后端返回字段命名格式

确认接口 JSON 统一使用 camelCase：

```json
{
  "optionSource": {
    "type": "API"
  }
}
```

数据库字段使用：

```text
option_source
```

Java 字段使用：

```text
optionSource
```

前端只按 `optionSource` 读取和提交。

## 15. 第一版最终边界

第一版采用：

```text
字段级 optionSource 配置 + 前端加载远程 options + 后端校验配置、默认值结构和提交值结构
```

后端第一版不请求远程 API，不代理远程 API，不校验默认值或提交值是否存在于远程 options。

后续如需要，可以扩展：

- 后端代理请求。
- URL 白名单。
- 远程 options 缓存。
- `validateRemoteValue` 强校验开关。
- 用户/部门/角色/字典等字段模板。