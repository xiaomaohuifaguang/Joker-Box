# 流程实例接口文档

## 1. 分页查询

`POST /processInstance/queryPage`

### 请求参数

```json
{
  "current": 1,
  "size": 10,
  "type": "2",
  "search": "",
  "processDefinitionId": null,
  "processStatus": null,
  "startTime": null,
  "endTime": null
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| current | long | 是 | 页码，从 1 开始 |
| size | long | 是 | 每页条数 |
| type | string | 是 | 查询类型，见下表 |
| search | string | 否 | 模糊搜索，匹配标题或流程编号 |
| processDefinitionId | integer | 否 | 按流程定义筛选 |
| processStatus | string | 否 | 按流程状态筛选，见状态枚举 |
| startTime | string | 否 | 创建时间起始，格式 `yyyy-MM-dd HH:mm:ss` |
| endTime | string | 否 | 创建时间结束，格式 `yyyy-MM-dd HH:mm:ss` |

> `userId` 由服务端自动填充，前端无需传递。

### type 查询类型

| 值 | 含义 | 说明 | 默认排序 |
|----|------|------|---------|
| 0 | 草稿 | 当前用户创建且未提交的草稿 | updateTime DESC |
| 1 | 我发起的（进行中） | 当前用户创建且正在审批中的流程 | updateTime DESC |
| 2 | 待办 | 分配给当前用户的活动任务，每个任务一行 | 任务创建时间 DESC |
| 3 | 待认领 | 当前用户作为候选人但未签收的任务 | 任务创建时间 DESC |
| 4 | 我已办的 | 当前用户历史处理过的任务，**不传 startTime 默认查近 6 个月** | 任务结束时间 DESC |
| 5 | 我发起的（全部） | 当前用户创建的所有流程（含已完成/已终止） | updateTime DESC |

### 各 type 支持的筛选字段

| 筛选字段 | 0 草稿 | 1 发起进行中 | 2 待办 | 3 待认领 | 4 已办 | 5 发起全部 |
|---------|--------|------------|--------|---------|--------|-----------|
| search | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| processDefinitionId | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| processStatus | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ |
| startTime | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ |
| endTime | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ |

> - type=2/3 的流程状态由任务运行时决定，processStatus 筛选无意义故不支持
> - type=4 的 startTime/endTime 筛选的是**任务处理时间**而非流程创建时间；不传 startTime 时默认查近 6 个月以保证性能
> - 传非法 type 值会返回空列表

### 响应

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "processDefinitionId": 5,
        "title": "请假申请",
        "code": "LC-20260525-0001",
        "processInstanceId": "25001",
        "processStatus": "1",
        "createBy": "1",
        "createByName": "张三",
        "createTime": "2026-05-25 10:00:00",
        "updateTime": "2026-05-25 11:00:00",
        "taskId": "25010",
        "taskName": "部门经理审批"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | integer | 流程实例 ID |
| processDefinitionId | integer | 流程定义 ID |
| title | string | 流程标题 |
| code | string | 流程编号，格式 `LC-yyyyMMdd-NNNN` |
| processInstanceId | string | Flowable 引擎实例 ID |
| processStatus | string | 流程状态，见状态枚举 |
| createBy | string | 创建人 ID |
| createByName | string | 创建人昵称 |
| createTime | string | 创建时间 |
| updateTime | string | 更新时间 |
| taskId | string | 当前任务 ID（type=2/3/4 时有值，其余为 null） |
| taskName | string | 当前任务名称（type=2/3/4 时有值，其余为 null） |

---

## 2. 流程状态枚举

| 值 | 含义 | 说明 |
|----|------|------|
| 0 | 草稿 | 尚未提交到引擎 |
| 1 | 审批中 | 正在流程中流转 |
| 10 | 已完成 | 流程正常走完 |
| 11 | 已终止 | 被驳回/拒绝 |
| 20 | 已挂起 | 流程被暂停 |

> 前端列表展示建议：
> - type=0 草稿列表：状态固定为草稿，可操作「编辑」「提交」「删除」
> - type=1 发起进行中：状态固定为审批中，可操作「催办」
> - type=2 待办：可操作「审批通过」「驳回」「退回」
> - type=3 待认领：可操作「认领」
> - type=4 已办：可操作「查看详情」
> - type=5 发起全部：根据 processStatus 展示不同标签，可操作「查看详情」

---

## 3. 前端 Tab 页推荐方案

```
┌──────────┬──────────┬──────────┬──────────┬──────────┐
│ 待办(2)  │ 待认领(3)│ 已办(4)  │ 我发起的 │ 草稿箱   │
│          │          │          │  (1/5)   │  (0)     │
└──────────┴──────────┴──────────┴──────────┴──────────┘
```

- **我发起的**可拆为两个子 Tab：「进行中」(type=1) 和「全部」(type=5)
- 待办/待认领 Tab 角标建议调用统计接口实时展示数量
- 已办 Tab 建议默认展示近 6 个月数据，提供时间范围筛选让用户主动扩大查询

---

## 4. 其他接口（未改动，供参考）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/processInstance/start` | POST | 发起流程，参数：`processDefinitionId`(必填), `title`(选填) |
| `/processInstance/info` | POST | 流程详情，参数：`id`(必填), `taskId`(选填) |
| `/processInstance/saveDraft` | POST | 保存草稿，参数：`id`(选填), `processDefinitionId`(必填), `title`(选填) |
| `/processInstance/claim` | POST | 认领任务，参数：`ProcessHandleParam` |
| `/processInstance/pass` | POST | 审批通过，参数：`ProcessHandleParam` |
| `/processInstance/reject` | POST | 拒绝（终止流程），参数：`ProcessHandleParam` |
| `/processInstance/back` | POST | 驳回到上一节点，参数：`ProcessHandleParam` |
| `/processInstance/availableBackTargets` | POST | 查询可驳回目标节点，参数：`taskId` |
| `/processInstance/backConfig` | POST | 读取驳回配置，参数：`taskId` |

### ProcessHandleParam

```json
{
  "processInstanceId": 1,
  "taskId": "25010",
  "remark": "同意",
  "targetNodeId": "activity-abc"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| processInstanceId | integer | 是 | 流程实例 ID |
| taskId | string | 是 | 任务 ID |
| remark | string | 否 | 审批意见 |
| targetNodeId | string | 驳回选择时 | 驳回目标节点 ID（backType=CHOOSE 时必填） |