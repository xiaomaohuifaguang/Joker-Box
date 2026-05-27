# 动态表单远程选项数据源问题清单

基于 `docs/dynamic-form-remote-option-source-design.md` 的前端实现视角审查，以下问题需要后端确认或补充设计。

## 1. OPTION 联动动作语义是否需要调整

当前文档第 11 节描述：`OPTION` 动作用于动态设置候选项，并推荐“联动 OPTION 动态设置的 options 优先”。

但当前前端已确定的联动设计是：

- `OPTION` 不新增、不替换候选项。
- `OPTION` 只对当前候选项集合做显示/隐藏过滤。
- 级联字段也只是对已有树形选项做显隐过滤。

需要后端确认：远程选项场景下是否采用以下优先级？

1. 先通过 `optionSource` 加载基础候选项。
2. 再应用字段自身 `options.visible` 的默认显隐配置。
3. 最后应用联动 `OPTION` 动作产生的显隐过滤。

如果不采用该优先级，请说明 `OPTION` 对远程候选项的准确语义。

## 2. 第一版 API URL 是否只允许同源相对路径

文档第一版不做 URL 白名单，也不做后端代理请求远程 API。

如果前端直接按配置请求 API，需要确认 `url` 的边界：

- 是否只允许同源相对路径，例如 `/api/common/options/users`？
- 是否禁止 `http://`、`https://` 绝对地址？
- 是否统一走当前前端 `http` 工具，以复用 baseURL、token、拦截器和错误处理？

建议第一版只允许同源相对路径，避免 CORS、鉴权和外部地址风险。

## 3. 第一版是否需要保留 headers 字段

文档模型中包含：

```java
private Map<String, String> headers;
```

但字段说明中又写“第一版可以先不开放”。

需要后端确认：

- 第一版是否在数据库和 DTO 中保存 `headers`？
- 如果保存，是否允许前端提交？
- 是否需要禁止保存敏感请求头，例如 `Authorization`、`Cookie`？

建议第一版不接收、不保存、不透传 `headers`，鉴权统一使用当前前端登录态和项目 HTTP 客户端。

## 4. params 的请求语义需要明确

文档中 `params` 尚未定义 GET/POST 下的具体用法。

需要后端确认：

- `method = GET` 时，`params` 是否作为 query 参数？
- `method = POST` 时，`params` 是否作为 JSON body？
- 第一版是否只支持静态参数？
- 第一版是否不支持引用其他字段值作为动态参数？
- `params` 的 value 类型允许哪些？例如 string、number、boolean、array、object 是否都允许？

建议第一版定义为：

- GET：`params` 作为 query。
- POST：`params` 作为 JSON body。
- 只支持静态参数。
- 不支持字段值引用和表达式。

## 5. mapping 路径语法需要明确

文档定义了：

- `listPath`
- `labelPath`
- `valuePath`
- `childrenPath`

但未定义路径语法。

需要后端确认：

- 是否只支持点路径，例如 `data.records`、`name`、`id`？
- 是否支持数组下标，例如 `data[0].children`？
- 如果接口响应本身就是数组，`listPath` 如何表示？
- `labelPath`、`valuePath`、`childrenPath` 是相对于每个选项节点，还是相对于响应根？
- 级联 children 递归映射时，子节点是否继续使用同一套 `labelPath` / `valuePath` / `childrenPath`？

建议第一版：

- 仅支持点路径。
- `listPath` 指向响应中的数组。
- 用 `$` 表示响应根数组。
- `labelPath`、`valuePath`、`childrenPath` 均相对于每个选项节点。
- 子节点递归时继续使用同一套映射规则。

## 6. valuePath 映射结果类型需要限制

当前前端选项值类型为 `string | number`。

需要后端确认：

- `valuePath` 映射结果是否只允许 string 或 number？
- 如果远程接口返回 boolean、object、array、null，前端应该加载失败还是转字符串？
- 后端发布前是否只校验 mapping 配置，不真实请求接口，因此无法校验返回值类型？

建议第一版要求远程接口映射后的 value 必须是 string 或 number；前端加载到其他类型时提示远程选项数据格式错误。

## 7. API 模式下默认值如何校验

文档明确 API 模式下提交时只校验值结构，不校验值是否存在于远程 API 返回值中。

但发布前默认值校验规则未明确。

需要后端确认：

- `optionSource.type = API` 时，发布前是否只校验 `defaultValue` 的结构？
- 是否不校验默认值是否存在于远程 options 中？
- 对 `SELECT` / `RADIO` 默认值是否只要求单值？
- 对 `MULTISELECT` / `CHECKBOX` 默认值是否只要求数组？
- 对 `CASCADER` 默认值是否只要求一维数组路径？
- 对 `MULTICASCADER` 默认值是否只要求二维数组路径？

建议 API 模式下默认值和提交值保持一致：只校验结构，不校验存在性。

## 8. 远程 options 加载失败时的前端行为是否需要后端约定

文档建议加载失败时前端显示加载失败状态，但没有进一步约定。

需要后端确认是否接受以下前端行为：

- 加载中禁用字段。
- 加载失败时显示错误提示和重试入口。
- 不静默降级为空数组。
- 必填字段加载失败时，用户无法选择值，正常触发表单必填校验。
- 非必填字段加载失败时，用户可以保持空值提交。
- 编辑历史数据时，如果远程 options 加载失败，保留原始值，但提示选项加载失败。

## 9. 远程 options 与联动运行时的异步关系

远程 options 是异步加载的，而联动规则可能依赖候选项显隐。

需要后端确认是否接受以下前端处理：

- 表单初次渲染时先使用空候选项或加载态。
- 远程 options 加载完成后，前端重新计算字段运行态。
- 联动 `OPTION` 过滤应用在远程 options 加载完成后的候选项集合上。
- 如果远程 options 重新加载，联动过滤也重新应用。

## 10. 级联字段提交结构需要再次确认

文档写：

- `CASCADER`：非空时必须是一维数组路径。
- `MULTICASCADER`：非空时必须是二维数组路径。

需要后端确认：

- 前端提交给后端的 `CASCADER` 值是否必须是数组路径，例如 `["beijing", "chaoyang"]`？
- 是否不再支持逗号字符串，例如 `"beijing,chaoyang"`？
- `MULTICASCADER` 是否固定为二维数组，例如 `[["beijing", "chaoyang"], ["shanghai", "pudong"]]`？

建议后端按数组结构校验，不再按逗号字符串处理。

## 11. STATIC 模式下 optionSource 是否可以不保存

文档说明 `optionSource` 为空等价于 `STATIC`。

需要后端确认：

- 静态选项字段保存时，是否允许不传 `optionSource`？
- 后端返回历史数据时，`optionSource` 为空是否由前端按 STATIC 处理？
- 是否避免为所有静态字段补 `{ "type": "STATIC" }`，减少数据噪音？

建议：静态字段可不保存 `optionSource`，空值即 STATIC。

## 12. 映射后的 children 是否只在有子节点时输出

文档中的统一结构包含：

```json
{
  "label": "显示文本",
  "value": "提交值",
  "visible": true,
  "children": []
}
```

需要后端确认：

- 叶子节点是否可以不输出 `children`？
- 是否只有存在子节点时才输出 `children`？

建议前端映射时：叶子节点不输出 `children`，避免部分级联组件把空 children 识别为可展开但无数据。

## 13. 第一版实现范围是否包含前端配置和运行态

第 15 节第一版推荐实现范围偏后端，前端实现范围没有完全展开。

需要后端确认第一版是否同步支持以下前端能力：

- 前端类型增加 `optionSource`。
- 字段编辑器支持配置选项来源：静态 / API。
- API 模式下配置 `url`、`method`、`params`、`mapping`。
- 字段渲染器支持远程加载 options。
- 设计预览和运行态填表共用远程 options 加载逻辑。
- 加载失败显示错误和重试入口。
- 联动 `OPTION` 可以叠加过滤远程 options。

## 14. 后端返回字段命名格式

数据库字段建议为 `option_source`，Java 字段为 `optionSource`。

需要后端确认接口 JSON 是否统一返回 camelCase：

```json
{
  "optionSource": {
    "type": "API"
  }
}
```

前端建议只按 `optionSource` 读取和提交。

## 15. 总结：建议后端优先答复的问题

为了前端能开始实现，建议后端优先确认以下 6 点：

1. `OPTION` 对远程选项的语义和优先级。
2. `url` 是否只允许同源相对路径。
3. 第一版是否移除或禁用 `headers`。
4. `params` 在 GET/POST 下的请求语义。
5. `mapping` 路径语法，特别是根数组表示方式。
6. API 模式下默认值和提交值是否都只校验结构，不校验存在性。
