# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Joker-Box is a Spring Boot 3 + Java 21 enterprise admin platform with workflow engine, dynamic forms, AI chat, and RBAC. It is a Maven multi-module project:

- `common/` — Shared entities, DTOs, utilities. No Spring Boot starter or web controllers. `spring-boot-starter-web` is `provided` scope to avoid conflicts with consumers.
- `simple/` — Main Spring Boot application. Contains all controllers, services, mappers, configurations, and the executable JAR.

## Build, Run, and Test

### Prerequisites
- JDK 21+
- Maven 3.8+
- MySQL 8.0+, Redis 6.0+ (running)
- MinIO, Kafka, OpenAI API key are optional

### Common Commands

Compile all modules:
```bash
mvn clean install
```

Run the application (from repo root):
```bash
cd simple && mvn spring-boot:run
```

Run all tests:
```bash
mvn test
```

Run a single test class:
```bash
cd simple && mvn test -Dtest=ProcessInstanceServiceTest
```

Run a single test method:
```bash
cd simple && mvn test -Dtest=ProcessInstanceServiceTest#testQueryPageDraft
```

Build distribution package:
```bash
mvn clean package
```
The assembly plugin produces `simple/target/simple-1.0-SNAPSHOT.tar.gz` (and `.zip`) containing JAR, startup scripts (`bin/`), and externalized config.

### Configuration
- `simple/src/main/resources/config/application.yaml` — Sets active profile (`dev`)
- `simple/src/main/resources/config/application-dev.yaml` — Database, Redis, MinIO, Kafka, mail, OAuth2, AI settings
- `simple/src/main/resources/config/application-prod.yaml` — Production overrides
- `simple/src/main/resources/log4j2.xml` — Logging config (uses Log4j2, **not** Spring default logging)

### Database
Initialize schema with:
```bash
mysql -u root -p < sql/init.sql
```
Flowable engine auto-creates its own tables on first startup if they do not exist.

### API Documentation
Knife4j (OpenAPI 3) at `http://localhost:8080/doc.html`

## High-Level Architecture

### 1. Module Dependency Direction
`simple` depends on `common`. `common` must never depend on `simple`. Entities and utilities live in `common`; all Spring beans, controllers, and configurations live in `simple`.

### 2. Security Architecture
Authentication is stateless JWT with a custom filter chain:
- `AuthFilter` (`UsernamePasswordAuthenticationFilter` position) reads `Authorization: Bearer <token>` header, resolves the user via `UserService`, and sets `SecurityContextHolder`.
- `AuthorizationManagerImpl` performs per-request authorization (role → menu → API path).
- `SecurityConfig.WHITE_LIST` holds paths bypassing auth (e.g., `/doc.html`, `/auth/getToken`).
- `UserDetailsImpl` wraps `LoginUser` (from `common`) for Spring Security.
- Tests that call secured services must populate `SecurityContextHolder` with a `UsernamePasswordAuthenticationToken` wrapping `UserDetailsImpl`.

### 3. Flowable Workflow Engine Extensions
The project extends Flowable 8.0 with custom BPMN parsing and approval strategies:

- `FlowableEngineConfigurer` registers `ApprovalUserTaskParseHandler` as a **pre-parse handler**, so custom extension elements are translated before Flowable's default parser runs.
- `ApprovalUserTaskParseHandler` parses custom `<flowable:approvalType>` elements and injects a `TaskListener` expression `${approvalTaskCreateListener}` into matching `UserTask` nodes.
- `ApprovalTaskCreateListener` (the TaskListener) looks up the BPMN `UserTask` at runtime, extracts `ApprovalContext`, and dispatches to the matching `ApprovalTypeHandler`.
- **Approval type strategy pattern**: Four handlers implement `ApprovalTypeHandler`:
  - `CountersignHandler` — multi-instance parallel, requires pass rate
  - `OrSignHandler` — multi-instance parallel, any completion proceeds
  - `RandomHandler` — picks 1 random assignee from candidates
  - `ClaimHandler` — candidates can claim, no pre-assignment
- `ProcessVariableStore` is the abstraction for Flowable variable I/O. Use it instead of calling `RuntimeService`/`TaskService` directly.
- `ProcessGuard` and `HandleInfoRecorder` provide runtime guards and history recording around task operations.

### 4. Process Definition Versioning
Process definitions have a lifecycle separate from Flowable's native versioning:

- **DRAFT** — editable working copy stored in `process_definition_bytearray` alongside form bindings and field permissions.
- **Deploy** — `ProcessDefinitionServiceImpl.deploy()` validates BPMN XML via `FlowableUtils.validateBpmnXml()`, deploys to Flowable, then copies DRAFT rows to the new Flowable-generated version number (e.g., `1`, `2`) and deletes DRAFT.
- **Stop** — copies the latest published version back to DRAFT, sets status `-1`.
- **Rollback** — copies a target historical version back to DRAFT, sets status `0`.
- Deletion is only allowed when status is `0` (draft/unpublished) and no published versions exist.

### 5. Dynamic Forms
Forms are defined in `DynamicForm` / `DynamicFormField` and have their own publish/deploy lifecycle similar to process definitions. `DynamicFormService` manages template CRUD and instance data via `FormData`. Forms can be bound to process definitions globally or per-node, with field-level permissions per node.

### 6. Global Response and Exception Handling
- `CustomResponseBodyAdviceAdapter` wraps controller responses (all return `HttpResult<T>`).
- `ErrorControllerAdvice` catches all exceptions and returns `HttpResult` with status codes mapped to `HttpResultStatus`.
- Controllers should return `HttpResult<T>` for consistency.

### 7. ORM and Pagination
MyBatis-Plus is used throughout. `MybatisPlusConfig` registers `PaginationInnerInterceptor(DbType.MYSQL)`. All paginated queries use `PageParam` (base class with `pageNum`/`pageSize`) and return `Page<T>`.

### 8. Notable Dependency Choices
- **Log4j2** is explicitly used; Spring Boot's default logging starter is excluded in both `common` and `simple` POMs.
- **MyBatis-Plus generator** and **Freemarker** are present for rapid development code generation.
- `spring-ai-starter-model-openai` provides AI chat via Spring AI.
- `knife4j-openapi3-jakarta-spring-boot-starter` generates API docs.