# API 文档

## 统一响应格式

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {}
}
```

错误码：`BAD_REQUEST` / `VALIDATION_ERROR` / `UNAUTHORIZED` / `FORBIDDEN` / `NOT_FOUND` / `CONFLICT` / `INTERNAL_ERROR`

## 认证方式

除 `/api/public/` 路径外，所有接口需要 `Authorization: Bearer <JWT>` header。

JWT 在企业注册或员工登录时返回，有效期 24 小时。

---

## 公开接口（无需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/public/register` | 企业注册（自动创建 ADMIN 账号） |
| POST | `/api/public/login` | 员工登录（返回 JWT） |
| POST | `/api/public/{tenantCode}/repair-requests` | 客户报修（自动创建客户 + 工单） |
| POST | `/api/public/{tenantCode}/ai/chat` | AI 售后问答 |

### AI 问答请求示例

```json
{
  "question": "空调不制冷怎么办",
  "customerName": "张三",
  "customerPhone": "13800001111"
}
```

### AI 问答响应示例

```json
{
  "code": "SUCCESS",
  "data": {
    "answer": "空调不制冷常见原因：...",
    "shouldCreateTicket": false,
    "matchedItemCount": 3,
    "conversationId": 1,
    "traceId": "abc-123"
  }
}
```

---

## 管理后台接口（需 ADMIN / DISPATCHER）

### 工单管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/tickets` | 工单列表（分页，支持状态/优先级筛选） |
| GET | `/api/admin/tickets/{id}` | 工单详情（含状态流转日志） |
| POST | `/api/admin/tickets` | 创建工单 |
| PUT | `/api/admin/tickets/{id}` | 编辑工单 |
| PUT | `/api/admin/tickets/{id}/assign` | 派单（指定师傅） |
| PUT | `/api/admin/tickets/{id}/reassign` | 改派（换师傅） |
| PUT | `/api/admin/tickets/{id}/cancel` | 取消工单 |
| PUT | `/api/admin/tickets/{id}/close` | 关闭工单 |

### 客户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/customers` | 客户列表（分页，支持手机号/姓名搜索） |
| GET | `/api/admin/customers/{id}` | 客户详情 |
| POST | `/api/admin/customers` | 创建客户（同租户手机号自动合并） |
| PUT | `/api/admin/customers/{id}` | 编辑客户 |

### 员工管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/users` | 员工列表 |
| POST | `/api/admin/users` | 创建员工 |
| PUT | `/api/admin/users/{id}` | 编辑员工 |
| PUT | `/api/admin/users/{id}/status` | 启用/禁用员工 |
| GET | `/api/admin/technicians` | 师傅列表（role=TECHNICIAN） |

### 知识库管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/knowledge-bases` | 创建知识库 |
| GET | `/api/admin/knowledge-bases` | 知识库列表 |
| GET | `/api/admin/knowledge-bases/{id}` | 知识库详情 |
| PUT | `/api/admin/knowledge-bases/{id}` | 编辑知识库 |
| PUT | `/api/admin/knowledge-bases/{id}/status` | 启用/停用知识库 |
| POST | `/api/admin/knowledge-items` | 创建知识条目 |
| GET | `/api/admin/knowledge-items` | 知识条目列表（支持 keyword 搜索） |
| GET | `/api/admin/knowledge-items/{id}` | 知识条目详情 |
| PUT | `/api/admin/knowledge-items/{id}` | 编辑知识条目 |
| PUT | `/api/admin/knowledge-items/{id}/status` | 启用/停用知识条目 |
| POST | `/api/admin/knowledge-items/sync-vectors` | 批量同步向量 |

### AI 对话记录

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/ai/conversations` | 对话列表（分页） |
| GET | `/api/admin/ai/conversations/{id}` | 对话详情（含消息列表） |

---

## 师傅端接口（需 TECHNICIAN）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/technician/tickets` | 我的工单列表 |
| GET | `/api/technician/tickets/{id}` | 工单详情 |
| PUT | `/api/technician/tickets/{id}/start` | 开始处理（ASSIGNED → IN_PROGRESS） |
| PUT | `/api/technician/tickets/{id}/complete` | 提交完成（IN_PROGRESS → COMPLETED） |

---

## 通用接口（需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/common/profile` | 当前用户信息 |
| PUT | `/api/common/password` | 修改密码 |

---

## Python Agent 接口（内部调用）

Python Agent 端点由 Java 后端内部调用，不直接暴露给前端。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查（Qdrant 状态、Mock/Live 模式） |
| POST | `/agent/chat` | AI 问答（向量检索 + LLM） |
| POST | `/agent/search` | 向量搜索 |
| POST | `/agent/knowledge/sync` | 同步单条知识到 Qdrant |
| POST | `/agent/knowledge/delete` | 从 Qdrant 删除单条知识 |

### /agent/chat 请求

```json
{
  "question": "空调不制冷",
  "tenantId": 1,
  "productType": "空调",
  "faultType": "不制冷",
  "topK": 5,
  "traceId": "abc-123"
}
```

### /agent/chat 响应

```json
{
  "answer": "空调不制冷的常见原因有...",
  "shouldCreateTicket": false,
  "matchedItemCount": 3,
  "model": "deepseek-chat",
  "traceId": "abc-123"
}
```

### /health 响应

```json
{
  "status": "ok",
  "mode": "mock",
  "model": "deepseek-chat",
  "embeddingMode": "mock",
  "qdrant": "connected"
}
```
