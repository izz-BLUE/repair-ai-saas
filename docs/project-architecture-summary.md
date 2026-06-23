# Repair AI SaaS 项目架构与技术链路总结

> 版本：V0.6.0 | 最后更新：2026-06-23

---

## 1. 项目定位

repair-ai-saas 是一个**面向小型售后维修团队的多租户 AI 工单 SaaS 平台**，已完成从功能开发到生产 Docker 部署验证的完整闭环。

**解决的问题：** 小型维修团队（空调、热水器、净水器等）面临客户报修分散、派单靠人工记忆、师傅维修记录不完整、客服重复回答相同问题的痛点。

**核心能力：**

- 平台方创建租户 / 管理套餐限额 / 启用禁用
- 企业后台管理（客户、工单、员工、知识库、AI 对话记录）
- 客户自助报修 + 查询进度
- AI 智能咨询（基于知识库的 RAG 检索增强问答）
- 工单派单 → 师傅处理 → 完工 → 客户查询，完整业务闭环
- 知识库 + 文档上传 → 自动解析 → Qdrant 向量同步
- Redis 滑动窗口限流保护公开接口
- Web 前端 + 微信小程序原生双端
- Docker Compose 生产部署（6 个容器服务，Nginx 统一入口）

**定位声明：** 本项目已通过本地和生产 Docker 环境的完整验证，具备生产化部署条件，但**尚未作为正式商业产品上线运行**。未验证 HTTPS 证书、公网域名、云服务器、微信小程序正式发布。

---

## 2. 整体架构

### 架构图（文本）

```
                               ┌──────────────────────────────────────────────────┐
                               │                   Nginx (:80)                    │
                               │          静态前端  +  API 反向代理                  │
                               └──────┬──────────────────┬────────────────────────┘
                                      │ /                  │ /api/**
                                      ▼                    ▼
                               ┌──────────┐    ┌───────────────────────────────────┐
                               │ 前端 dist │    │      Java Backend (:8080)         │
                               │ (React)  │    │  Spring Boot 3.2 + MyBatis-Plus   │
                               └──────────┘    │  JWT + RBAC + 多租户 + 状态机      │
                                               └──┬──────────┬──────────┬──────────┘
                                                  │          │          │
                                          ┌───────▼──┐ ┌─────▼─────┐ ┌▼──────────┐
                                          │  MySQL   │ │  Redis    │ │ Python    │
                                          │  业务数据 │ │  限流     │ │ Agent     │
                                          └──────────┘ └───────────┘ │ (:8090)   │
                                                                      └────┬──────┘
                                                                           │
                                                                  ┌────────▼────────┐
                                                                  │     Qdrant      │
                                                                  │   向量索引 (:6333)│
                                                                  └─────────────────┘
                                                                           │
                                                                  ┌────────▼────────┐
                                                                  │  LLM / Embedding│
                                                                  │ (DeepSeek/OpenAI│
                                                                  │  或 Mock 模式)   │
                                                                  └─────────────────┘

┌──────────┐   ┌──────────┐   ┌──────────┐
│ 客户 H5  │   │小程序客户│   │小程序师傅│
│ /portal  │   │ 端页面   │   │ 端页面   │
└──────────┘   └──────────┘   └──────────┘
      │              │              │
      └──────────────┼──────────────┘
                     │
            Nginx (:80) 统一入口
```

### 6 个 Docker 服务

| 服务 | 镜像 | 端口绑定 | 职责 |
|------|------|---------|------|
| nginx | nginx:alpine | 0.0.0.0:80 | 统一入口，静态前端 + API 代理 |
| backend | 自定义 (multi-stage) | 127.0.0.1:8080 | Java 业务主服务 |
| agent-python | 自定义 | 127.0.0.1:8090 | AI / RAG / Embedding |
| mysql | mysql:8.0 | 127.0.0.1:3306 | 业务数据持久化 |
| redis | redis:7-alpine | 127.0.0.1:6379 | 公开接口限流 |
| qdrant | qdrant/qdrant:v1.18.0 | 127.0.0.1:6333 | FAQ 向量索引 |

> 除 Nginx 外所有服务绑定 127.0.0.1，不暴露公网。

---

## 3. 技术栈清单

### 3.1 Java 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.2.7 | 应用框架 |
| MyBatis-Plus | 3.5.7 | ORM（分页、自动填充、逻辑删除） |
| MySQL Connector | 8.0 | 数据库驱动 |
| Flyway | 随 Spring Boot | 数据库迁移（7 个版本） |
| Spring Data Redis | 随 Spring Boot | Redis 操作 |
| jjwt | 0.12.6 | JWT 签发与验证 |
| spring-security-crypto | 随 Spring Boot | BCrypt 密码加密 |
| Lombok | 最新 | 代码简化 |
| Maven | 3.x | 构建工具 |
| JUnit 5 | 随 Spring Boot | 单元测试（125 个） |

**关键设计：** 不使用 Spring Security 框架，认证由自定义 `JwtAuthenticationFilter`（继承 `OncePerRequestFilter`）+ `UserContext`（ThreadLocal）实现。

### 3.2 Python Agent

| 技术 | 版本 | 用途 |
|------|------|------|
| Python | 3.11 | 运行环境 |
| FastAPI | 0.110+ | Web 框架 |
| Uvicorn | 0.29+ | ASGI 服务器 |
| OpenAI SDK | 1.12+ | LLM + Embedding API 调用 |
| Qdrant Client | 1.12+ | 向量数据库操作 |
| Pydantic | 2.6+ | 数据校验 |
| Pydantic Settings | 2.2+ | 环境变量配置管理 |
| httpx | 0.27+ | HTTP 客户端 |
| pytest | 8.0+ | 测试框架 |

**运行模式：** Mock 模式（无 API Key 可运行，规则匹配 + 模板回答）→ Live 模式（调用 DeepSeek/OpenAI API）

### 3.3 Web 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 19.2 | UI 框架 |
| TypeScript | 6.0 | 类型安全 |
| Vite | 8.0 | 构建工具 |
| Ant Design | 6.4 | UI 组件库 |
| React Router | 7.18 | 前端路由 |
| Axios | 1.18 | HTTP 请求 |

**页面清单（15 个）：**

| 分组 | 页面 | 路由 |
|------|------|------|
| 登录 | 登录页 | `/admin/login` |
| 管理后台 | Dashboard、工单管理、知识库管理、知识条目管理、文档管理、AI 对话记录、企业设置 | `/admin/*` |
| 平台管理 | 租户管理 | `/platform/tenants` |
| 企业门户 | 首页、AI 客服、提交报修、查询进度 | `/portal/:tenantCode/*` |
| 师傅端 | 工单列表、工单详情 | `/technician/*` |

### 3.4 微信小程序

| 技术 | 说明 |
|------|------|
| 原生框架 | WXML + WXSS + JavaScript |
| 客户端页面 | 首页、AI 咨询、报修、查询进度 |
| 师傅端页面 | 登录、工单列表、工单详情、完成工单 |
| 工具模块 | request.js（HTTP 封装）、auth.js（JWT 管理）、tenant.js、status.js |

**不使用的技术：** Taro、uni-app（保持原生，体积小、审核风险低）

### 3.5 基础设施

| 组件 | 说明 |
|------|------|
| Docker + Docker Compose | 容器编排 |
| MySQL 8.0 | 业务数据库（utf8mb4） |
| Redis 7 | 限流缓存 |
| Qdrant v1.18.0 | 向量数据库（COSINE 距离） |
| Nginx | 统一入口 + 反向代理 |
| .env / .env.example | 环境变量管理 |
| deploy/scripts/ | 部署脚本（环境检查、数据初始化、备份恢复） |

### 3.6 开发工具

Git / GitHub / Claude Code / Cursor / Postman / curl / PowerShell / Docker Desktop

---

## 4. 核心业务链路

### 4.1 客户报修链路

```
客户访问门户 (/portal/{tenantCode})
  → 点击"提交报修"
  → 填写表单（姓名、手机、地址、产品类型、故障描述）
  → POST /api/public/{tenantCode}/repair-requests
  → Java: 租户校验（ACTIVE + portalEnabled + 未到期）
  → Java: IP + 租户维度限流检查（Redis ZSet）
  → Java: 查找或创建客户（同租户手机号自动合并）
  → Java: 创建工单（状态 PENDING，生成工单号 TK20260623xxxx）
  → MySQL: INSERT repair_ticket + ticket_status_log
  → 返回工单号给客户
```

### 4.2 后台派单链路

```
管理员登录 (/admin/login)
  → 进入工单管理 (/admin/tickets)
  → 查看 PENDING 状态工单
  → 点击"派单"→ 选择师傅 → 设置预约时间
  → PUT /api/admin/tickets/{id}/assign
  → Java: 权限校验（ADMIN/DISPATCHER）
  → Java: 状态机校验（PENDING → ASSIGNED 合法）
  → Java: 更新 technicianId + scheduledTime + status
  → MySQL: UPDATE repair_ticket + INSERT ticket_status_log + INSERT operation_log
  → 返回成功，客户查询页可看到师傅信息
```

### 4.3 师傅处理链路

```
师傅登录小程序 / H5
  → 进入"我的工单"列表（仅显示分配给自己的工单）
  → 点击工单查看详情（客户信息 + 工单信息 + 时间线）
  → 点击"开始处理"
  → PUT /api/technician/tickets/{id}/start
  → Java: 权限校验（TECHNICIAN）+ 所有权校验（technicianId 匹配）
  → Java: 状态机校验（ASSIGNED → IN_PROGRESS 合法）
  → MySQL: 更新 status=IN_PROGRESS, startTime + 写状态日志
  → 师傅到现场维修
  → 点击"完成维修"→ 填写维修结果（必填）+ 配件/费用/备注
  → PUT /api/technician/tickets/{id}/complete
  → Java: 状态机校验（IN_PROGRESS → COMPLETED 合法）
  → MySQL: 更新 status=COMPLETED, completionTime, repairResult + 写状态日志
  → 客户查询页状态同步为"已完成"
```

### 4.4 客户查询链路

```
客户访问门户 → 点击"查询进度"
  → 输入工单号 + 手机号
  → GET /api/public/{tenantCode}/tickets/query?ticketNo=xxx&phone=xxx
  → Java: 租户校验 + IP/租户维度限流
  → Java: ticketNo + phone 双重匹配（不匹配返回相同错误，防手机号枚举）
  → Java: 手机号脱敏（138****5678）+ 地址脱敏
  → 返回：工单状态、师傅信息（脱敏）、完整状态时间线
```

### 4.5 AI 咨询链路

```
客户访问门户 (/portal/{tenantCode}/chat) 或小程序 AI 咨询
  → 输入问题
  → POST /api/public/{tenantCode}/ai/chat
  → Java: 租户校验 + IP 限流（租户日限额由 AiUsageService 负责）
  → Java: 调用 Python Agent (POST /agent/chat)

  【Agent 内部】
  → FastAPI 接收请求
  → vector_service.search(): Embedding → Qdrant 向量检索
    → filter: tenant_id + status=ACTIVE + 可选 productType/faultType
    → 放宽重试（如果带条件没搜到，去掉条件再搜）
  → llm_service.chat(): 组装 Prompt（system + context + question）
  → 调用 LLM 或 Mock 模式生成回答
  → 返回 {answer, shouldCreateTicket, matchedItemCount, model}

  【返回 Java】
  → Java: 落库 ai_conversation + ai_message
  → Java: 记录 AI 日使用量（ai_usage_daily）
  → 返回回答给客户
  
  【降级场景】
  Python Agent 不可用 → Java FaqSearchService SQL LIKE 兜底
  Qdrant 不可用 → Python 返回空 → Java SQL LIKE 兜底
  LLM 不可用（Mock 模式）→ 规则匹配 + 模板回答
```

---

## 5. 多租户与权限模型

### 5.1 角色体系

| 角色 | 编码 | 视图 | 权限范围 |
|------|------|------|---------|
| 平台管理员 | SUPER_ADMIN | Web 后台 | 管理所有租户（创建/启用/禁用/设置限额/重置密码） |
| 租户管理员 | ADMIN | Web 后台 | 管理本租户所有资源（员工、客户、工单、知识库） |
| 调度员 | DISPATCHER | Web 后台 | 管理工单和客户（派单、改派） |
| 维修师傅 | TECHNICIAN | 小程序/H5 | 查看分配给自己的工单、开始处理、完成 |
| 客户 | 无（匿名） | 门户/小程序 | 提交报修、查询进度、AI 咨询 |

### 5.2 认证流程

```
登录: POST /api/public/login { tenantCode, username, password }
  → 根据 tenantCode 查租户（必须 ACTIVE + 未到期）
  → 根据 tenant_id + username 查用户（必须 ACTIVE）
  → BCrypt 验证密码
  → 生成 JWT（payload: userId, tenantId, username, role）
  → 返回 token（有效期 24 小时）
```

### 5.3 请求鉴权流程

```
请求进入 → JwtAuthenticationFilter (OncePerRequestFilter)
  ├── 路径在 PUBLIC_PATHS 白名单？→ 直接放行
  │     ├── /api/public/**（登录、注册、报修、查询、AI chat、门户配置）
  │     └── /api/health（健康检查）
  ├── Authorization header 不存在？→ 401 "未登录"
  ├── Token 无效/过期？→ 401 "Token无效或已过期"
  ├── 查数据库校验用户存在 + 未禁用 + 未删除？→ 否 → 401
  └── 全部通过 → 用户信息写入 UserContext (ThreadLocal) → 放行

Controller 层:
  → @CurrentUserInfo 注解自动注入 CurrentUser
  → RoleChecker.requireAdmin() / requireTechnician() 等角色校验
  → Service 层所有查询带 tenantId 条件
  → 师傅端额外校验 technicianId 所有权
```

### 5.4 数据隔离

| 层级 | 机制 |
|------|------|
| 数据库 | 所有业务表含 `tenant_id` 列，MyBatis-Plus `@TableLogic` 逻辑删除 |
| ORM | Service 层所有查询 LambdaQueryWrapper 带 `eq(Entity::getTenantId, tenantId)` |
| 向量库 | Qdrant 搜索 filter `must=[tenant_id == X, status == ACTIVE]` |
| JWT | Token payload 包含 tenantId，不可伪造 |
| 公开接口 | tenantCode → 查租户 → 获取 tenantId → 后续校验 |

### 5.5 公开接口与受保护接口

| 前缀 | 鉴权 | 说明 |
|------|------|------|
| `/api/public/**` | 无需 JWT | 公开接口：登录、注册、报修、查询、AI chat、门户配置 |
| `/api/health` | 无需 JWT | 健康检查 |
| `/api/admin/**` | ADMIN/DISPATCHER | 管理后台 |
| `/api/technician/**` | TECHNICIAN | 师傅端 |
| `/api/platform/**` | SUPER_ADMIN | 平台管理 |
| `/api/common/**` | 任意已认证用户 | 个人信息、修改密码 |

---

## 6. 数据与状态流转

### 6.1 核心实体关系

```
tenant (租户)
  ├── sys_user (员工: ADMIN/DISPATCHER/TECHNICIAN)
  ├── customer (客户: 同租户手机号自动合并)
  ├── repair_ticket (工单: 关联 customer + technician)
  │     └── ticket_status_log (状态日志: 每次状态变更)
  ├── knowledge_base (知识库)
  │     └── knowledge_item (知识条目: 关联 knowledge_base + 可选 knowledge_document)
  ├── knowledge_document (上传文档: .txt/.md)
  ├── ai_conversation (AI 对话)
  │     └── ai_message (对话消息)
  ├── ai_usage_daily (AI 日使用量统计)
  └── operation_log (操作日志)
```

### 6.2 数据库表清单（11 张）

| 表 | 行数 | 核心字段 |
|------|------|------|
| tenant | ~ | id, tenant_code, name, status, portal_title, logo_url, theme_color, max_knowledge_bases, max_documents, max_ai_daily_calls, expired_at, deleted |
| sys_user | ~ | id, tenant_id, username, password(BCrypt), role, status, deleted |
| customer | ~ | id, tenant_id, name, phone, address, deleted |
| repair_ticket | ~ | id, tenant_id, ticket_no, customer_id, technician_id, status, priority, product_type, fault_description, repair_result, scheduled_time, start_time, completion_time, deleted |
| ticket_status_log | ~ | id, tenant_id, ticket_id, from_status, to_status, operator_id, remark |
| operation_log | ~ | id, tenant_id, operator_id, operation_type, target_type, target_id, description, request_ip |
| knowledge_base | ~ | id, tenant_id, name, description, status, deleted |
| knowledge_item | ~ | id, tenant_id, knowledge_base_id, document_id, title, question, answer, product_type, fault_type, status, deleted |
| knowledge_document | ~ | id, tenant_id, knowledge_base_id, filename, file_path, file_size, parse_status, item_count, deleted |
| ai_conversation | ~ | id, tenant_id, trace_id, customer_name, customer_phone, status |
| ai_message | ~ | id, conversation_id, role(user/assistant), content, matched_items, should_create_ticket |
| ai_usage_daily | ~ | id, tenant_id, usage_date, call_count |

### 6.3 Flyway 迁移版本

| 版本 | 内容 | 新增表 |
|------|------|--------|
| V1 | 初始化建表 | tenant, sys_user, customer, repair_ticket, ticket_status_log, operation_log |
| V2 | 客户唯一约束 | — |
| V3 | 知识库表 | knowledge_base, knowledge_item |
| V4 | AI 对话表 | ai_conversation, ai_message |
| V5 | 文档上传 | knowledge_document |
| V6 | 门户配置与商业化字段 | tenant 表新增 portal/限额字段 |
| V7 | AI 日使用量 | ai_usage_daily |

### 6.4 工单状态机

```
                    ┌──────────────────────────────────────┐
                    │          工单状态流转图                 │
                    └──────────────────────────────────────┘

  PENDING ──────► ASSIGNED ──────► IN_PROGRESS ──────► COMPLETED ──────► FOLLOWED_UP ──────► CLOSED
  待处理           已派单            处理中              已完成           已回访              已关闭
    │                │                                    │
    │                │                                    │
    ▼                ▼                                    ▼
  CANCELLED       CANCELLED                             CLOSED
  已取消           已取消                                已关闭

  每个节点的合法下一状态（由 TicketStatus.getAllowedTargets() 定义）：
  - PENDING     → ASSIGNED, CANCELLED
  - ASSIGNED    → IN_PROGRESS, CANCELLED
  - IN_PROGRESS → COMPLETED
  - COMPLETED   → FOLLOWED_UP, CLOSED
  - FOLLOWED_UP → CLOSED
  - CLOSED      → (无，终态)
  - CANCELLED   → (无，终态)
```

**状态机保障：** `TicketStatus.canTransitionTo(target)` 校验每次状态变更，非法转换抛出 `BusinessException`。Service 层在 assignTicket / startTicket / completeTicket / cancelTicket / closeTicket 中均调用此校验。

---

## 7. Redis 限流设计（已落地验证）

### 7.1 限流矩阵

| 接口 | 维度 | 窗口 | 限制 | 实现 |
|------|------|------|------|------|
| POST `/api/public/login` | IP | 1 min | 5 次 | `RateLimiter.checkLogin(ip)` |
| POST `/api/public/{code}/repair-requests` | IP | 1 min | 3 次 | `RateLimiter.checkRepairIp(ip)` |
| POST `/api/public/{code}/repair-requests` | tenant | 1 min | 10 次 | `RateLimiter.checkRepairTenant(tenantCode)` |
| GET `/api/public/{code}/tickets/query` | IP | 1 min | 10 次 | `RateLimiter.checkQueryIp(ip)` |
| GET `/api/public/{code}/tickets/query` | tenant | 1 min | 30 次 | `RateLimiter.checkQueryTenant(tenantCode)` |
| POST `/api/public/{code}/ai/chat` | IP | 1 min | 5 次 | `RateLimiter.checkChatIp(ip)` |
| AI chat 日调用 | tenant | 1 day | 可配置 | `AiUsageService`（ai_usage_daily 表） |

### 7.2 技术实现

- **算法：** Redis ZSET 滑动窗口（score = 时间戳，member = 唯一标识）
- **事务性：** `SessionCallback` + `multi()/exec()` 保证 removeRangeByScore → zCard → add → expire 原子性
- **429 响应：** HTTP 429 + `code: "TOO_MANY_REQUESTS"` + `message: "请求过于频繁，请稍后重试"`（不暴露阈值和维度）
- **Fail-open：** Redis 异常时放行请求，不阻塞业务
- **IP 获取：** X-Forwarded-For → X-Real-IP → remoteAddr 优先级

### 7.3 关键 Bug 修复（V0.5.5）

Redis 7.4 对 Lua 脚本的类型处理与旧版本不兼容，原有 Lua 脚本返回类型导致限流完全不生效。**修复方案：** 放弃 Lua 脚本，改用 `ZSetOperations` + `SessionCallback`（Redis 事务），保持原子性的同时规避 Lua 兼容问题。本地验证 4/4 限流端点全部 429 生效。

---

## 8. RAG / AI 链路

### 8.1 知识入库流程

```
管理员创建知识条目（或上传文档自动解析）
  → MySQL 写入 knowledge_item
  → Java 调用 Python Agent: POST /agent/knowledge/sync（fire-and-forget）
  → Python: embed(title + question)
  → Python: Qdrant upsert(point)
  → 搜索时通过 filter 按 tenant_id + status=ACTIVE 过滤

文档上传:
  上传 .txt/.md → 安全校验（大小/类型/路径穿越）
  → 按空行切分段落 → 每条生成一个 knowledge_item
  → 逐个同步 Qdrant
  → 更新 knowledge_document.parse_status
```

### 8.2 检索问答流程

```
用户提问
  → Python: embed(question)
  → Qdrant.search(vector, filter, topK=5)
  → 组装 Prompt: system("你是专业售后维修客服") + context(Top-K FAQ) + question
  → LLM 生成回答 / Mock 规则匹配
  → 返回 {answer, shouldCreateTicket, matchedItemCount}
```

### 8.3 Mock vs Live 模式

| 模式 | LLM | Embedding | 适用场景 |
|------|-----|-----------|---------|
| Mock | 规则匹配 + 模板回答 | SHA-256 伪向量（1536 维） | 本地开发 / 无 API Key 演示 |
| Live | DeepSeek Chat / OpenAI | text-embedding-3-small | 生产环境 |

### 8.4 降级策略

```
Java → Python Agent 不可用？→ FaqSearchService SQL LIKE 搜索 FAQ
Python → Qdrant 不可用？→ 返回空搜索结果 → Java SQL LIKE 兜底
Python → LLM 不可用？→ Mock 模式返回模板回答
```

**核心设计原则：** `FaqSearchService`（SQL LIKE 兜底）始终保留，不因 Python/向量库故障导致 AI 对话返回 500。

---

## 9. 生产 Docker 部署链路

### 9.1 部署架构

```
                    ┌───────────────────────────────────┐
                    │         Nginx (:80)                │
                    │  /usr/share/nginx/html ← 前端 dist │
                    │  /api/ → backend:8080              │
                    └──────┬────────────────────────────┘
                           │
          ┌────────────────┼─────────────────────────┐
          │                │                         │
    ┌─────▼──────┐  ┌──────▼──────┐  ┌──────────────▼──┐
    │  backend   │  │agent-python │  │  mysql / redis   │
    │  :8080     │  │   :8090     │  │  / qdrant        │
    │ (multi-    │  │ (pip        │  │  (127.0.0.1 绑定) │
    │  stage)    │  │  install .) │  │                  │
    └────────────┘  └─────────────┘  └──────────────────┘
```

### 9.2 docker-compose.prod.yml 关键设计

- 6 个 service：mysql, redis, qdrant, backend, agent-python, nginx
- **安全绑定：** 除 nginx 外所有 service 端口绑定 127.0.0.1
- **健康检查：** MySQL `mysqladmin ping` → backend 等待 mysql healthy 后才启动
- **环境变量：** 所有敏感值从 `.env` 注入（JWT_SECRET, MYSQL_ROOT_PASSWORD, LLM_API_KEY 等）
- **数据持久化：** 4 个 named volumes（mysql_data, redis_data, qdrant_data, upload_data）
- **Backend Dockerfile：** Maven 多阶段构建（eclipse-temurin:17-jdk-alpine → JRE runtime）
- **Agent Dockerfile：** `pip install .`（V0.6.0 修复 COPY + install 顺序）

### 9.3 V0.6.0 验证结果

| 检查项 | 结果 |
|--------|------|
| `docker compose config --quiet` | ✅ PASS |
| `docker compose build backend` | ✅ PASS |
| `docker compose build agent-python` | ✅ PASS |
| 6 个容器全部 Running | ✅ PASS |
| MySQL healthy | ✅ |
| Redis PONG | ✅ |
| Qdrant healthz ok | ✅ |
| Backend Flyway 迁移无 ERROR | ✅ |
| Agent Uvicorn running + qdrant connected | ✅ |
| Nginx 返回前端 HTML | ✅ |
| `/api/health` 返回 200 `{"status":"UP"}` | ✅ |
| `/api/public/PLATFORM/portal-settings` 返回 200 | ✅ |
| `init-demo-data.sh` 8 步全部成功 | ✅ |
| 生产 smoke test（8 步业务流程） | ✅ 全部通过 |

### 9.4 部署前置操作

1. `cp .env.example .env` → 编辑所有密码和密钥
2. `cd frontend && npm run build` → 生成 dist/
3. `docker compose -f docker-compose.prod.yml up -d`
4. `bash deploy/scripts/init-demo-data.sh`（可选）
5. 登录平台管理 → 修改默认密码

---

## 10. 版本演进

| 版本 | 内容 | 状态 |
|------|------|------|
| V0.1 | 工单 SaaS 后端：多租户、RBAC、状态机、客户、工单、操作日志 | ✅ |
| V0.2.0 | FAQ 知识库 CRUD | ✅ |
| V0.2.1 | AI Chat（SQL LIKE 检索 + LLM） | ✅ |
| V0.2.2 | Qdrant 向量检索 + Embedding + RAG | ✅ |
| V0.2.3 | 单元测试 + GitHub Actions CI | ✅ |
| V0.2.4 | 项目文档与面试表达沉淀 | ✅ |
| V0.3.0 | 文档上传解析（txt/md → knowledge_item → Qdrant） | ✅ |
| V0.3.1 | 前端管理后台（React + Ant Design） | ✅ |
| V0.3.2 | 企业服务门户（AI 客服 / 报修 / 查询占位） | ✅ |
| V0.3.3 | 商业化交付（租户配置、平台管理、门户品牌化） | ✅ |
| V0.3.4 | 商业化安全（到期拦截、随机密码、AI 限额、知识库限额） | ✅ |
| V0.3.5 | 试点客户部署包（部署脚本、演示数据、备份恢复） | ✅ |
| V0.4.0 | 工单闭环增强（公开查询 / 后台管理 / Dashboard 统计） | ✅ |
| V0.4.2 | 师傅端移动 H5（工单列表 / 开始处理 / 完成维修） | ✅ |
| V0.4.3 | 小程序化方案设计（接口适配分析 + 产品方案） | ✅ |
| V0.5.0 | 售后维修小程序 MVP（原生 WXML/WXSS/JS） | ✅ |
| V0.5.1 | 小程序报修 payload 修复 | ✅ |
| V0.5.2 | 生产配置增强 + 小程序联调稳定性修复 | ✅ |
| V0.5.3 | 公开接口 Redis 限流落地 | ✅ |
| V0.5.4 | 演示数据与验收准备 | ✅ |
| V0.5.5 | 本地全流程 Smoke Test + RateLimiter Redis 7.4 修复 | ✅ |
| V0.5.6 | 小程序师傅端 DevTools 验收通过 | ✅ |
| V0.5.7 | Web 派单弹窗 500 修复（日期格式 + 异常处理） | ✅ |
| V0.6.0 | 生产 Docker 部署验证（6 容器全栈构建启动） | ✅ |

---

## 11. 真实问题与排障案例

以下是开发过程中遇到并已修复的真实工程问题，体现排查和解决能力：

### 案例 1：Redis 7.4 Lua 脚本限流完全不生效（V0.5.5）

- **现象：** 4 个公开接口的 Redis 限流全部不生效，连续请求不会返回 429
- **根因：** Redis 7.4 的 Lua 脚本对 `ZADD` / `ZREMRANGEBYSCORE` 的返回值类型处理与旧版本不同，原 Lua 脚本的类型判断逻辑在 Redis 7.4 中失效
- **修复：** 完全放弃 Lua 脚本，改用 Java 侧 `ZSetOperations` + `SessionCallback`（Redis 事务），保持原子性
- **启示：** Lua 脚本跨 Redis 版本兼容性不如官方 SDK，生产环境应谨慎使用

### 案例 2：Web 派单预约时间导致后端 500（V0.5.7）

- **现象：** Web 后台工单详情点击"派单"填写预约时间后弹窗报 500
- **根因：** 前端发送 `YYYY-MM-DD HH:mm:ss`（空格分隔），后端 `LocalDateTime` 默认期望 ISO-8601 `T` 分隔格式，Jackson 反序列化失败抛 `HttpMessageNotReadableException`（500）
- **修复：** 前端改为 `YYYY-MM-DDTHH:mm:ss`；后端 `AssignRequest.scheduledTime` 添加 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 兼容旧格式；`GlobalExceptionHandler` 新增 `HttpMessageNotReadableException` 处理器返回 400 而非 500
- **启示：** 前后端日期格式约定要明确，后端应宽容解析 + 优雅降级

### 案例 3：Agent Dockerfile COPY + pip install 顺序错误（V0.6.0）

- **现象：** `docker compose build agent-python` 失败，`pip install .` 时 `app/` 源码目录缺失
- **根因：** Dockerfile 先 `COPY pyproject.toml ./` 再 `RUN pip install .`，导致 `pip install .` 时只有 `pyproject.toml`，没有 `app/` 源码包
- **修复：** 改为 `COPY . .` 后再 `RUN pip install .`；补充 `pyproject.toml` 的 `[build-system]` 配置；新增 `.dockerignore` 排除 `.venv/`、`__pycache__/` 等
- **启示：** Python 项目 Docker 化时，`pip install .` 需要完整的 package 目录

### 案例 4：Spring Boot 占位符默认值导致数据库用户变成 `-root`（V0.6.0）

- **现象：** 生产 Docker Compose 启动后 Backend 一直 Restarting，日志 `Access denied for user '-root'@'172.19.0.6'`
- **根因：** `application-prod.yml` 使用 bash 语法 `${DB_USERNAME:-root}`，Spring 的 `PropertyPlaceholderHelper` 使用 `:` 作为分隔符，`-` 成为默认值的一部分
- **修复：** 4 处 `${VAR:-default}` → `${VAR:default}`
- **启示：** Spring 配置占位符语法与 Docker Compose / bash 不同，混用时需注意

### 案例 5：`/api/health` 被 JWT Filter 拦截返回 401（V0.6.0）

- **现象：** `curl http://localhost/api/health` 返回 401 `{"code":"UNAUTHORIZED"}`
- **根因：** `JwtAuthenticationFilter.PUBLIC_PATHS` 只包含 `/api/public/**`，`/api/health` 被拦截
- **修复：** 将 `/api/health` 加入 `PUBLIC_PATHS` 白名单，新增 `HealthController` 返回 `{"status":"UP"}`
- **启示：** 自定义 Filter 的白名单需要与新增的公开端点保持同步

### 案例 6：`init-demo-data.sh` 依赖 `python3`（V0.6.0）

- **现象：** 生产服务器可能未安装 Python 3，但演示数据初始化脚本依赖 `python3 -c` 解析 JSON
- **修复：** 8 处 `python3 -c "import sys,json;..."` 替换为 `jq -r '.data.field'`；启动时检查 `curl` + `jq` 是否安装
- **启示：** 运维脚本应最小化依赖，`jq` 比 `python3` 更轻量通用

---

## 12. 面试表达亮点

### 架构层面

1. **多租户 SaaS 建模：** 所有业务表 `tenant_id` 隔离，数据库层 + ORM 层 + 向量库层三层隔离，JWT 携带租户信息不可伪造
2. **Java + Python 双服务解耦：** Java 负责业务逻辑，Python 专注 AI/向量检索，通过 HTTP 通信，Python 不可用时 Java 自动降级 SQL 兜底
3. **工单状态机：** 枚举定义合法转换路径，非法流转直接拒绝，每次变更写状态日志，完整审计追踪
4. **自研轻量认证：** 不用 Spring Security 框架，自定义 `OncePerRequestFilter` + `ThreadLocal` + 注解注入，更轻量可控

### 工程层面

5. **公开接口限流：** Redis ZSET 滑动窗口 + SessionCallback 事务，4 个端点 6 条规则，支持 IP + 租户双维度，fail-open 不阻塞业务
6. **RAG 检索增强问答：** FAQ → Qdrant 向量索引 → Embedding 检索 → Prompt 组装 → LLM 生成，支持 Mock 模式无需 API Key 即可演示
7. **多端接入：** Web 管理后台 + 企业门户 + 师傅端 H5 + 微信小程序，全部复用同一套后端 API
8. **Docker 生产化部署：** 6 容器编排，安全端口绑定，健康检查依赖链，环境变量管理，一键启动
9. **Flyway 数据库迁移：** 7 个版本渐进式演进，支持 baseline-on-migrate，clean-disabled 防误删
10. **CI 自动化：** GitHub Actions 跑 Java 125 个测试 + Python 测试 + Flyway 迁移文件存在性检查
11. **真实排障能力：** Redis Lua 跨版本兼容、Spring 占位符语法陷阱、Java/Python Dockerfile 构建顺序、前后端日期格式约定、JWT Filter 白名单维护 —— 均有完整的发现→根因→修复→验证链条

### 产品层面

12. **商业化能力内置：** 租户限额（知识库/文档/AI 日调用）、到期自动拦截（登录 + 公开接口 + 门户）、随机临时密码、门户品牌化（企业名称/Logo/主题色）
13. **降级兜底设计：** Python → SQL LIKE 降级，Qdrant → 空结果降级，LLM → Mock 降级，Redis → fail-open 放行
14. **安全设计：** 手机号脱敏、防枚举（相同错误信息）、路径穿越防护、文件类型白名单

---

## 13. 后续规划

| 方向 | 内容 |
|------|------|
| 图片上传 | 报修拍照 + 完工前后对比，新增 `ticket_attachment` 表 |
| 微信能力 | 手机号授权、openid 绑定、订阅消息推送 |
| HTTPS | Nginx SSL 证书配置 |
| 更多文档格式 | PDF / Word / Excel 解析 |
| 套餐计费 | 按订阅周期收费、在线支付 |
| API 文档 | Knife4j / Swagger 自动生成 |

---

> 本文档随项目持续更新，最新版本见 [docs/project-architecture-summary.md](project-architecture-summary.md)。
