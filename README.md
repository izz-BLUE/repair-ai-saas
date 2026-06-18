# repair-ai-saas

AI 售后维修工单 SaaS —— V0.1 内部演示版本（V0.2 知识库 + AI 问答 + 向量检索已实现）。

## 技术栈

| 组件 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2 |
| ORM | MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0 |
| 向量库 | Qdrant (V0.2) |
| 缓存 | Redis 7 |
| 迁移 | Flyway |
| 认证 | JWT (jjwt 0.12) |
| 密码加密 | BCrypt |
| 构建 | Maven |

## V0.1 功能范围

- ✅ 企业注册登录（多租户）
- ✅ 员工账号管理（ADMIN / DISPATCHER / TECHNICIAN）
- ✅ 客户管理（手机号自动合并）
- ✅ 工单 CRUD
- ✅ 派单 / 改派
- ✅ 师傅接单 / 提交维修结果
- ✅ 工单状态机（PENDING → ASSIGNED → IN_PROGRESS → COMPLETED → CLOSED）
- ✅ 工单状态流转日志
- ✅ 操作日志
- ✅ 统一响应结构 + 全局异常处理
- ✅ 多租户数据隔离

## 快速启动

### 1. 启动 MySQL + Redis

```bash
docker compose up -d
```

### 2. 启动后端

```bash
cd backend-java
mvn spring-boot:run
```

Flyway 会在首次启动时自动建表。

### 3. 启动 AI 代理服务（可选）

```bash
cd agent-python
pip install -e .

# Mock 模式（无需 API Key，本地演示用）
uvicorn app.main:app --host 0.0.0.0 --port 8090

# Live 模式（需要 DeepSeek / OpenAI API Key）
LLM_API_KEY=sk-xxx uvicorn app.main:app --host 0.0.0.0 --port 8090

# Live 模式 + 真实 Embedding（需要 Embedding API Key）
LLM_API_KEY=sk-xxx EMBEDDING_API_KEY=sk-xxx uvicorn app.main:app --host 0.0.0.0 --port 8090
```

不启动 agent-python 时，Java 后端会自动降级为 SQL LIKE FAQ 摘要兜底回答。

### 4. 服务端口

- 后端：http://localhost:8080
- AI 代理：http://localhost:8090
- Qdrant：http://localhost:6333

## 测试

### Java 单元测试

```bash
mvn -f backend-java/pom.xml test
```

测试内容：TicketStatus 状态机、TicketPriority 解析、KnowledgeStatus 解析、RoleChecker 权限校验。均为纯逻辑测试，不需要 MySQL / Redis / Qdrant。

### Python 测试

```bash
cd agent-python
pip install -e ".[test]"
pytest tests/ -v
```

测试内容：Mock Embedding 的确定性、维度、L2 归一化。不需要真实 Qdrant 或 API Key。

## CI

GitHub Actions 自动运行：

- **backend-java** job：Java 17 + Maven 编译 + 单元测试 + Flyway 迁移文件检查
- **agent-python** job：Python 3.11 + 安装依赖 + 编译检查 + 导入检查 + pytest

每次 push/PR 到 main 自动触发，详见 `.github/workflows/ci.yml`。

## API 测试流程

### 1. 注册企业 → 获取 JWT

```bash
curl -X POST http://localhost:8080/api/public/register \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "蓝天家电维修",
    "contactName": "张三",
    "contactPhone": "13800001111",
    "username": "admin",
    "password": "123456"
  }'
```

响应：`{ code: "SUCCESS", data: { token: "...", tenantCode: "TABC123", ... } }`

### 2. 创建客服员工

```bash
curl -X POST http://localhost:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "username": "kefu01",
    "password": "123456",
    "realName": "李四",
    "phone": "13800002222",
    "role": "DISPATCHER"
  }'
```

### 3. 创建师傅员工

```bash
curl -X POST http://localhost:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "username": "shifu01",
    "password": "123456",
    "realName": "王师傅",
    "phone": "13800003333",
    "role": "TECHNICIAN"
  }'
```

### 4. 创建客户

```bash
curl -X POST http://localhost:8080/api/admin/customers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "赵六",
    "phone": "13900001111",
    "address": "北京市朝阳区xx小区3-502"
  }'
```

### 5. 创建工单

```bash
curl -X POST http://localhost:8080/api/admin/tickets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "customerId": 1,
    "productType": "空调",
    "faultType": "不制冷",
    "faultDescription": "开机后出风口有风但不制冷，外机不转",
    "priority": "HIGH"
  }'
```

### 6. 派单

```bash
curl -X PUT http://localhost:8080/api/admin/tickets/1/assign \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"technicianId": 3}'
```

### 7. 师傅开始处理（用师傅 JWT）

```bash
curl -X PUT http://localhost:8080/api/technician/tickets/1/start \
  -H "Authorization: Bearer <shifu_token>"
```

### 8. 师傅提交完成

```bash
curl -X PUT http://localhost:8080/api/technician/tickets/1/complete \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <shifu_token>" \
  -d '{
    "repairResult": "更换启动电容，已恢复正常制冷",
    "costNote": "上门费50 + 电容80 = 130元",
    "partsNote": "启动电容 x1"
  }'
```

### 9. 关闭工单

```bash
curl -X PUT http://localhost:8080/api/admin/tickets/1/close \
  -H "Authorization: Bearer <token>"
```

### 10. 查看工单详情（含状态流转日志）

```bash
curl http://localhost:8080/api/admin/tickets/1 \
  -H "Authorization: Bearer <token>"
```

### 11. 客户公开报修（无需登录）

```bash
curl -X POST http://localhost:8080/api/public/TABC123/repair-requests \
  -H "Content-Type: application/json" \
  -d '{
    "name": "钱七",
    "phone": "13700001111",
    "address": "上海市浦东新区xx路88号",
    "productType": "热水器",
    "faultDescription": "不出热水，显示E2错误"
  }'
```

## 完整 API 列表

### 公开接口（无需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/public/register` | 企业注册 |
| POST | `/api/public/login` | 员工登录 |
| POST | `/api/public/{tenantCode}/repair-requests` | 客户报修 |
| POST | `/api/public/{tenantCode}/ai/chat` | AI 售后问答（V0.2） |

### 管理后台（需 ADMIN / DISPATCHER）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/tickets` | 工单列表 |
| GET | `/api/admin/tickets/{id}` | 工单详情 |
| POST | `/api/admin/tickets` | 创建工单 |
| PUT | `/api/admin/tickets/{id}` | 编辑工单 |
| PUT | `/api/admin/tickets/{id}/assign` | 派单 |
| PUT | `/api/admin/tickets/{id}/reassign` | 改派 |
| PUT | `/api/admin/tickets/{id}/cancel` | 取消工单 |
| PUT | `/api/admin/tickets/{id}/close` | 关闭工单 |
| GET | `/api/admin/customers` | 客户列表 |
| GET | `/api/admin/customers/{id}` | 客户详情 |
| POST | `/api/admin/customers` | 创建客户 |
| PUT | `/api/admin/customers/{id}` | 编辑客户 |
| GET | `/api/admin/technicians` | 师傅列表 |
| POST | `/api/admin/users` | 创建员工 |
| GET | `/api/admin/users` | 员工列表 |
| PUT | `/api/admin/users/{id}` | 编辑员工 |
| PUT | `/api/admin/users/{id}/status` | 启用/禁用 |
| POST | `/api/admin/knowledge-bases` | 创建知识库 |
| GET | `/api/admin/knowledge-bases` | 知识库列表 |
| GET | `/api/admin/knowledge-bases/{id}` | 知识库详情 |
| PUT | `/api/admin/knowledge-bases/{id}` | 编辑知识库 |
| PUT | `/api/admin/knowledge-bases/{id}/status` | 知识库状态变更 |
| POST | `/api/admin/knowledge-items` | 创建知识条目 |
| GET | `/api/admin/knowledge-items` | 知识条目列表（支持 keyword 搜索） |
| GET | `/api/admin/knowledge-items/{id}` | 知识条目详情 |
| PUT | `/api/admin/knowledge-items/{id}` | 编辑知识条目 |
| PUT | `/api/admin/knowledge-items/{id}/status` | 知识条目状态变更 |
| POST | `/api/admin/knowledge-items/sync-vectors` | 批量同步向量（V0.2） |
| GET | `/api/admin/ai/conversations` | AI 对话列表（V0.2） |
| GET | `/api/admin/ai/conversations/{id}` | AI 对话详情含消息（V0.2） |

### 师傅端（需 TECHNICIAN）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/technician/tickets` | 我的工单 |
| GET | `/api/technician/tickets/{id}` | 工单详情 |
| PUT | `/api/technician/tickets/{id}/start` | 开始处理 |
| PUT | `/api/technician/tickets/{id}/complete` | 提交完成 |

### 通用接口（需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/common/profile` | 当前用户信息 |
| PUT | `/api/common/password` | 修改密码 |

## 项目结构

```
repair-ai-saas/
├── docker-compose.yml              # MySQL + Redis
├── docs/
│   └── product.md                  # 产品文档
├── agent-python/                   # AI 代理服务（FastAPI）
│   ├── app/
│   │   ├── main.py                 # FastAPI 入口
│   │   ├── schemas.py              # 请求/响应 DTO
│   │   ├── core/config.py          # 配置（环境变量）
│   │   └── services/llm_service.py # LLM 调用 + Mock 模式
│   ├── pyproject.toml
│   └── README.md
├── backend-java/
│   ├── pom.xml
│   └── src/main/java/com/repair/ai/saas/
│       ├── RepairAiSaasApplication.java
│       ├── common/                  # ApiResponse, BusinessException, GlobalExceptionHandler
│       ├── config/                  # MyBatisPlus, WebMvc, Redis, AiConfig 配置
│       ├── security/                # JWT, UserContext, @CurrentUserInfo
│       ├── dto/                     # PageResponse
│       └── module/
│           ├── tenant/              # 租户注册
│           ├── user/                # 员工/登录
│           ├── customer/            # 客户管理
│           ├── ticket/              # 工单（核心）
│           │   └── enums/           # TicketStatus, TicketPriority
│           ├── knowledge/           # FAQ 知识库（V0.2）
│           │   └── enums/           # KnowledgeStatus
│           ├── ai/                  # AI 问答（V0.2）
│           └── operation/           # 操作日志
│               └── enums/           # OperationType
```

## 数据库（10 张表）

| 表名 | 说明 |
|------|------|
| tenant | 企业/租户 |
| sys_user | 员工账号 |
| customer | 客户 |
| repair_ticket | 维修工单 |
| ticket_status_log | 工单状态流转日志 |
| operation_log | 操作日志 |
| knowledge_base | 知识库（V0.2） |
| knowledge_item | 知识条目（V0.2） |
| ai_conversation | AI 对话记录（V0.2） |
| ai_message | AI 消息记录（V0.2） |
| Qdrant `repair_faq_items` | FAQ 向量索引（V0.2） |

所有业务表含 `tenant_id`、`created_at`、`updated_at`，核心表含 `deleted` 逻辑删除。

## 安全注意事项

> **公开报修接口限流说明（V0.1 演示版本）**
>
> `POST /api/public/{tenantCode}/repair-requests` 无需认证即可访问。
> 当前 V0.1 演示版本**未做 IP 限流、验证码或频率控制**，存在被恶意刷单的风险。
>
> 生产环境部署前必须补充以下防护措施：
> - IP 级别限流（如 Redis + 滑动窗口，建议每个 IP 每分钟最多 5 次）
> - 图形验证码 / 短信验证码
> - 单租户每日报修次数上限
> - 请求参数长度和格式严格校验（已部分实现）

## V0.1 未完成事项

- [x] AI 问答（V0.2）✅ 已实现 FAQ 检索 + AI 回答 + 兜底
- [x] 知识库管理（V0.2）✅ 已实现 FAQ 知识库 CRUD
- [ ] 前端管理后台（V0.2）
- [ ] 移动端/师傅端 H5（V0.3）
- [ ] 文件/图片上传（V0.3）
- [ ] 基础统计看板（V0.3）
- [ ] 限流（V0.3）
- [ ] 短信通知
- [ ] 微信通知
- [ ] Docker 部署配置
- [ ] 单元测试
- [ ] API 文档（Knife4j/Swagger）
