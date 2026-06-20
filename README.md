# repair-ai-saas

面向小型售后维修团队的 **AI 工单管理 SaaS**，支持多租户、RBAC 权限、工单状态机、FAQ 知识库、Qdrant 向量检索、AI 智能问答。

> Java 后端 + Python AI 服务双架构，附带 React 管理后台和企业服务门户。Python 不可用时自动降级为 SQL 兜底。

## 业务场景

小型家电维修团队（空调、热水器、净水器等）日常面临：

- 客户报修分散在微信、电话、表格中，容易漏单
- 派单靠人工记忆，老板看不到工单进度
- 师傅维修记录不完整，追责困难
- 客服重复回答相同问题，效率低

本项目通过 SaaS 化 + AI 问答，帮助维修团队**减少漏单、提高派单效率、沉淀维修记录、减少重复客服咨询**。

## 核心功能

| 模块 | 功能 |
|------|------|
| 企业注册 | 多租户注册登录，自动生成租户码 |
| 员工管理 | ADMIN / DISPATCHER / TECHNICIAN 三种角色 |
| 客户管理 | 手机号自动合并，地址和历史工单沉淀 |
| 工单管理 | 创建、派单、改派、取消、关闭 |
| 工单状态机 | PENDING → ASSIGNED → IN_PROGRESS → COMPLETED → CLOSED |
| 师傅端 | 接单、开始处理、提交维修结果 |
| 知识库 | FAQ 知识库 + 知识条目 CRUD |
| 文档上传 | 上传 txt/md 自动解析为知识条目（V0.3） |
| AI 问答 | 基于 Qdrant 向量检索的 RAG 问答 |
| 企业门户 | 客户自助服务：AI 客服 / 提交报修 / 查询进度（V0.3.2） |
| 操作日志 | 关键操作全记录 |

## 技术架构

```
┌─────────────┐     HTTP      ┌──────────────────┐
│  客户/师傅   │ ──────────▶  │   Java 后端       │
│  (H5/浏览器) │              │  Spring Boot 3.2  │
└─────────────┘              │  JWT + RBAC       │
                              │  MyBatis-Plus     │
                              └──────┬───────────┘
                                     │ HTTP JSON
                                     ▼
                              ┌──────────────────┐     gRPC/HTTP     ┌───────────┐
                              │  Python Agent     │ ──────────────▶  │  Qdrant   │
                              │  FastAPI          │                   │  向量库    │
                              │  Embedding + LLM  │                   └───────────┘
                              └──────────────────┘
                                     │
                                     ▼
                              ┌──────────────────┐
                              │  DeepSeek / OpenAI│
                              │  (可选，支持 Mock) │
                              └──────────────────┘

         ┌──────────┐    ┌──────────┐
         │  MySQL   │    │  Redis   │
         │  业务数据 │    │  缓存    │
         └──────────┘    └──────────┘
```

| 组件 | 技术 | 职责 |
|------|------|------|
| Java 后端 | Spring Boot 3.2 + MyBatis-Plus 3.5 | 业务主服务：工单、客户、权限、知识库管理 |
| Python Agent | FastAPI + OpenAI SDK | AI 服务：Embedding、向量检索、LLM 问答 |
| 管理后台 | React 18 + TypeScript + Ant Design 5 | 后台管理 UI：知识库、文档、AI 对话 |
| MySQL 8.0 | Flyway 迁移 | 业务数据持久化（11 张表） |
| Redis 7 | Spring Data Redis | 缓存 |
| Qdrant | v1.18.0 | FAQ 向量索引（COSINE 距离） |
| 认证 | JWT (jjwt 0.12) + BCrypt | 无 Spring Security，自研轻量认证 |

## 系统模块

```
backend-java/src/main/java/com/repair/ai/saas/
├── common/          # ApiResponse, BusinessException, GlobalExceptionHandler
├── config/          # MyBatisPlus, WebMvc, Redis, AiConfig
├── security/        # JWT, UserContext, @CurrentUserInfo, RoleChecker
├── dto/             # PageResponse
└── module/
    ├── tenant/      # 企业注册、租户码生成
    ├── user/        # 员工 CRUD、登录
    ├── customer/    # 客户管理
    ├── ticket/      # 工单（核心），含状态机
    ├── knowledge/   # FAQ 知识库 + 文档上传解析（V0.3）
    ├── ai/          # AI 问答 + 对话记录
    └── operation/   # 操作日志

agent-python/app/
├── main.py          # FastAPI 入口 + 端点
├── schemas.py       # 请求/响应 DTO
├── core/config.py   # 环境变量配置
└── services/
    ├── llm_service.py      # LLM 调用 + Mock 模式
    └── vector_service.py   # Qdrant 客户端 + Embedding + 搜索

frontend/src/
├── api/             # Axios 封装 + API 函数
├── layouts/         # AdminLayout（Sider）、PortalLayout（顶栏）
└── pages/           # 管理后台 + portal/ 企业门户
```

## 本地启动

### 1. 启动基础设施

```bash
docker compose up -d
# MySQL :3307, Redis :6379, Qdrant :6333
```

### 2. 启动 Java 后端

```bash
cd backend-java
mvn spring-boot:run
# http://localhost:8080
# Flyway 首次启动自动建表
```

### 3. 启动 Python Agent（可选）

```bash
cd agent-python
pip install -e .

# Mock 模式（无需 API Key，本地演示用）
uvicorn app.main:app --host 0.0.0.0 --port 8090

# Live 模式（需要 API Key）
LLM_API_KEY=sk-xxx uvicorn app.main:app --host 0.0.0.0 --port 8090

# Live + 真实 Embedding
LLM_API_KEY=sk-xxx EMBEDDING_API_KEY=sk-xxx uvicorn app.main:app --host 0.0.0.0 --port 8090
```

> 不启动 agent-python 时，Java 后端自动降级为 SQL LIKE FAQ 摘要兜底回答。

### 4. 启动前端管理后台（可选）

```bash
cd frontend
npm install
npm run dev
# http://localhost:3000
# 代理 /api → http://localhost:8080
```

> 需要先启动 Java 后端，前端通过 Vite proxy 转发 API 请求。

### 5. 访问企业服务门户

门户页面不需要登录，通过 URL 中的 `tenantCode` 识别企业：

```
http://localhost:3000/portal/{tenantCode}
```

| 路由 | 功能 |
|------|------|
| `/portal/:tenantCode` | 企业服务首页（AI 客服入口 + 报修入口） |
| `/portal/:tenantCode/chat` | AI 智能客服（调用公开 API，无需登录） |
| `/portal/:tenantCode/repair` | 提交报修（自动创建客户 + 工单） |
| `/portal/:tenantCode/query` | 查询进度（占位，后续版本开放） |

> 复用已有公开接口 `POST /api/public/{tenantCode}/ai/chat` 和 `POST /api/public/{tenantCode}/repair-requests`，不需要 JWT，前端通过 URL 参数传入 tenantCode。

## 测试与 CI

### Java 单元测试

```bash
mvn -f backend-java/pom.xml test
```

覆盖：TicketStatus 状态机（23）、TicketPriority 解析（12）、KnowledgeStatus 解析（13）、RoleChecker 权限校验（12）。纯逻辑测试，不需要 MySQL / Redis / Qdrant。

### Python 测试

```bash
cd agent-python
pip install -e ".[test]"
pytest tests/ -v
```

覆盖：Mock Embedding 确定性、维度、L2 归一化。不需要真实 Qdrant 或 API Key。

### GitHub Actions CI

每次 push/PR 到 main 自动触发：

| Job | 内容 |
|-----|------|
| backend-java | Java 17 编译 → 单元测试 → Flyway 迁移文件检查 |
| agent-python | Python 3.11 安装 → 编译检查 → 导入检查 → pytest |

详见 `.github/workflows/ci.yml`。

## API 概览

| 分组 | 前缀 | 说明 |
|------|------|------|
| 公开接口 | `/api/public/` | 注册、登录、客户报修、AI 问答 |
| 管理后台 | `/api/admin/` | 工单、客户、员工、知识库、AI 记录管理 |
| 师傅端 | `/api/technician/` | 我的工单、开始处理、提交完成 |
| 通用接口 | `/api/common/` | 个人信息、修改密码 |
| Python Agent | `/agent/` | Chat、搜索、知识同步、删除 |

详细 API 文档见 [docs/api.md](docs/api.md)。

## 版本路线

| 版本 | 内容 | 状态 |
|------|------|------|
| V0.1 | 工单 SaaS 后端（多租户、RBAC、状态机、客户、工单） | ✅ |
| V0.2.0 | FAQ 知识库 CRUD | ✅ |
| V0.2.1 | AI Chat（SQL LIKE 检索 + LLM） | ✅ |
| V0.2.2 | Qdrant 向量检索 + Embedding + RAG | ✅ |
| V0.2.3 | 单元测试 + GitHub Actions CI | ✅ |
| V0.2.4 | 项目文档与面试表达沉淀 | ✅ |
| V0.3.0 | 文档上传解析（txt/md → knowledge_item → Qdrant） | ✅ |
| V0.3.1 | 前端管理后台（React + Ant Design 5） | ✅ |
| V0.3.2 | 企业服务门户（AI 客服 / 报修 / 查询占位） | ✅ |
| V0.3.3 | 商业化交付（租户配置、平台管理、门户品牌化、部署文档） | ✅ |
| V0.3.4 | 商业化安全（到期拦截、随机密码、AI 限额、修改密码、知识库限额） | ✅ |
| V0.3.5 | 试点客户部署包（部署脚本、演示数据、备份恢复、交付文档） | ✅ |
| V0.4 | 师傅端 H5 / 限流 | 📋 |

详见 [docs/roadmap.md](docs/roadmap.md)。

## 项目亮点

1. **多租户 SaaS**：所有业务表含 `tenant_id`，JWT 携带租户信息，Service 层自动过滤
2. **RBAC 权限**：ADMIN / DISPATCHER / TECHNICIAN 三种角色，API 路径按角色分组
3. **工单状态机**：`TicketStatus` 枚举定义合法转换，非法流转直接拒绝，每次变更写日志
4. **RAG 问答**：FAQ → Qdrant 向量检索 → Prompt 组装 → LLM 生成回答
5. **Java + Python 双服务**：Java 处理业务逻辑，Python 处理 AI/向量，职责清晰
6. **优雅降级**：Python 不可用时 Java 自动降级为 SQL LIKE 兜底，不返回 500
7. **统一响应 + 全局异常**：所有接口返回 `ApiResponse<T>`，`BusinessException` 统一处理
8. **CI 保障**：GitHub Actions 自动跑 Java 测试 + Python 测试 + Flyway 检查
9. **Mock 模式**：无 API Key 也能本地演示全流程（LLM Mock + Embedding Mock）

## 数据库

| 表名 | 说明 |
|------|------|
| tenant | 企业/租户 |
| sys_user | 员工账号 |
| customer | 客户 |
| repair_ticket | 维修工单 |
| ticket_status_log | 工单状态流转日志 |
| operation_log | 操作日志 |
| knowledge_base | 知识库 |
| knowledge_item | 知识条目（V0.3 支持 document_id 追踪来源） |
| knowledge_document | 知识文档（V0.3） |
| ai_conversation | AI 对话记录 |
| ai_message | AI 消息记录 |

+ Qdrant `repair_faq_items` 向量索引

所有业务表含 `tenant_id`、`created_at`、`updated_at`，核心表含 `deleted` 逻辑删除。

## 试点部署

> ⚠️ `docker-compose.prod.yml` 和 Nginx 配置为参考模板，未经实际生产环境验证。部署前请根据实际环境调整。

### 快速开始

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env，修改所有密码和 API Key

# 2. 检查环境
bash deploy/scripts/check-env.sh

# 3. 构建前端 + 启动服务
cd frontend && npm install && npm run build && cd ..
docker compose -f docker-compose.prod.yml up -d

# 4. 初始化演示数据（可选）
bash deploy/scripts/init-demo-data.sh
```

详细部署流程见 [试点客户交付文档](docs/trial-customer-onboarding.md)。

### 部署脚本

| 脚本 | 用途 |
|------|------|
| `deploy/scripts/check-env.sh` | 部署前环境检查（.env、Docker、端口、目录） |
| `deploy/scripts/init-demo-data.sh` | 演示数据初始化（纯 API 调用，不直接写库） |
| `deploy/scripts/backup-mysql.sh` | MySQL 数据库备份 |
| `deploy/scripts/backup-uploads.sh` | 上传文件备份 |
| `deploy/scripts/backup-qdrant.sh` | Qdrant 向量数据备份 |
| `deploy/scripts/restore-mysql.sh` | MySQL 数据库恢复（带二次确认） |

所有脚本从 `.env` 读取配置，不硬编码密码。详见 [deploy/README.md](deploy/README.md)。

## 商业化能力

系统已内置以下商业化管理能力，通过平台管理后台（SUPER_ADMIN）配置：

| 能力 | 说明 |
|------|------|
| 多租户隔离 | 所有业务表含 `tenant_id`，Service 层自动过滤 |
| 门户品牌化 | 每个租户可配置标题、描述、主题色、Logo |
| 知识库限额 | `max_knowledge_bases` 控制租户知识库数量 |
| 文档限额 | `max_documents` 控制租户文档数量 |
| AI 调用计量 | `max_ai_daily_calls` 控制每日 AI 调用量，按天统计 |
| 到期管理 | `expired_at` 全链路拦截（登录 + 公开接口 + 门户） |
| 随机临时密码 | 创建租户和重置密码均自动生成安全密码 |

建议套餐方案见 [套餐与限额建议](docs/pricing-and-limits.md)。

## 详细文档

- [系统架构](docs/architecture.md)
- [API 文档](docs/api.md)
- [面试表达](docs/interview.md)
- [版本路线](docs/roadmap.md)
- [产品文档](docs/product.md)
- [部署文档](docs/deployment.md)
- [备份与恢复](docs/backup.md)
- [试点客户交付](docs/trial-customer-onboarding.md)
- [套餐与限额](docs/pricing-and-limits.md)
- [共享服务器部署](docs/shared-server-deployment.md)

## 安全说明

> **⚠️ 生产部署后必须立即修改所有默认密码，否则存在严重安全风险。**

**默认平台管理员账号：** 企业编码 `PLATFORM`，用户名 `superadmin`，密码 `Admin@2024`。
**首次登录后立即修改密码。** 详见 [部署文档安全检查清单](docs/deployment.md#生产部署安全检查清单)。

**安全检查清单：**
- 替换 `.env` 中的 `JWT_SECRET`（用 `openssl rand -base64 32` 生成）
- 替换 `MYSQL_ROOT_PASSWORD`
- 配置真实 `LLM_API_KEY` 和 `EMBEDDING_API_KEY`
- 配置 HTTPS（Nginx SSL）
- 配置定时备份（`deploy/scripts/backup-mysql.sh`）

`POST /api/public/{tenantCode}/repair-requests` 无需认证即可访问。当前版本未做 IP 限流。生产环境需补充：IP 限流、验证码、单租户频率限制。

## License

MIT
