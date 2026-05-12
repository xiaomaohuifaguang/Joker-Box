# 动态表单 - 发布接口

> 作者：小猫会发光
> 日期：2026-05-09

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/dynamicForm/deploy` |
| 请求方式 | `POST` |
| Content-Type | `application/x-www-form-urlencoded`（`@RequestParam`） |
| Service | `DynamicFormService#deploy` |
| 事务 | `@Transactional(rollbackFor = Exception.class)` |

> 将 DRAFT 版本的字段（含分组）、联动规则复制为新版本，清空 DRAFT，状态变为已发布。

---

## 请求参数

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| formId | String | 是 | 表单ID |

### 请求示例

```http
POST /dynamicForm/deploy?formId=abc123
```

---

## 业务流程

1. 校验表单存在且为草稿/停用状态
2. 生成新版本号（原版本 +1，原为空则版本为 1）
3. 将 DRAFT 版本的字段、分组、联动规则复制到新版本
4. 物理删除 DRAFT 版本的数据
5. 表单状态变为 `1`（已发布）

---

## 响应数据（HttpResult<Void>）

---

## 异常情况

| 触发条件 | 说明 |
| --- | --- |
| 表单不存在 | 返回 false |
| 表单已发布 | 只有 status=0/-1 可发布 |
| 非本人创建 | 返回 false |
