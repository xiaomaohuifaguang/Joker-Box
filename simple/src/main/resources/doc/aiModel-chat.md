# AI 聊天接口

> 作者：小猫会发光
> 日期：2026-05-07

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/ai/chat` |
| 请求方式 | `POST` |
| Content-Type | `application/json` |
| Tag | ai接口 |
| Operation | 聊天 |
| Service | `AiChatService#chat` |

> 业务流程：
> 1. 根据 `model`（AiModel ID）查询模型配置；
> 2. 校验模型存在，否则抛 `RuntimeException("模型不存在")`；
> 3. AES 解密模型 `apiKey`；
> 4. 调用外部 OpenAI 兼容接口 `/v1/chat/completions`；
> 5. 非流式：等待完整响应，包装为 `HttpResult<ChatResponse>` 返回；
> 6. 流式：通过 `SseEmitter` 逐条推送 `data` 事件，最后推送 `[DONE]`。

---

## 请求参数

### ChatParam

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| model | String | 是 | 模型 ID，对应 `cat_ai_model` 表的 `id` |
| messages | List<Message> | 是 | 消息列表 |
| stream | boolean | 否 | 是否流式响应，默认 `false` |

### Message

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| content | String | 是 | 消息内容 |
| reasoning_content | String | 否 | 思考内容（如 DeepSeek 推理模型返回） |
| role | String | 是 | 角色，如 `system`、`user`、`assistant` |
| name | String | 否 | 参与者名称，用于区分同一角色的不同参与者 |

### 请求示例

#### 非流式

```json
{
  "model": "abc123",
  "messages": [
    {
      "role": "system",
      "content": "你是一个有帮助的助手"
    },
    {
      "role": "user",
      "content": "你好"
    }
  ],
  "stream": false
}
```

#### 流式

```json
{
  "model": "abc123",
  "messages": [
    {
      "role": "user",
      "content": "讲个笑话"
    }
  ],
  "stream": true
}
```

---

## 响应数据

本接口统一返回 `SseEmitter`，无论 `stream` 取值，均通过 **SSE (Server-Sent Events)** 格式输出。

### 非流式响应

发送一次 `data` 事件后自动 `complete()`。

```
data: {"code":200,"data":{...},"msg":"请求成功","timestamp":1715059200000}

```

### 流式响应

逐条推送 `data` 事件，最后推送 `[DONE]`。

```
data: {"code":200,"data":{"id":"chat-1","choices":[{"delta":{"content":"你"}}]},"msg":"请求成功","timestamp":1715059200000}

data: {"code":200,"data":{"id":"chat-1","choices":[{"delta":{"content":"好"}}]},"msg":"请求成功","timestamp":1715059200000}

data: [DONE]

```

### ChatResponse 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | String | 对话 ID |
| object | String | 对象类型，如 `chat.completion` |
| created | String | 创建时间戳 |
| model | String | 使用的模型名称 |
| system_fingerprint | String | 系统指纹 |
| choices | List<Choice> | 生成结果列表 |
| usage | Usage | Token 用量统计（非流式末尾返回） |

### HttpResult 包装

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | long | 状态码，`200` 成功 |
| data | ChatResponse | OpenAI 兼容响应体 |
| msg | String | 响应消息 |
| timestamp | long | 服务端时间戳（毫秒） |

---

## 响应示例

### 非流式成功

```json
{
  "code": 200,
  "msg": "请求成功",
  "timestamp": 1715059200000,
  "data": {
    "id": "chatcmpl-abc123",
    "object": "chat.completion",
    "created": "1715059200",
    "model": "gpt-4",
    "system_fingerprint": "fp_abc",
    "choices": [
      {
        "index": 0,
        "message": {
          "role": "assistant",
          "content": "你好！有什么可以帮助你的吗？"
        },
        "finish_reason": "stop"
      }
    ],
    "usage": {
      "prompt_tokens": 20,
      "completion_tokens": 15,
      "total_tokens": 35
    }
  }
}
```

### 流式成功（SSE 片段）

```
data: {"code":200,"data":{"id":"chatcmpl-abc123","choices":[{"index":0,"delta":{"role":"assistant"}}]},"msg":"请求成功","timestamp":1715059200000}

data: {"code":200,"data":{"id":"chatcmpl-abc123","choices":[{"index":0,"delta":{"content":"你好"}}]},"msg":"请求成功","timestamp":1715059200001}

data: {"code":200,"data":{"id":"chatcmpl-abc123","choices":[{"index":0,"delta":{"content":"！"}}]},"msg":"请求成功","timestamp":1715059200002}

data: [DONE]

```

---

## 异常情况

| 触发条件 | 异常 | 说明 |
| --- | --- | --- |
| `model` 对应 AiModel 不存在 | `RuntimeException("模型不存在")` | 由全局异常处理转换为失败响应 |
| 外部 AI 接口调用失败（网络、鉴权等） | `IOException` | 流式模式下通过 SSE `event: error` 推送；非流式下 `SseEmitter` 调用 `completeWithError` |
| 未登录或 Token 失效 | `AccessDeniedException` / `AuthenticationException` | 由 Spring Security 统一拦截，返回 401/403 |
