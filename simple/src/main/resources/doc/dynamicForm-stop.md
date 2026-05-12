# 动态表单 - 停用接口

> 作者：小猫会发光
> 日期：2026-05-09

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/dynamicForm/stop` |
| 请求方式 | `POST` |
| Content-Type | `application/x-www-form-urlencoded`（`@RequestParam`） |
| Service | `DynamicFormService#stop` |
| 事务 | `@Transactional(rollbackFor = Exception.class)` |

> 将最新版本复制为 DRAFT，状态变为已停用。可继续编辑后重新发布。

---

## 请求参数

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| formId | String | 是 | 表单ID |

### 请求示例

```http
POST /dynamicForm/stop?formId=abc123
```

---

## 业务流程

1. 校验表单存在且为已发布状态
2. 物理删除现有 DRAFT 数据（如果有）
3. 将最新版本的字段、分组、联动规则复制为 DRAFT
4. 表单状态变为 `-1`（已停用）

---

## 响应数据（HttpResult<Void>）

---

## 异常情况

| 触发条件 | 说明 |
| --- | --- |
| 表单不存在 | 返回 false |
| 非已发布状态 | 只有 status=1 可停用 |
| 非本人创建 | 返回 false |
