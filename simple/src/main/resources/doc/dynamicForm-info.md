# 动态表单 - 详情接口

> 作者：小猫会发光
> 日期：2026-05-09

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/dynamicForm/info` |
| 请求方式 | `POST` |
| Content-Type | `application/json`（`@RequestBody`） |
| Service | `DynamicFormService#info` |

---

## 请求参数

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | String | 是 | 表单ID |
| version | String | 否 | 版本号，不传返回最新版本 |

### 请求示例

```http
POST /dynamicForm/info
Content-Type: application/json

{
  "id": "abc123",
  "version": "2"
}
```

---

## 响应数据（HttpResult<DynamicForm>）

### 有分组时返回结构

```json
{
  "code": 200,
  "msg": "请求成功",
  "data": {
    "id": "form_xxx",
    "name": "请假申请单",
    "version": "3",
    "status": "1",
    "groups": [
      {
        "id": "g_xxx",
        "name": "基本信息",
        "description": "请填写申请人基本信息",
        "sort": 1,
        "collapsed": "0",
        "fields": [
          {
            "id": "f_xxx",
            "fieldId": "field_name",
            "groupId": "g_xxx",
            "title": "姓名",
            "type": "INPUT",
            "required": "1",
            "span": 12,
            "sort": 1
          },
          {
            "id": "f_yyy",
            "fieldId": "field_dept",
            "groupId": "g_xxx",
            "title": "部门",
            "type": "SELECT",
            "required": "1",
            "span": 12,
            "sort": 2,
            "options": [
              { "label": "技术部", "value": "tech" }
            ]
          }
        ]
      },
      {
        "id": "g_yyy",
        "name": "请假明细",
        "sort": 2,
        "collapsed": "0",
        "fields": [
          {
            "id": "f_zzz",
            "fieldId": "field_type",
            "groupId": "g_yyy",
            "title": "请假类型",
            "type": "RADIO",
            "required": "1",
            "options": [
              { "label": "事假", "value": "personal" },
              { "label": "病假", "value": "sick" }
            ]
          }
        ]
      }
    ],
    "linkageRules": [
      {
        "id": "rule_xxx",
        "name": "病假显示医院证明",
        "targetFieldId": "field_hospital",
        "actionType": "SHOW",
        "enable": true,
        "conditionTree": [
          {
            "nodeType": "CONDITION",
            "triggerFieldId": "field_type",
            "triggerCondition": "EQ",
            "triggerValue": "sick"
          }
        ]
      }
    ],
    "formFields": null
  }
}
```

### 无分组时返回结构（兼容存量）

```json
{
  "code": 200,
  "data": {
    "id": "form_xxx",
    "name": "简单表单",
    "version": "1",
    "status": "1",
    "groups": null,
    "formFields": [
      {
        "id": "f_xxx",
        "fieldId": "field_1",
        "groupId": null,
        "title": "姓名",
        "type": "INPUT",
        "required": "1"
      }
    ],
    "linkageRules": []
  }
}
```

---

## 前端判断方式

```js
const form = res.data;
if (form.groups && form.groups.length > 0) {
  // 按分组渲染折叠面板 / 卡片
  form.groups.forEach(group => {
    console.log(group.name, group.fields);
  });
} else if (form.formFields) {
  // 按平铺列表渲染
  form.formFields.forEach(field => {
    console.log(field.title);
  });
}
```
