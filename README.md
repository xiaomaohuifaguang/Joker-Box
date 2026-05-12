# Joker-Box

> 一个基于 Spring Boot 3 + Java 21 的全栈快速开发平台，集成了工作流、动态表单、AI 对话、权限管理等企业级功能。

---

## 项目简介

Joker-Box 是一个轻量级、可扩展的企业级后台管理系统，旨在提供开箱即用的基础能力。项目采用模块化设计，涵盖用户权限、动态表单、工作流审批、AI 智能助手、文件存储等核心模块，适用于快速搭建中小型业务系统。

---

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 开发语言 |
| Spring Boot | 3.2.12 | 基础框架 |
| Spring Security | 6.x | 安全认证与授权 |
| Spring AI | 1.0.0 | AI 模型集成 |
| Flowable | 8.0.0 | 工作流引擎 |
| MyBatis-Plus | 3.5.7 | ORM 框架 |
| MySQL | 8.4+ | 关系型数据库 |
| Redis | - | 缓存与会话 |
| Kafka | - | 消息队列 |
| MinIO | 8.5.11 | 对象存储 |
| Knife4j | 4.4.0 | API 文档 |
| JWT | 0.12.5 | Token 认证 |
| Maven | - | 构建工具 |

---

## 模块结构

```
joker-box
├── common/          # 公共模块：实体类、工具类、常量等
├── simple/          # 主应用模块：业务逻辑、控制器、配置等
└── sql/
    └── init.sql     # 数据库初始化脚本
```

### 模块说明

| 模块 | 说明 |
|------|------|
| [common](./common/README.md) | 公共模块，提供实体类、通用工具、常量定义，可被其他模块依赖 |
| [simple](./simple/README.md) | 核心业务模块，包含所有 Controller、Service、Mapper、配置等 |

---

## 核心功能

- **用户与权限**：基于 Spring Security + JWT 的认证体系，支持角色、菜单、API 路径的细粒度权限控制
- **动态表单**：可视化配置表单字段、联动规则，支持运行时动态渲染与数据收集
- **工作流引擎**：集成 Flowable，支持流程定义、审批任务、会签/或签/随机签等多种审批模式
- **AI 智能助手**：基于 Spring AI 集成 OpenAI，提供智能对话与提示词管理
- **文件管理**：基于 MinIO 的对象存储，支持文件上传、下载、预览
- **爬虫任务**：内置爬虫任务调度与管理
- **邮件服务**：支持邮件发送与模板管理
- **统计分析**：提供数据可视化图表与统计中心
- **快速开发**：基于 MyBatis-Plus 的代码生成器，加速 CRUD 开发

---

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- MinIO（可选）
- Kafka（可选）

### 1. 克隆项目

```bash
git clone <repository-url>
cd joker-box
```

### 2. 初始化数据库

```bash
mysql -u root -p < sql/init.sql
```

### 3. 修改配置

编辑 `simple/src/main/resources/config/application-dev.yaml`，配置数据库、Redis、MinIO、Kafka 等连接信息。

### 4. 编译运行

```bash
# 编译
mvn clean install

# 运行
cd simple
mvn spring-boot:run
```

或者：

```bash
java -jar simple/target/simple-1.0-SNAPSHOT.jar
```

### 5. 访问系统

- 系统地址：http://localhost:8080
- API 文档：http://localhost:8080/doc.html

---

## 项目打包

```bash
mvn clean package
```

打包后的完整发行包位于 `simple/target/simple-1.0-SNAPSHOT.tar.gz`，包含启动脚本、配置文件与可执行 JAR。

---

## 配置文件说明

| 文件 | 说明 |
|------|------|
| `simple/src/main/resources/config/application.yaml` | 主配置文件，指定激活的 Profile |
| `simple/src/main/resources/config/application-dev.yaml` | 开发环境配置 |
| `simple/src/main/resources/config/application-prod.yaml` | 生产环境配置 |
| `simple/src/main/resources/log4j2.xml` | 日志配置 |

---

## 贡献指南

欢迎提交 Issue 和 Pull Request。

---

## 许可证

本项目仅供学习交流使用。