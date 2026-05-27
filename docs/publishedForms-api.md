# 已发布表单列表接口 - publishedForms

## 接口信息

| 项目 | 值 |
|---|---|
| URL | `/dynamicForm/publishedForms` |
| Method | `POST` |
| 请求参数 | 无 |
| 响应类型 | `HttpResult<List<DynamicFormPublishedVersion>>` |

## 用途

供流程定义绑定表单时使用，返回所有已发布表单及其历史版本，用于填充表单下拉框和版本选择器。

## 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "formId": "form_main_001",
      "formName": "请假申请表单",
      "latestVersion": "3",
      "versions": [
        { "version": "1", "publishTime": "2026-05-20 10:00:00" },
        { "version": "2", "publishTime": "2026-05-22 14:30:00" },
        { "version": "3", "publishTime": "2026-05-25 09:00:00" }
      ]
    },
    {
      "formId": "form_expense_002",
      "formName": "报销申请表单",
      "latestVersion": "1",
      "versions": [
        { "version": "1", "publishTime": "2026-05-21 16:00:00" }
      ]
    }
  ]
}
```

## 响应字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `formId` | String | 表单 ID，绑定表单时使用此值 |
| `formName` | String | 表单名称，下拉框展示用 |
| `latestVersion` | String | 最新发布版本号，绑定表单时默认使用此值作为 `formVersion` |
| `versions` | Array | 该表单的所有发布版本 |
| `versions[].version` | String | 版本号 |
| `versions[].publishTime` | String | 发布时间 |

## 前端交互建议

1. **下拉框选项**：展示 `formName`，值为 `formId`
2. **默认版本**：选中表单后，自动将 `latestVersion` 填入 `formVersion`
3. **切换历史版本**：展开 `versions` 列表供用户选择，选择后将对应 `version` 填入 `formVersion`
4. **版本锁定**：`formVersion` 随流程定义保存，表单后续发布新版本不会自动更新已绑定的流程