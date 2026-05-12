# 流程实例 - 保存草稿接口

> 作者：小猫会发光
> 日期：2026-05-09

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/processInstance/saveDraft` |
| 请求方式 | `POST` |
| Content-Type | `application/x-www-form-urlencoded`（`@RequestParam`） |
| Tag | 流程实例 |
| Operation | 保存草稿 |
| Service | `ProcessInstanceService#saveDraft` |
| 事务 | `@Transactional(rollbackFor = Exception.class)` |

> 业务流程：
> 1. 校验流程定义存在（`cat_process_definition`），草稿不校验是否已发布；
> 2. 校验当前用户已登录（`SecurityUtils.getLoginUser()`）；
> 3. 若传了 `id`：更新现有草稿，校验必须是草稿状态（`processStatus = '0'`）且是当前用户的草稿，同时更新 `title`；
> 4. 若未传 `id`：新建草稿，`processStatus = '0'`（DRAFT），不启动 Flowable 引擎，写入 `title`；
> 5. 整体在事务内，任一环节失败均回滚。

---

## 请求参数

| 字段 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | query / form | Integer | 否 | 草稿流程实例id，传则更新，不传则新建 |
| processDefinitionId | query / form | Integer | 是 | 自建 `cat_process_definition` 主键 |
| title | query / form | String | 否 | 流程标题，可为空 |

> 新建草稿时无需传 `id`；更新草稿时需传入此前保存草稿返回的 `id`。

### 请求示例

**新建草稿**

```http
POST /processInstance/saveDraft?processDefinitionId=48&title=请假申请
```

或表单：

```http
POST /processInstance/saveDraft
Content-Type: application/x-www-form-urlencoded

processDefinitionId=48&title=请假申请
```

**更新草稿**

```http
POST /processInstance/saveDraft?id=101&processDefinitionId=48&title=请假申请-修改版
```

或表单：

```http
POST /processInstance/saveDraft
Content-Type: application/x-www-form-urlencoded

id=101&processDefinitionId=48&title=请假申请-修改版
```

---

## 响应数据（HttpResult\<ProcessInstance\>）

### HttpResult 包装

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | long | 状态码，`200` 成功，`-1` 失败 |
| data | ProcessInstance | 保存后的流程实例 |
| msg | String | 响应消息 |
| timestamp | long | 服务端时间戳（毫秒） |
| refreshToken | String | 续签 token，可为空 |

### ProcessInstance 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Integer | 自建主键，新建时由 MyBatis-Plus 自增回填 |
| processDefinitionId | Integer | 入参回写 |
| title | String | 流程标题，入参回写，可为 `null` |
| code | String | 草稿状态为 `null`，正式发起后生成 |
| processDefinitionName | String | 非 DB 字段，本接口不回填 |
| processInstanceId | String | Flowable 引擎实例 id，草稿状态为 `null` |
| processStatus | String | `0` 草稿（DRAFT） |
| deleted | String | `0` |
| createBy | String | 当前登录人 userId |
| createByName | String | 非 DB 字段，本接口不回填 |
| createTime | String | 创建时间 `yyyy-MM-dd HH:mm:ss` |
| updateTime | String | 更新时间 `yyyy-MM-dd HH:mm:ss` |
| taskId | String | 非 DB 字段，本接口不回填 |

> 草稿不启动 Flowable 引擎，因此 `processInstanceId` 为 `null`。草稿不生成编号，因此 `code` 为 `null`。后续可通过 `/processInstance/start` 将草稿真正发起为流程。

---

## 响应示例

### 成功（新建草稿）

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
    "code": null,
    "processDefinitionName": null,
    "processInstanceId": null,
    "processStatus": "0",
    "deleted": "0",
    "createBy": "1",
    "createByName": null,
    "createTime": "2026-05-09 09:30:00",
    "updateTime": "2026-05-09 09:30:00",
    "taskId": null
  }
}
```

### 成功（更新草稿）

```json
{
  "code": 200,
  "msg": "请求成功",
  "timestamp": 1714896000000,
  "refreshToken": null,
  "data": {
    "id": 101,
    "processDefinitionId": 48,
    "title": "请假申请-修改版",
    "code": null,
    "processDefinitionName": null,
    "processInstanceId": null,
    "processStatus": "0",
    "deleted": "0",
    "createBy": "1",
    "createByName": null,
    "createTime": "2026-05-09 09:00:00",
    "updateTime": "2026-05-09 09:30:00",
    "taskId": null
  }
}
```

---

## 异常情况

| 触发条件 | 异常 | 说明 |
| --- | --- | --- |
| `processDefinitionId` 在 `cat_process_definition` 找不到 | `IllegalArgumentException("流程定义不存在: {id}")` | 由全局异常处理转换为失败响应 |
| 当前未登录 | `IllegalStateException("当前未登录, 无法保存草稿")` | `SecurityUtils.getLoginUser()` 为空 |
| 更新时 `id` 在 `cat_process_instance` 找不到 | `IllegalArgumentException("草稿不存在: {id}")` | 由全局异常处理转换为失败响应 |
| 更新时流程实例不是草稿状态 | `IllegalStateException("该流程实例不是草稿状态, 无法更新: {id}")` | 只能更新 `processStatus = '0'` 的记录 |
| 更新时非当前用户创建的草稿 | `IllegalStateException("无权更新他人草稿: {id}")` | 只能更新自己创建的草稿 |
