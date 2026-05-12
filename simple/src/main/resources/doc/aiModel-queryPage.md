## 模型管理 - 分页查询

### 接口信息

| 项目 | 内容 |
|------|------|
| 接口地址 | `/ai/model/queryPage` |
| 请求方式 | `POST` |
| 接口说明 | 分页查询 AI 模型列表 |

---

### 请求参数

**Content-Type:** `application/json`

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| size | long | 否 | 页大小，默认 10 |
| current | long | 否 | 页码，默认 1 |
| search | string | 否 | 搜索关键词 |

#### 请求示例

```json
{
  "size": 10,
  "current": 1,
  "search": ""
}
```

---

### 响应参数

| 参数名 | 类型 | 说明 |
|--------|------|------|
| code | long | 状态码，成功为 200 |
| data | object | 分页数据 |
| ├─ records | array | 模型列表 |
| ├─ total | long | 总记录数 |
| ├─ size | long | 页大小 |
| ├─ current | long | 当前页码 |
| msg | string | 响应消息 |
| timestamp | long | 时间戳 |

#### AiModel 字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | string | ID |
| name | string | 名称 |
| model | string | 模型 |
| baseUrl | string | 基础 URL |
| apiKey | string | API 密钥（脱敏显示，仅展示前 2 位与后 2 位，中间以 `*` 替代） |
| description | string | 描述 |
| userId | string | 创建人 |
| createTime | string | 创建时间，格式 `yyyy-MM-dd HH:mm:ss` |

#### 响应示例

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": "abc123",
        "name": "GPT-4",
        "model": "gpt-4",
        "baseUrl": "https://api.openai.com",
        "apiKey": "sk******yz",
        "description": "OpenAI GPT-4",
        "userId": "user001",
        "createTime": "2025-01-15 10:30:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  },
  "msg": "success",
  "timestamp": 1715059200000
}
```