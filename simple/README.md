# simple 模块

> Joker-Box 核心业务模块，包含系统启动入口、所有业务控制器、服务层、数据访问层及各类配置。

---

## 模块定位

`simple` 是项目的主应用模块，提供可独立运行的 Spring Boot 应用。它依赖 `common` 模块，并在此之上实现了完整的业务功能，包括 Web API、安全认证、工作流、AI 对话、文件存储等。

---

## 目录结构

```
simple/src/main/java/com/cat/simple/
├── SimpleApplication.java              # Spring Boot 启动类
├── config/                             # 配置类
│   ├── advice/                         # 全局响应/异常处理
│   │   ├── CustomResponseBodyAdviceAdapter.java
│   │   └── ErrorControllerAdvice.java
│   ├── aspect/                         # AOP 切面
│   │   └── WebLogAspect.java           # 请求日志切面
│   ├── bean/                           # Bean 工具
│   │   └── SpringContextHolder.java
│   ├── flowable/                       # Flowable 工作流配置
│   │   ├── FlowableEngineConfigurer.java       # 流程引擎配置
│   │   ├── ProcessInstanceEndListener.java     # 流程结束监听
│   │   ├── approval/                   # 审批类型策略
│   │   │   ├── ApprovalContext.java
│   │   │   ├── ApprovalTypeEnum.java
│   │   │   ├── ApprovalTypeHandler.java
│   │   │   └── handler/                # 会签、或签、随机签、认领处理器
│   │   ├── candidate/                  # 候选人解析
│   │   ├── linkage/                    # 联动校验
│   │   ├── listener/                   # 任务创建监听
│   │   ├── parse/                      # 自定义节点解析
│   │   └── reject/                     # 任务驳回服务
│   ├── kafka/                          # Kafka 配置
│   │   └── KafkaConfig.java
│   ├── mail/                           # 邮件配置
│   │   └── MailConfig.java
│   ├── mapping/                        # 端点映射配置
│   ├── minio/                          # MinIO 对象存储
│   │   ├── MinioConfig.java
│   │   └── MinioService.java
│   ├── mybatisPlus/                    # MyBatis-Plus 配置
│   │   └── MybatisPlusConfig.java
│   ├── redis/                          # Redis 配置与服务
│   │   ├── RedisConfig.java
│   │   └── RedisService.java
│   ├── security/                       # Spring Security 配置
│   │   ├── SecurityConfig.java         # 核心安全配置
│   │   ├── AuthFilter.java             # JWT 认证过滤器
│   │   ├── AuthorizationManagerImpl.java# 鉴权管理器
│   │   ├── UserDetailsImpl.java        # 用户详情实现
│   │   ├── CustomOAuth2UserService.java# OAuth2 用户服务
│   │   ├── OAuth2LoginSuccessHandler.java
│   │   ├── OAuth2LoginFailureHandler.java
│   │   └── SecurityUtils.java          # 安全工具类
│   ├── system/                         # 系统级配置
│   │   ├── AppStartupRunner.java       # 启动后初始化
│   │   └── OpenBrowser.java            # 自动打开浏览器
│   └── web/                            # Web 配置
│       └── WebMvcConfig.java
├── controller/                         # REST API 控制器
│   ├── AuthController.java             # 认证（登录、注册、Token）
│   ├── UserController.java             # 用户管理
│   ├── RoleController.java             # 角色管理
│   ├── MenuController.java             # 菜单管理
│   ├── OrgController.java              # 组织架构
│   ├── ApiPathController.java          # API 路径管理
│   ├── FileController.java             # 文件上传/下载
│   ├── DynamicFormController.java      # 动态表单
│   ├── ProcessDefinitionController.java# 流程定义
│   ├── ProcessInstanceController.java  # 流程实例与审批
│   ├── StatisticalCenterController.java# 统计中心
│   ├── SystemController.java           # 系统配置
│   ├── MailInfoController.java         # 邮件管理
│   ├── CrawlerTaskController.java      # 爬虫任务
│   ├── WebsiteController.java          # 网站管理
│   ├── WebLogController.java           # 请求日志
│   ├── GanDaShiPostController.java     # 甘大师帖子
│   ├── GanDaShiCommentController.java  # 甘大师评论
│   ├── RapidDevelopmentController.java # 快速开发（代码生成）
│   ├── InfoController.java             # 系统信息
│   └── ai/                             # AI 相关接口
│       ├── AiController.java
│       └── AiModelController.java
├── mapper/                             # MyBatis Mapper 接口
├── service/                            # 业务服务层
│   ├── ai/                             # AI 聊天服务
│   │   ├── AiChatService.java
│   │   └── impl/AiChatServiceImpl.java
│   └── impl/                           # 各服务实现类
└── task/                               # 定时任务与初始化任务
    ├── InitSystemTask.java             # 系统初始化
    ├── UserRegisterTask.java           # 用户注册任务
    ├── WebLogBakTask.java              # 日志备份
    ├── DynamicFormTest.java            # 动态表单测试
    └── FlowableTest.java               # 工作流测试
```

---

## 主要依赖

| 依赖 | 说明 |
|------|------|
| `common` | 项目公共模块 |
| `spring-boot-starter-web` | Web 容器 |
| `spring-boot-starter-security` | 安全框架 |
| `spring-boot-starter-oauth2-client` | OAuth2 登录 |
| `spring-boot-starter-data-redis` | Redis 缓存 |
| `spring-boot-starter-mail` | 邮件发送 |
| `spring-boot-starter-actuator` | 健康监控 |
| `spring-kafka` | 消息队列 |
| `spring-ai-starter-model-openai` | Spring AI OpenAI |
| `mybatis-plus-spring-boot3-starter` | ORM |
| `mysql-connector-j` | 数据库驱动 |
| `flowable-spring-boot-starter-process` | 工作流引擎 |
| `minio` | 对象存储 |
| `knife4j-openapi3-jakarta-spring-boot-starter` | API 文档 |

---

## 核心功能说明

### 1. 安全认证

- 基于 **Spring Security + JWT** 的无状态认证
- 支持用户名密码登录、邮箱验证码注册
- 支持 **OAuth2** 第三方登录
- 支持 **Google Authenticator** 二次验证
- 接口级权限控制：角色 -> 菜单 -> API 路径

### 2. 动态表单

- 表单模板自定义：字段类型、选项、校验规则、联动规则
- 运行时渲染表单并收集数据
- 支持表单实例的增删改查

### 3. 工作流引擎

- 基于 **Flowable 8.0**
- 支持 BPMN 流程定义部署
- 审批模式：会签（全部通过）、或签（一人通过）、随机签、认领
- 支持流程驳回、转办、委托
- 流程实例与业务数据关联

### 4. AI 智能助手

- 基于 **Spring AI** 集成 **OpenAI**
- 支持多轮对话、上下文记忆
- 提示词模板管理（`SystemPrompt`）
- AI 模型配置管理

### 5. 文件存储

- 基于 **MinIO** 的分布式对象存储
- 支持文件上传、下载、删除、预览
- 文件信息持久化到数据库

### 6. 其他功能

| 功能 | 说明 |
|------|------|
| 爬虫任务 | 定时抓取外部数据 |
| 邮件服务 | 模板化邮件发送 |
| 统计中心 | 数据图表与指标统计 |
| 甘大师 | 轻量级社区（帖子、评论） |
| 快速开发 | 基于数据库表自动生成前后端代码 |
| 请求日志 | 自动记录 API 调用日志 |

---

## 配置文件

```
simple/src/main/resources/
├── application.yaml                    # 主配置（指定 Profile）
├── config/
│   ├── application-dev.yaml            # 开发环境
│   └── application-prod.yaml           # 生产环境
├── log4j2.xml                          # 日志配置
├── banner.txt                          # 启动 Banner
├── mapper/                             # MyBatis XML 映射文件
├── process/                            # BPMN 流程定义文件
├── sql/                                # SQL 脚本
├── static/                             # 静态资源
└── templates/                          # 模板文件
```

---

## 启动方式

### 开发环境

```bash
mvn spring-boot:run
```

### 生产环境

```bash
mvn clean package
cd target
# 解压发行包
tar -xzf simple-1.0-SNAPSHOT.tar.gz
cd simple-1.0-SNAPSHOT/bin
./start.sh
```

---

## API 文档

启动后访问：http://localhost:8080/doc.html

文档基于 Knife4j + OpenAPI 3 自动生成。

---

## 注意事项

- 启动前请确保 MySQL、Redis 已正确配置并可用。
- Flowable 引擎首次启动会自动创建相关表，或执行 `sql/init.sql` 初始化。
- MinIO、Kafka、邮件服务为可选依赖，未配置时不影响核心功能运行。
- AI 功能需要配置有效的 OpenAI API Key。