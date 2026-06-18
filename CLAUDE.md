# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

AI 售后维修工单 SaaS（V0.1 内部演示版本）。纯后端项目，无前端。使用简体中文编写文档和注释。

## 常用命令

```bash
# 启动基础设施（MySQL:3307 + Redis:6379）
docker compose up -d

# 构建并启动后端（端口 8080）
cd backend-java
mvn spring-boot:run

# 仅编译
mvn compile

# 打包
mvn package -DskipTests
```

项目无单元测试，无 lint 工具配置。数据库 Schema 由 Flyway 在首次启动时自动迁移（`backend-java/src/main/resources/db/migration/`）。

## 技术栈

- Java 17 + Spring Boot 3.2.7
- MyBatis-Plus 3.5.7（ORM，自动填充、逻辑删除、分页）
- MySQL 8.0（utf8mb4）+ Redis 7
- Flyway（数据库迁移）
- JWT 认证（jjwt 0.12.6），**不使用 Spring Security 框架**，仅引入 spring-security-crypto 做 BCrypt
- Lombok

## 架构

**模块化单体**，包根路径：`com.repair.ai.saas`

每个业务模块遵循 `controller → service → mapper → entity` 分层，模块之间通过 Service 层调用。

```
backend-java/src/main/java/com/repair/ai/saas/
├── common/          # ApiResponse, BusinessException, GlobalExceptionHandler
├── config/          # MyBatisPlus, WebMvc, Redis 配置
├── dto/             # PageResponse 等通用 DTO
├── security/        # JWT 工具, UserContext, @CurrentUserInfo 注解, JwtAuthenticationFilter
└── module/
    ├── tenant/      # 企业注册、租户码生成
    ├── user/        # 员工 CRUD、登录
    ├── customer/    # 客户管理（同租户手机号自动合并）
    ├── ticket/      # 工单（核心模块），含 enums/ (TicketStatus, TicketPriority)
    └── operation/   # 操作日志，含 enums/ (OperationType, Role)
```

### 关键设计

**多租户隔离**：所有业务表含 `tenant_id`，JWT 中携带 tenantId，Service 层按租户过滤。新增查询时必须带 tenantId 条件。

**工单状态机**（TicketStatus 枚举定义合法转换）：
```
PENDING → ASSIGNED → IN_PROGRESS → COMPLETED → FOLLOWED_UP → CLOSED
    ↓         ↓                     ↓
CANCELLED  CANCELLED              CLOSED
```
状态变更时会写入 `ticket_status_log` 表。修改工单逻辑时必须同步更新状态机校验。

**认证与鉴权**：
- `JwtAuthenticationFilter` 解析 Authorization header，将用户信息存入 `UserContext`（ThreadLocal）
- `@CurrentUserInfo` 注解自动注入当前用户到 Controller 参数
- 角色：ADMIN（管理员）、DISPATCHER（客服/调度）、TECHNICIAN（师傅）
- API 路径按角色分组：`/api/admin/`、`/api/technician/`、`/api/common/`、`/api/public/`

**统一响应**：所有接口返回 `ApiResponse<T>`，包含 code/message/data。

**数据库**：核心表含 `deleted` 逻辑删除字段，`created_at`/`updated_at` 由 MyBatis-Plus 自动填充。

## 环境配置

- 主配置：`backend-java/src/main/resources/application.yml`
- 开发配置：`backend-java/src/main/resources/application-dev.yml`（MySQL localhost:3307/root/root）
- JWT 过期时间：24 小时
- 时区：Asia/Shanghai
