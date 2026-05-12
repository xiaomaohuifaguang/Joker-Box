# 流程定义 - 已部署流程列表接口

> 作者：小猫会发光
> 日期：2026-05-05

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/processDefinition/deployList` |
| 请求方式 | `POST` |
| Content-Type | 无（无入参） |
| Tag | 流程引擎 |
| Operation | 已部署流程列表 |
| Service | `ProcessDefinitionService#deployList` |

> 说明：返回所有 `cat_process_definition.status = '1'`（已发布）的流程定义。配合 `/processInstance/start` 使用：前端在发起流程下拉框中选择列表项，再用 `id` 调用 `start` 启动实例。

---

## 请求参数

无。

### 请求示例

```http
POST /processDefinition/deployList
```

---

## 响应数据（HttpResult\<List\<ProcessDefinition\>\>）

### HttpResult 包装

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | long | 状态码，`200` 成功 |
| data | List\<ProcessDefinition\> | 已发布流程定义列表，可能为空数组 |
| msg | String | 响应消息 |
| timestamp | long | 服务端时间戳（毫秒） |
| refreshToken | String | 续签 token，可为空 |

### ProcessDefinition 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Integer | 自建流程定义主键 |
| processKey | String | 流程定义 key（对应 Flowable `ACT_RE_PROCDEF.KEY_`） |
| processCategory | String | 流程分类 |
| processName | String | 流程定义名称 |
| processDescription | String | 流程描述 |
| version | String | 使用版本 |
| status | String | 流程状态：本接口固定返回 `1`（已发布） |
| createBy | String | 创建人 userId |
| createByName | String | 创建人昵称（非 DB 字段，本接口不回填） |
| createTime | String | 创建时间 `yyyy-MM-dd HH:mm:ss` |
| updateTime | String | 更新时间 `yyyy-MM-dd HH:mm:ss` |
| deleted | String | 逻辑删除 0 否 / 1 是 |
| xmlStr | String | BPMN XML 内容（非 DB 字段，列表场景默认 null） |
| rawData | Object | LogicFlow 原始 data（非 DB 字段，列表场景默认 null） |

### status 枚举

| 取值 | 含义 |
| --- | --- |
| `0` | 草稿 |
| `1` | 已发布（本接口仅返回此状态） |
| `-1` | 已停用 |

---

## 响应示例

### 命中数据

```json
{
  "code": 200,
  "msg": "请求成功",
  "timestamp": 1714896000000,
  "refreshToken": null,
  "data": [
    {
      "id": 48,
      "processKey": "Process_p9q6vjdkwp",
      "processCategory": "请假",
      "processName": "请假流程",
      "processDescription": "员工请假审批",
      "version": "1",
      "status": "1",
      "createBy": "1",
      "createByName": null,
      "createTime": "2026-04-20 10:00:00",
      "updateTime": "2026-04-20 10:05:00",
      "deleted": "0",
      "xmlStr": null,
      "rawData": null
    },
    {
      "id": 49,
      "processKey": "Process_a1b2c3",
      "processCategory": "报销",
      "processName": "费用报销",
      "processDescription": "差旅 / 餐饮报销",
      "version": "2",
      "status": "1",
      "createBy": "1",
      "createByName": null,
      "createTime": "2026-04-22 14:30:00",
      "updateTime": "2026-05-01 09:00:00",
      "deleted": "0",
      "xmlStr": null,
      "rawData": null
    }
  ]
}
```

### 无可发起的流程

```json
{
  "code": 200,
  "msg": "请求成功",
  "timestamp": 1714896000000,
  "refreshToken": null,
  "data": []
}
```
