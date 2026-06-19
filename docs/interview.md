# 面试表达

## 1. 一句话介绍

> 设计并实现面向小型售后维修团队的 AI 工单 SaaS，支持多租户、RBAC 权限、工单状态机、FAQ 知识库、Qdrant 向量检索、RAG 智能问答，采用 Java + Python 双服务架构。

## 2. 为什么做这个项目

小型家电维修团队（空调、热水器、净水器）日常面临几个痛点：

- 客户报修分散在微信、电话、表格中，容易漏单
- 派单靠人工记忆，老板看不到工单进度
- 师傅维修记录不完整，后续追责困难
- 客服重复回答相同问题（空调不制冷、热水器 E2 错误等），效率低

现有的通用工单系统要么太重（Salesforce），要么没有 AI 能力。我做了一个轻量级 SaaS，既解决工单管理问题，又通过 RAG 把常见问题自动化。

## 3. 解决了什么业务问题

1. **减少漏单**：统一报修入口，每个报修自动生成工单
2. **提高派单效率**：管理员看到所有待处理工单，一键派单
3. **沉淀维修记录**：师傅填写维修结果、费用、配件，全程可追溯
4. **减少重复客服**：AI 基于知识库自动回答常见问题，无答案时引导报修
5. **数据可看**：工单状态、处理时长、常见故障类型可统计

## 4. 技术架构怎么设计

**Java + Python 双服务架构：**

- **Java 后端**（Spring Boot 3.2）：负责所有业务逻辑——工单、客户、权限、知识库管理
- **Python Agent**（FastAPI）：负责 AI 相关——Embedding、向量检索、LLM 问答

为什么分两个服务？

1. AI 生态（LangChain、OpenAI SDK、Qdrant 客户端）在 Python 中更成熟
2. 业务逻辑和 AI 逻辑职责清晰，互不耦合
3. 可以独立扩缩容（AI 服务吃 GPU/内存，业务服务吃 CPU）
4. Python 服务挂了不影响核心业务（工单、客户等照常使用）

**存储层：**
- MySQL：业务数据（10 张表，Flyway 迁移）
- Redis：缓存
- Qdrant：FAQ 向量索引

## 5. 多租户怎么做

采用**共享数据库 + tenant_id 隔离**方案：

1. 所有业务表都有 `tenant_id` 字段
2. 用户登录后 JWT 中携带 `tenantId`
3. `JwtAuthenticationFilter` 解析 JWT，将 `tenantId` 存入 `UserContext`（ThreadLocal）
4. Service 层所有查询都自动带 `tenant_id` 条件
5. Qdrant 搜索也带 `tenant_id` filter

为什么不用独立数据库？因为当前目标客户是小型团队（5-50 人），共享数据库方案开发简单、运维成本低。后续大客户可以升级为独立库或私有化部署。

## 6. 权限怎么做

**RBAC 角色模型**，三种角色：

| 角色 | 说明 | 典型操作 |
|------|------|----------|
| ADMIN | 管理员/老板 | 全部操作 + 员工管理 + 知识库管理 |
| DISPATCHER | 客服/调度 | 工单管理 + 派单 + 客户管理 |
| TECHNICIAN | 师傅 | 查看自己的工单 + 开始处理 + 提交完成 |

实现方式：
- API 路径按角色分组：`/api/admin/`、`/api/technician/`、`/api/common/`
- `RoleChecker` 工具类在 Controller 入口校验角色，不通过抛 `BusinessException(FORBIDDEN)`
- 不使用 Spring Security 框架，自研轻量实现（JWT + Filter + ThreadLocal）

## 7. 工单状态机怎么做

用 `TicketStatus` 枚举定义状态和合法转换：

```
PENDING → ASSIGNED → IN_PROGRESS → COMPLETED → FOLLOWED_UP → CLOSED
    ↓         ↓                     ↓
CANCELLED  CANCELLED              CLOSED
```

关键设计：
- `getAllowedTargets(current)` 返回当前状态允许转换的目标集合
- `canTransitionTo(target)` 判断是否允许转换
- Service 层在状态变更前校验，非法转换直接拒绝
- 每次状态变更写入 `ticket_status_log` 表，可追溯完整流转历史
- CLOSED 和 CANCELLED 是终态，不能继续流转

这个设计保证了工单不会出现非法状态（比如从 PENDING 直接到 COMPLETED）。

## 8. RAG 是怎么做的

**检索增强生成（Retrieval-Augmented Generation）流程：**

1. 管理员维护 FAQ 知识库（标题、问题、答案、产品类型、故障类型）
2. 创建/编辑时 Java 通知 Python Agent，Python 将 `title + question` 做 Embedding 后存入 Qdrant
3. 客户提问时，Python Agent 将问题做 Embedding，在 Qdrant 中按 `tenant_id + status=ACTIVE` 过滤后做 COSINE 相似度搜索
4. 搜索结果组装成 Prompt context，连同客户问题一起发给 LLM
5. LLM 基于 context 生成回答

**为什么用 RAG 而不是微调？**
- FAQ 数据量小（几十到几百条），微调成本高
- FAQ 内容经常变化，RAG 可以实时更新
- RAG 有明确的引用来源，可追溯

## 9. 为什么使用 Qdrant

选型考虑：

| 方案 | 优点 | 缺点 |
|------|------|------|
| pgvector | 不用额外组件 | MySQL 不支持，需要 PostgreSQL |
| Milvus | 功能强大 | 部署重，小项目过重 |
| Qdrant | 轻量、Docker 一键部署、API 简单 | 生态不如 Milvus |
| FAISS | 性能好 | 纯库，需要自己管理持久化 |

选择 Qdrant 的原因：
1. Docker 一键部署，开发友好
2. Python SDK 成熟（`qdrant-client`）
3. 支持 payload filter（按 tenant_id 过滤），不需要额外处理
4. 支持 COSINE 距离，适合文本 Embedding
5. 轻量级，适合小项目

## 10. Java 和 Python 如何协作

**通信方式：** HTTP JSON，Java 调用 Python

```
Java AiClient → HTTP POST → Python /agent/chat → 返回 JSON
```

**数据流：**
- Java 传给 Python：question, tenantId, productType, faultType, topK
- Python 返回给 Java：answer, shouldCreateTicket, matchedItemCount, model

**知识同步：**
- Java 知识条目 CRUD 后，fire-and-forget 调用 Python 同步向量
- try-catch 包裹，同步失败不影响主业务，只打 warn 日志
- 提供批量同步接口 `/sync-vectors`，用于修复或初始化

**职责边界：**
- Java 不知道什么是 Embedding、什么是 Qdrant
- Python 不知道什么是工单、什么是租户
- Java 通过 tenantId 和 itemId 传参，Python 只做向量操作

## 11. AI 服务失败如何兜底

**三层降级策略：**

1. **Python 整体不可用**（进程挂了/网络不通）
   → Java `AiClient` 返回 null
   → Java 降级为 `FaqSearchService`（SQL LIKE 搜索 FAQ）
   → 构建兜底回答："根据现有资料，可能的解决方案是..."

2. **Qdrant 不可用**
   → Python search 返回空列表
   → Python 返回空 contexts 给 LLM
   → LLM 返回通用回答 + `shouldCreateTicket=true`

3. **LLM 不可用**
   → Python Mock 模式返回模板回答
   → 或者 Python 返回错误 → Java 降级 SQL LIKE

**关键设计原则：**
- `FaqSearchService` 始终保留，是最后的兜底
- AI 服务失败不返回 500，而是降级为较弱的回答
- 用户体验：降级时回答质量降低，但不会报错

## 12. 项目目前不足和后续优化

### 已知不足

1. **没有前端**：目前是纯后端 + curl 测试，不适合演示
2. **没有限流**：公开报修接口和 AI 问答接口没有防刷
3. **没有文件上传**：工单图片、故障图片暂不支持
4. **没有集成测试**：只有纯逻辑单元测试，没有 Controller/Service 集成测试
5. **Mock 模式的 Embedding**：基于 SHA-256 hash，语义相似度不准确
6. **没有监控**：没有 Prometheus metrics、链路追踪

### 后续优化方向

1. Vue 3 管理后台 + 师傅端 H5
2. 文件上传（故障图片、维修照片）
3. Redis 限流（滑动窗口）
4. Testcontainers 集成测试
5. 企业服务门户（/r/{tenantCode} 公开页）
6. 基础统计看板
7. Docker Compose 一键部署
8. API 文档（Knife4j / Swagger）

## 13. 简历项目描述

### 简短版

> 设计并实现面向小型售后维修团队的 AI 工单 SaaS，采用 Java + Python 双服务架构。Java 后端基于 Spring Boot 实现多租户、RBAC 权限、工单状态机；Python Agent 基于 FastAPI 实现 FAQ 向量检索（Qdrant）和 RAG 智能问答。支持 AI 服务不可用时 Java 自动降级为 SQL 兜底。

### 详细版

> **AI 售后维修工单 SaaS** | Java 17 + Spring Boot 3.2 + Python FastAPI + Qdrant + MySQL
>
> 独立设计并实现面向小型售后维修团队的 SaaS 工单管理系统，支持多租户隔离、RBAC 权限控制、工单状态机流转、FAQ 知识库管理和 RAG 智能问答。
>
> 核心工作：
> - 设计多租户数据隔离方案，所有业务表含 tenant_id，JWT 携带租户信息，Service 层自动过滤
> - 实现工单状态机，枚举定义合法转换，非法流转直接拒绝，状态变更全量日志
> - 搭建 RAG 问答系统，FAQ 存入 Qdrant 向量库，客户提问时做语义检索 + LLM 生成回答
> - 采用 Java + Python 双服务架构，职责分离（业务 vs AI），Python 不可用时 Java 自动降级 SQL 兜底
> - 引入 GitHub Actions CI，Java 60 个单元测试 + Python 5 个测试 + Flyway 迁移检查
> - 使用 Flyway 管理数据库迁移（4 个版本脚本），保证 Schema 可重复部署

## 14. 面试官可能追问的问题

### Q: 多租户隔离为什么不用独立数据库？

**A:** 当前目标客户是小型团队（5-50 人），数据量不大。共享数据库 + tenant_id 方案开发简单、运维成本低。如果后续有大客户，可以升级为独立库或私有化部署。关键是在 Service 层强制带 tenant_id 条件，不会出现数据泄露。

### Q: 工单状态机为什么用枚举而不是数据库配置？

**A:** 工单状态流转是固定的业务规则，不应该被用户修改。用枚举可以在编译期检查，状态转换逻辑清晰。如果用数据库配置，反而增加了误配置的风险。枚举的 `getAllowedTargets()` 方法集中管理所有合法转换，修改时只需改一个地方。

### Q: 为什么选 RAG 而不是微调？

**A:** FAQ 数据量小（几十到几百条），微调成本高且效果不确定。FAQ 内容经常变化（新增产品、新故障类型），RAG 可以实时更新。RAG 的回答有明确的 FAQ 来源，可以追溯和审核。

### Q: 向量检索的准确率怎么样？

**A:** Mock 模式下用 SHA-256 hash 生成向量，语义相似度不准确，仅用于本地演示。Live 模式下用 OpenAI text-embedding-3-small，对中文 FAQ 的检索效果足够。可以按 product_type / fault_type 过滤提高精确度。如果搜索不到，会放宽条件重试一次。

### Q: 为什么不直接用 Spring Security？

**A:** Spring Security 框架很重，引入后会带来大量自动配置。这个项目的鉴权需求简单（三种角色 + 路径分组），用自研的 JWT Filter + RoleChecker 更轻量、更可控。只引入了 spring-security-crypto 做 BCrypt 密码加密。

### Q: AI 服务挂了怎么办？

**A:** 三层降级：Python 整体不可用 → Java 降级 SQL LIKE 搜索 FAQ 并构建兜底回答；Qdrant 不可用 → Python 返回空结果，LLM 给通用回答并建议报修；LLM 不可用 → Mock 模式返回模板回答。核心原则：AI 服务失败不返回 500，用户始终能得到一个回答（质量可能降低）。

### Q: fire-and-forget 同步失败了怎么办？

**A:** 同步失败只打 warn 日志，不影响主业务。提供批量同步接口 `/sync-vectors`，管理员可以手动触发重建向量。生产环境可以加定时任务自动重试。

### Q: 如何保证代码质量？

**A:** 1) GitHub Actions CI 每次 push 自动跑 Java 测试 + Python 测试；2) 纯逻辑单元测试覆盖状态机、权限校验、枚举解析；3) Flyway 迁移文件检查防止遗漏；4) 统一响应结构 + 全局异常处理保证接口一致性。

### Q: 如果要支持大文件上传（PDF/Word）怎么做？

**A:** V0.3 计划支持文件上传。技术方案：1) Java 后端接收文件存到对象存储（MinIO/OSS）；2) Python Agent 解析文件内容（PDF 用 PyPDF2，Word 用 python-docx）；3) 按段落切片，每片做 Embedding 存入 Qdrant；4) 搜索时按相似度返回 Top-K 片段。

### Q: 为什么 Java 调 Python 而不是反过来？

**A:** Java 是业务主服务，处理所有 CRUD 和权限。Python 是 AI 辅助服务，只负责向量和 LLM。如果反过来，Python 需要理解所有业务逻辑（工单状态机、权限、租户），耦合太重。Java 调 Python 的方式让 Python 保持简单——接收 question 和 tenantId，返回 answer。

### Q: 文档上传如何保证多租户隔离？

**A:** 三层隔离：1) 文件存储路径按 `tenantId` 隔离：`data/uploads/{tenantId}/{uuid}.txt`；2) knowledge_document 表有 `tenant_id` 字段，所有查询强制带 `tenant_id` 条件；3) 生成的 knowledge_item 也带 `tenant_id`，并且通过 `document_id` 追踪来源。禁止 `selectById` 后再判断 tenant，必须在查询条件中直接过滤。

### Q: 文档解析失败如何处理？

**A:** 解析过程不在数据库事务内。文件保存成功后创建 document 记录（PENDING），然后尝试解析。如果解析失败：1) document 记录更新为 `parse_status=FAILED`，`error_message` 记录具体错误；2) 文件仍然保留在磁盘上，可以后续重试；3) 提供 `POST /{id}/reparse` 接口重新解析。这样即使解析失败，也不会导致整个上传接口 500，用户可以查看失败原因并重试。

### Q: 为什么 MVP 使用本地文件存储，后续如何演进到对象存储？

**A:** MVP 阶段用本地磁盘存储是因为：1) 开发简单，不需要额外基础设施；2) 演示环境数据量小，本地磁盘足够；3) 减少外部依赖，部署更简单。演进路径：抽象一个 `FileStorage` 接口，MVP 实现 `LocalStorage`，后续实现 `S3Storage` 或 `MinioStorage`，上层代码不需要修改。配置切换即可。

### Q: 上传文档如何同步到向量库，失败怎么兜底？

**A:** 每个解析出的段落生成 knowledge_item 后，fire-and-forget 调用 Python Agent 同步到 Qdrant。同步失败只记录 warning 日志，不影响 MySQL 中的条目数据。兜底方式：1) 管理员可以手动触发 `POST /api/admin/knowledge-items/sync-vectors` 批量重建向量；2) 重解析接口会重新同步所有条目；3) 即使向量同步全部失败，Java 的 SQL LIKE 兜底仍能搜索到这些条目。

### Q: 为什么把管理后台和企业门户放在同一个前端项目？

**A:** 两者共享技术栈（React + Ant Design + Axios）、主题 Token、路由机制和 HTTP 封装。分开放会增加构建、部署和依赖管理成本。路由层面天然隔离（`/admin/*` vs `/portal/:tenantCode/*`），不会互相干扰。如果是大型团队，可以按路由懒加载拆分 chunk，但对小项目来说放一个仓库更高效。

### Q: 企业门户如何通过 tenantCode 实现多租户访问？

**A:** URL 路径携带 tenantCode（如 `/portal/TBQTW3G`），前端提取后拼入 API 路径（`/api/public/{tenantCode}/ai/chat`）。后端通过 `TenantService.getByTenantCode()` 查找对应租户，所有后续操作自动隔离到该租户的数据范围。不需要 JWT，因为是公开面向客户的接口。tenantCode 不存在时后端返回 `NOT_FOUND`，前端展示友好提示，不白屏。

### Q: AI 问答如何引导用户从自助解决转为报修？

**A:** AI 返回 `shouldCreateTicket` 和 `matchedItemCount` 两个字段。匹配到知识库条目时（matchedItemCount > 0），前端显示"已参考 N 条知识库资料"；shouldCreateTicket 为 true 时（知识库未覆盖），前端在 AI 回复下方展示橙色提示卡片"建议提交报修"，点击直接跳转报修表单。这个设计体现了"AI 优先，人工兜底"的服务理念。
