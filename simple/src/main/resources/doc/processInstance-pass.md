# 流程实例 - 审批通过接口

> 作者：小猫会发光
> 日期：2026-05-05

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/processInstance/pass` |
| 请求方式 | `POST` |
| Content-Type | `application/json`（`@RequestBody`） |
| Tag | 流程实例 |
| Operation | 审批通过 |
| Service | `ProcessInstanceService#pass` |
| 事务 | `@Transactional(rollbackFor = Exception.class)` |

> 业务流程：
> 1. 校验自建流程实例存在（`cat_process_instance`）；
> 2. 校验当前用户已登录（`SecurityUtils.getLoginUser()`）；
> 3. 通过 Flowable `taskService.createTaskQuery().taskId(taskId).taskAssignee(userId)` 验证当前用户是否为该任务的办理人（已认领或被指派）；
> 4. 验证通过后调用 `taskService.complete(taskId)` 完成任务，流程自动流转到下一节点；
> 5. 落一条 `cat_process_handle_info`：handleType = `1`（PASS 通过），记录 taskId、taskName、handleUser、remark、handleTime；
> 6. 刷新流程实例状态（若流程已结束则标记为 COMPLETED）；
> 7. 整体在事务内，任一环节失败均回滚。

---

## 请求参数（ProcessHandleParam）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| processInstanceId | Integer | 是 | 自建 `cat_process_instance` 主键 |
| taskId | String | 是 | Flowable 任务 id（可通过待办列表等接口获取） |
| remark | String | 否 | 备注/审批意见，为空时默认存储 "通过" |

### 请求示例

```http
POST /processInstance/pass
Content-Type: application/json

{
  "processInstanceId": 101,
  "taskId": "abc123",
  "remark": "同意，没有问题"
}
```

---

## 响应数据（HttpResult\<Void\>）

### HttpResult 包装

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | long | 状态码，`200` 成功，`-1` 失败 |
| data | Void | 本接口无业务数据返回，为 `null` |
| msg | String | 响应消息 |
| timestamp | long | 服务端时间戳（毫秒） |
| refreshToken | String | 续签 token，可为空 |

> 副作用：本调用往 `cat_process_handle_info` 写入一条 handleType=`1`（PASS，通过）的记录，可通过 `ProcessHandleInfoMapper#selectDetailListByProcessInstanceId` 查询审批轨迹。

---

## 响应示例

### 成功

```json
{
  "code": 200,
  "msg": "请求成功",
  "timestamp": 1714896000000,
  "refreshToken": null,
  "data": null
}
```

---

## 异常情况

| 触发条件 | 异常 | 说明 |
| --- | --- | --- |
| `processInstanceId` 在 `cat_process_instance` 找不到 | `IllegalArgumentException("流程实例不存在: {id}")` | 由全局异常处理转换为失败响应 |
| 当前未登录 | `IllegalStateException("当前未登录, 无法审批通过")` | `SecurityUtils.getLoginUser()` 为空 |
| 当前用户不是该任务的办理人 | `IllegalStateException("当前用户没有该任务的办理权限, taskId: {taskId}")` | 通过 Flowable assignee 校验失败 |
| Flowable `complete` 失败 | Flowable 抛出的异常（如任务不存在、流程挂起等） | 整个事务回滚，处理记录不会落库 |