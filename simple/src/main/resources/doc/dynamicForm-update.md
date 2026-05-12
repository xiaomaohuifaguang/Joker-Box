# 动态表单 - 修改接口

> 作者：小猫会发光
> 日期：2026-05-09

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/dynamicForm/update` |
| 请求方式 | `POST` |
| Content-Type | `application/json`（`@RequestBody`） |
| Service | `DynamicFormService#update` |
| 事务 | `@Transactional(rollbackFor = Exception.class)` |

> 修改草稿或已停用表单。会物理删除旧 DRAFT 版本的字段、分组、联动规则，然后重新插入。

---

## 请求参数（DynamicForm）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | String | 是 | 表单ID |
| name | String | 否 | 新名称 |
| description | String | 否 | 新描述 |
| groups | List<DynamicFormFieldGroup> | 否 | 新分组（覆盖旧 DRAFT） |
| formFields | List<DynamicFormField> | 否 | 新字段（覆盖旧 DRAFT） |
| linkageRules | List<DynamicFormLinkageRule> | 否 | 新联动规则（覆盖旧 DRAFT） |

> 只有草稿（status=0）和已停用（status=-1）可修改。

---

## 请求示例

```http
POST /dynamicForm/update
Content-Type: application/json

{
  "id": "abc123",
  "name": "请假申请单V2",
  "groups": [
    {
      "name": "基本信息",
      "sort": 1,
      "collapsed": "0",
      "fields": [
        {
          "fieldId": "field_name",
          "title": "姓名",
          "type": "INPUT",
          "required": "1",
          "span": 12
        }
      ]
    }
  ]
}
```

---

## 响应数据（HttpResult<Void>）

---

## 异常情况

| 触发条件 | 说明 |
| --- | --- |
| 表单不存在 | 返回 false |
| 表单已发布（status=1） | 不可直接修改，需先停用 |
| 非本人创建 | `IllegalStateException("无权操作他人表单")` |
