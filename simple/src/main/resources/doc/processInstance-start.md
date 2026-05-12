# 流程实例 - 发起流程接口

> 作者：小猫会发光
> 日期：2026-05-09

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/processInstance/start` |
| 请求方式 | `POST` |
| Content-Type | `application/x-www-form-urlencoded`（`@RequestParam`） |
| Tag | 流程实例 |
| Operation | 发起流程 |
| Service | `ProcessInstanceService#start` |
| 事务 | `@Transactional(rollbackFor = Exception.class)` |

> 业务流程：
> 1. 校验自建流程定义存在；
> 2. 校验流程定义 `status = '1'`（已发布）；
> 3. 校验当前用户已登录（`SecurityUtils.getLoginUser()`）；
> 4. 调用 Flowable `runtimeService.startProcessInstanceByKey(processKey)` 真正启动；
> 5. 生成流程编号 `code`（Redis 原子自增，格式 `LC-YYYYMMDD-NNNN`）；
> 6. 落一条 `cat_process_instance`：title = 入参 / null，code = 生成的编号，processStatus = `1`，createBy = 当前登录人；
> 7. 落一条 `cat_process_handle_info`：handleType = `0`（APPLY 申请）；
> 8. 整体在事务内，任一环节失败均回滚。

---

## 请求参数

| 字段 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| processDefinitionId | query / form | Integer | 是 | 自建 `cat_process_definition` 主键，需是已发布(`status='1'`)状态 |
| title | query / form | String | 否 | 流程标题，可为空 |

> 通常配合 `/processDefinition/deployList` 获取的 `id` 使用。

### 请求示例

```http
POST /processInstance/start?processDefinitionId=48&title=请假申请
```

或表单：

```http
POST /processInstance/start
Content-Type: application/x-www-form-urlencoded

processDefinitionId=48&title=请假申请
```

---

## 响应数据（HttpResult\<ProcessInstance\>）

### HttpResult 包装

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | long | 状态码，`200` 成功，`-1` 失败 |
| data | ProcessInstance | 新建的自建流程实例 |
| msg | String | 响应消息 |
| timestamp | long | 服务端时间戳（毫秒） |
| refreshToken | String | 续签 token，可为空 |

### ProcessInstance 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Integer | 自建主键，由 MyBatis-Plus 自增回填 |
| processDefinitionId | Integer | 入参回写 |
| title | String | 流程标题，入参回写，可为 `null` |
| code | String | 流程编号，格式 `LC-YYYYMMDD-NNNN`，如 `LC-20260509-0001` |
| processDefinitionName | String | 非 DB 字段，本接口不回填 |
| processInstanceId | String | Flowable 引擎生成的实例 id |
| processStatus | String | `1` 审批中（ACTIVE） |
| deleted | String | `0` |
| createBy | String | 当前登录人 userId |
| createByName | String | 非 DB 字段，本接口不回填 |
| createTime | String | 创建时间 `yyyy-MM-dd HH:mm:ss` |
| updateTime | String | 更新时间 `yyyy-MM-dd HH:mm:ss` |
| taskId | String | 非 DB 字段，本接口不回填 |

> 副作用：本调用同时往 `cat_process_handle_info` 写入一条 handleType=`0`（APPLY，申请）的记录，可通过 `ProcessHandleInfoMapper#selectDetailListByProcessInstanceId` 查询审批轨迹。

---

## 响应示例

### 成功

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

---

## 异常情况

| 触发条件 | 异常 | 说明 |
| --- | --- | --- |
| `processDefinitionId` 在 `cat_process_definition` 找不到 | `IllegalArgumentException("流程定义不存在: {id}")` | 由全局异常处理转换为失败响应 |
| 流程定义状态不是 `1`（已发布） | `IllegalStateException("流程定义未发布, 无法发起: {id}")` | 草稿 / 已停用都会被拦截 |
| 当前未登录 | `IllegalStateException("当前未登录, 无法发起流程")` | `SecurityUtils.getLoginUser()` 为空 |
| Flowable `startProcessInstanceByKey` 失败 | Flowable 抛出的异常（如 `processKey` 未部署） | 整个事务回滚，自建实例 / 处理记录都不会落库 |
| Redis 故障导致编号冲突 | `DuplicateKeyException`（唯一索引 `uk_code` 兜底） | 编号重复时会抛异常，外层可重试 |
