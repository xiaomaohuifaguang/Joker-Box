# common 模块

> Joker-Box 公共模块，提供各业务模块共享的实体类、工具类、常量定义与通用配置。

---

## 模块定位

`common` 是一个纯依赖库模块，不包含任何 Spring Boot 启动类或 Web 控制器。它封装了项目通用的数据结构与工具方法，供 `simple` 等上层模块引用。

---

## 目录结构

```
common/src/main/java/com/cat/common/
├── entity/                      # 实体类与 DTO
│   ├── ai/                      # AI 对话相关实体
│   │   ├── chat/                # 聊天消息、对话、请求参数
│   │   └── model/               # AI 模型配置实体
│   ├── auth/                    # 认证授权相关实体
│   │   ├── LoginInfo.java       # 登录信息
│   │   ├── LoginUser.java       # 登录用户
│   │   ├── User.java            # 用户实体
│   │   ├── Role.java            # 角色实体
│   │   ├── Org.java             # 组织架构
│   │   └── ...
│   ├── crawler/                 # 爬虫任务实体
│   ├── dynamicForm/             # 动态表单实体
│   │   ├── DynamicForm.java     # 表单定义
│   │   ├── DynamicFormField.java# 表单字段
│   │   ├── FormData.java        # 表单数据
│   │   └── ...
│   ├── file/                    # 文件信息实体
│   ├── ganDaShi/                # 甘大师社区实体（帖子、评论）
│   ├── mail/                    # 邮件信息实体
│   ├── menu/                    # 菜单实体
│   ├── process/                 # 工作流相关实体
│   │   ├── ProcessDefinition.java      # 流程定义
│   │   ├── ProcessInstance.java        # 流程实例
│   │   ├── ProcessHandleInfo.java      # 处理信息
│   │   └── enums/               # 流程状态、按钮、驳回类型等枚举
│   ├── rapidDevelopment/        # 快速开发（代码生成）实体
│   ├── statisticalCenter/       # 统计中心实体
│   ├── system/                  # 系统配置实体
│   ├── website/                 # 网站管理实体
│   ├── HttpResult.java          # 统一响应包装类
│   ├── Page.java                # 分页结果
│   ├── PageParam.java           # 分页参数
│   └── WebLog.java              # 请求日志实体
└── utils/                       # 工具类
    ├── CatUUID.java             # UUID 生成器
    ├── CryptoUtils.java         # 加密工具
    ├── JwtUtils.java            # JWT 工具
    ├── JSONUtils.java           # JSON 工具
    ├── RegexUtils.java          # 正则工具
    ├── ServletUtils.java        # Servlet 工具
    ├── base64/                  # Base64 工具
    ├── crypto/                  # 密钥生成、SHA256
    ├── datetime/                # 日期时间工具
    ├── flowable/                # Flowable 工作流工具
    ├── googleauth/              # Google 身份验证器工具
    ├── http/                    # HTTP 请求工具（HttpClient、OkHttp）
    └── who/                     # 随机用户名等趣味工具
```

---

## 主要依赖

| 依赖 | 说明 |
|------|------|
| `mybatis-plus-spring-boot3-starter` | MyBatis-Plus ORM |
| `mybatis-plus-generator` | 代码生成器 |
| `mysql-connector-j` | MySQL 驱动 |
| `jjwt-*` | JWT 生成与解析 |
| `knife4j-openapi3-jakarta-spring-boot-starter` | API 文档注解支持 |
| `hutool-all` | Hutool 工具库 |
| `flowable-spring-boot-starter-process` | Flowable 工作流 |
| `fastjson2` | JSON 处理 |
| `googleauth` | Google Authenticator 2FA |
| `zxing` | 二维码生成 |

---

## 核心类说明

### 统一响应

- **`HttpResult<T>`**：所有接口的统一返回结构，包含状态码、消息和数据。
- **`HttpResultStatus`**：响应状态码枚举。

### 分页

- **`PageParam`**：分页查询参数基类，支持 `pageNum`、`pageSize`。
- **`Page<T>`**：分页结果包装，包含总记录数与当前页数据。

### 认证实体

- **`LoginUser`**：登录用户上下文，包含用户基本信息、角色列表、权限列表。
- **`User`** / **`Role`** / **`Org`**：基础权限模型实体。

### 工作流实体

- **`ProcessDefinition`**：自定义流程定义扩展。
- **`ProcessInstance`**：流程实例与业务数据关联。
- **`ProcessHandleInfo`**：审批处理记录。

### 动态表单实体

- **`DynamicForm`**：表单模板定义。
- **`DynamicFormField`**：表单字段定义（类型、校验、选项等）。
- **`DynamicFormInstance`**：表单实例（一次填表记录）。
- **`FormData`**：表单提交数据结构。

---

## 使用方式

在其他模块的 `pom.xml` 中引入：

```xml
<dependency>
    <groupId>com.cat</groupId>
    <artifactId>common</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

---

## 注意事项

- 本模块的 `spring-boot-starter-web` 依赖 scope 为 `provided`，避免与上层模块的 Web 依赖冲突。
- 实体类统一使用 Lombok 注解简化代码。