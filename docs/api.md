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
| GET | `/api/public/{tenantCode}/tickets/query` | 公开工单查询（需工单号+手机号） |
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
  ]
}
```

### 公开工单查询

`GET /api/public/{tenantCode}/tickets/query?ticketNo=xxx&phone=xxx`

**查询参数：**

| 参数 | 必填 | 说明 |
|------|------|------|
| ticketNo | 是 | 工单编号（如 TK202606210001） |
| phone | 是 | 客户手机号（需与报修时填写的一致） |

**安全约束：**
- 租户不存在 → 404
- 租户禁用 / portal 未启用 / 已到期 → 403
- 工单不存在或手机号不匹配 → 返回相同错误，防止手机号枚举
- 返回数据脱敏：手机号 138****5678，地址前 6 字 + ***

**响应示例：**

```json
{
  "code": "SUCCESS",
  "data": {
    "ticketNo": "TK202606210001",
    "productType": "空调",
    "faultDescription": "不制冷，开机后外机不转",
    "status": "ASSIGNED",
    "statusLabel": "已派单",
    "priority": "NORMAL",
    "customerName": "张三",
    "customerPhone": "138****5678",
    "serviceAddress": "广东省佛山市顺德***",
    "createdAt": "2026-06-21T10:30:00",
    "scheduledTime": "2026-06-21T14:00:00",
    "startTime": null,
    "completionTime": null,
    "technicianName": "李师傅",
    "technicianPhone": "139****9012",
    "statusLogs": [
      {
        "toStatus": "PENDING",
        "toStatusLabel": "待处理",
        "remark": "客户报修",
        "createdAt": "2026-06-21T10:30:00"
      },
      {
        "toStatus": "ASSIGNED",
        "toStatusLabel": "已派单",
        "remark": "派单给师傅",
        "createdAt": "2026-06-21T10:35:00"
      }
    ]
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
| GET | `/api/admin/dashboard/stats` | 工单统计（今日新增/待处理/处理中/已完成/AI对话数） |

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

### 文档上传（V0.3）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/knowledge-documents/upload` | 上传文档（multipart: knowledgeBaseId + file） |
| GET | `/api/admin/knowledge-documents` | 文档列表（支持 knowledgeBaseId / parseStatus 过滤） |
| GET | `/api/admin/knowledge-documents/{id}` | 文档详情 |
| DELETE | `/api/admin/knowledge-documents/{id}` | 删除文档（关联条目标记 INACTIVE） |
| POST | `/api/admin/knowledge-documents/{id}/reparse` | 重新解析文档 |

> 当前仅支持 .txt / .md 格式，文件大小限制 10MB。上传后自动解析为知识条目并同步到 Qdrant。

### 企业门户前端路由

| 路径 | 说明 |
|------|------|
| `/portal/:tenantCode` | 企业服务首页（AI 客服入口 + 报修入口 + 查询入口） |
| `/portal/:tenantCode/chat` | AI 智能客服（调用 `POST /api/public/{tenantCode}/ai/chat`） |
| `/portal/:tenantCode/repair` | 提交报修表单（调用 `POST /api/public/{tenantCode}/repair-requests`） |
| `/portal/:tenantCode/query` | 查询进度（输入工单号+手机号查询维修状态） |

### AI 对话记录

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/ai/conversations` | 对话列表（分页） |
| GET | `/api/admin/ai/conversations/{id}` | 对话详情（含消息列表） |

### 企业设置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/settings` | 获取当前租户门户配置 |
| PUT | `/api/admin/settings` | 更新门户配置（portalTitle/portalDescription/contactPhone/logoUrl/themeColor/portalEnabled） |

### 门户配置（公开）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/public/{tenantCode}/portal-settings` | 公开门户配置（无需登录） |

---

## 平台管理接口（需 SUPER_ADMIN）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/platform/tenants` | 租户列表（分页，排除 PLATFORM） |
| POST | `/api/platform/tenants` | 创建租户（自动生成 tenantCode + 随机临时密码） |
| GET | `/api/platform/tenants/{id}` | 租户详情 |
| PUT | `/api/platform/tenants/{id}` | 编辑租户（基础信息 + 限额 + 到期时间） |
| POST | `/api/platform/tenants/{id}/enable` | 启用租户 |
| POST | `/api/platform/tenants/{id}/disable` | 禁用租户 |
| POST | `/api/platform/tenants/{id}/reset-admin-password` | 重置管理员密码（随机临时密码） |

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
| PUT | `/api/common/password` | 修改密码（body: `oldPassword`, `newPassword`），修改后前端应清除 token 并跳转登录 |

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
