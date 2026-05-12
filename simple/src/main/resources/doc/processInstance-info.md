# 流程实例 - 详情接口

> 作者：小猫会发光
> 日期：2026-05-09

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/processInstance/info` |
| 请求方式 | `POST` |
| Content-Type | `application/x-www-form-urlencoded`（`@RequestParam`） |
| Tag | 流程实例 |
| Operation | 详情 |
| Service | `ProcessInstanceService#info` |

> 说明：直接按主键 `id` 查 `cat_process_instance`，未做权限校验和非数据库字段回填（`processDefinitionName` / `createByName` / `taskId` 仍为 `null`）。如需脱敏或越权拦截，请在网关或拦截器层面叠加。

---

## 请求参数

| 字段 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | query / form | Integer | 是 | 自建 `cat_process_instance` 主键 |

### 请求示例

```http
POST /processInstance/info?id=101
```

或表单：

```http
POST /processInstance/info
Content-Type: application/x-www-form-urlencoded

id=101
```

---

## 响应数据（HttpResult\<ProcessInstance\>）

### HttpResult 包装

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | long | 状态码，`200` 成功 |
| data | ProcessInstance | 实例详情，未命中时为 `null` |
| msg | String | 响应消息 |
| timestamp | long | 服务端时间戳（毫秒） |
| refreshToken | String | 续签 token，可为空 |

### ProcessInstance 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Integer | 自建主键 |
| processDefinitionId | Integer | 自建流程定义 id（cat_process_definition.id） |
| title | String | 流程标题，可为 `null` |
| code | String | 流程编号，格式 `LC-YYYYMMDD-NNNN`；草稿状态为 `null` |
| processDefinitionName | String | 流程定义名称（非 DB 字段，本接口不回填） |
| processInstanceId | String | Flowable 引擎流程实例 id |
| processStatus | String | 流程状态，见下方枚举 |
| deleted | String | 逻辑删除 0 否 / 1 是 |
| createBy | String | 创建人（申请人）userId |
| createByName | String | 创建人昵称（非 DB 字段，本接口不回填） |
| createTime | String | 创建时间 `yyyy-MM-dd HH:mm:ss` |
| updateTime | String | 更新时间 `yyyy-MM-dd HH:mm:ss` |
| taskId | String | 当前任务 id（非 DB 字段，本接口不回填） |

### processStatus 枚举（ProcessStatusEnum）

| 取值 | 含义 |
| --- | --- |
| `0` | 草稿（默认值） |
| `1` | 审批中（ACTIVE） |
| `10` | 已完成（COMPLETED） |
| `11` | 已终止（TERMINATED） |
| `20` | 已挂起（SUSPENDED） |
| `21` | 待办（TODO，运行时使用，不入库） |

---

## 响应示例

### 命中

```json
{
  "code": 200,
  "msg": "请求成功",
  "timestamp": 1714896000000,
  "refreshToken": null,
  "data": {
    "id": 101,
    "processDefinitionId": 48,
    "title": "请假申请",
    "code": "LC-20260509-0001",
    "processDefinitionName": null,
    "processInstanceId": "9c1f2a90-aabb-11ee-9f08-00ffe9eaf737",
    "processStatus": "1",
    "deleted": "0",
    "createBy": "1",
    "createByName": null,
    "createTime": "2026-05-09 09:30:00",
    "updateTime": "2026-05-09 09:30:00",
    "taskId": null
  }
}
```

### 未命中

```json
{
  "code": 200,
  "msg": "请求成功",
  "timestamp": 1714896000000,
  "refreshToken": null,
  "data": null
}
```