# 流程实例 - 分页查询接口

> 作者：小猫会发光
> 日期：2026-05-09

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/processInstance/queryPage` |
| 请求方式 | `POST` |
| Content-Type | `application/json` |
| Tag | 流程实例 |
| Operation | 分页 |
| Service | `ProcessInstanceService#queryPage` |

> 注意：服务端会用当前登录用户强制覆盖 `userId` 字段，前端无需传，传了也会被忽略；非法 `type` 会被归一为 `-1`，结果集为空。

---

## 请求参数（ProcessInstancePageParam）

继承自 `PageParam`。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| current | long | 否 | 页码，默认 1 |
| size | long | 否 | 页大小，默认 10 |
| search | String | 否 | 搜索关键字（保留字段，当前 SQL 暂未使用） |
| type | String | 是 | 查询类型，见下方枚举 |
| userId | String | - | 用户 id，**后端强制覆盖为当前登录人**，前端忽略 |

### type 枚举

| 取值 | 含义 | 数据来源 / 过滤条件 |
| --- | --- | --- |
| `0` | 草稿 | `cat_process_instance.process_status = '0' AND create_by = 当前用户` |
| `1` | 全部 | 当前用户创建过 或 在 `ACT_HI_TASKINST` 中以处理人身份参与过 |
| `2` | 待办 | `INNER JOIN ACT_RU_TASK ON task.ASSIGNEE_ = 当前用户` |
| `3` | 待认领 | 候选人(`ACT_RU_IDENTITYLINK.TYPE_='candidate'`)包含当前用户且 `task.ASSIGNEE_ IS NULL` |
| `-1`（含其它非法值） | 默认 | 后端兜底，返回空集 |

---

## 响应数据（HttpResult\<Page\<ProcessInstance\>\>）

### Page 包装

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| records | List\<ProcessInstance\> | 当前页记录 |
| total | long | 总记录数 |
| size | long | 页大小 |
| current | long | 当前页码 |

### ProcessInstance 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Integer | 自建流程实例主键（cat_process_instance.id） |
| processDefinitionId | Integer | 自建流程定义 id（cat_process_definition.id） |
| title | String | 流程标题，可为 `null` |
| code | String | 流程编号，格式 `LC-YYYYMMDD-NNNN`；草稿状态为 `null` |
| processDefinitionName | String | 流程定义名称（非数据库字段，按需回填） |
| processInstanceId | String | Flowable 引擎流程实例 id |
| processStatus | String | 流程状态，见下方枚举 |
| deleted | String | 逻辑删除 0 否 / 1 是 |
| createBy | String | 创建人（申请人）userId |
| createByName | String | 创建人昵称（非数据库字段，按需回填） |
| createTime | String | 创建时间 `yyyy-MM-dd HH:mm:ss` |
| updateTime | String | 更新时间 `yyyy-MM-dd HH:mm:ss` |
| taskId | String | 当前用户在该实例下命中的任务 id（来源 `ACT_RU_TASK.ID_`，非数据库字段） |

### processStatus 枚举（ProcessStatusEnum）

| 取值 | 含义 |
| --- | --- |
| `0` | 草稿（默认值，未真正提交到引擎） |
| `1` | 审批中（ACTIVE） |
| `10` | 已完成（COMPLETED） |
| `11` | 已终止（TERMINATED） |
| `20` | 已挂起（SUSPENDED） |
| `21` | 待办（TODO，不入库，仅运行时使用） |

---

## 请求示例

### 1. 待办列表

```json
{
  "current": 1,
  "size": 10,
  "type": "2"
}
```

### 2. 待认领列表

```json
{
  "current": 1,
  "size": 10,
  "type": "3"
}
```

### 3. 草稿列表

```json
{
  "current": 1,
  "size": 10,
  "type": "0"
}
```

### 4. 全部（我参与的）

```json
{
  "current": 1,
  "size": 10,
  "type": "1"
}
```

---

## 响应示例

### 命中数据

```json
{
  "code": 200,
  "msg": "请求成功",
  "timestamp": 1714896000000,
  "refreshToken": null,
  "data": {
    "records": [
      {
        "id": 101,
        "processDefinitionId": 48,
        "title": "请假申请",
        "code": "LC-20260509-0001",
        "processDefinitionName": "请假流程",
        "processInstanceId": "9c1f2a90-aabb-11ee-9f08-00ffe9eaf737",
        "processStatus": "1",
        "deleted": "0",
        "createBy": "1",
        "createByName": "管理员",
        "createTime": "2026-05-09 09:30:00",
        "updateTime": "2026-05-09 09:30:00",
        "taskId": "9c1f51a1-aabb-11ee-9f08-00ffe9eaf737"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

### 非法 type 兜底（结果为空）

请求 `{"type": "x"}` → `init()` 归一为 `-1` → SQL `WHERE 1=2`：

```json
{
  "code": 200,
  "msg": "请求成功",
  "timestamp": 1714896000000,
  "refreshToken": null,
  "data": {
    "records": [],
    "total": 0,
    "size": 10,
    "current": 1
  }
}
```

### 未登录

后端 `SecurityUtils.getLoginUser()` 为空时抛 `IllegalStateException("当前未登录, 无法查询流程实例")`，统一异常处理后返回错误状态码（具体由全局异常处理决定）。