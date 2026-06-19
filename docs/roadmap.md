# 版本路线

## V0.1 — 工单 SaaS 后端

**目标：** 跑通完整售后维修流程，可内部演示。

**功能：**
- 企业注册登录（多租户）
- 员工账号管理（ADMIN / DISPATCHER / TECHNICIAN）
- 客户管理（手机号自动合并）
- 工单 CRUD + 派单 + 改派
- 师傅接单 + 提交维修结果
- 工单状态机（PENDING → ASSIGNED → IN_PROGRESS → COMPLETED → CLOSED）
- 工单状态流转日志
- 操作日志
- 统一响应结构 + 全局异常处理
- Flyway 数据库迁移

**技术：** Java 17 + Spring Boot 3.2 + MyBatis-Plus + MySQL + Redis + JWT

---

## V0.2.0 — FAQ 知识库

**目标：** 支持管理员维护 FAQ 知识库。

**功能：**
- 知识库 CRUD（创建、编辑、启用/停用）
- 知识条目 CRUD（标题、问题、答案、产品类型、故障类型）
- 按知识库/关键词筛选

**新增表：** `knowledge_base`、`knowledge_item`

---

## V0.2.1 — AI Chat

**目标：** 客户可以通过 AI 问答获取自助解决方案。

**功能：**
- AI 问答接口（基于 SQL LIKE 搜索 FAQ + LLM 回答）
- 无答案时引导报修（shouldCreateTicket=true）
- AI 对话记录落库
- 后台查看 AI 对话列表和详情
- Python Agent 服务（FastAPI + Mock 模式）

**新增表：** `ai_conversation`、`ai_message`

---

## V0.2.2 — Qdrant RAG

**目标：** 将 FAQ 检索从 SQL LIKE 升级为向量检索，提高语义匹配准确率。

**功能：**
- Qdrant 向量库集成
- Embedding 服务（Mock 模式用 SHA-256，Live 模式用 OpenAI API）
- FAQ 创建/编辑时自动同步向量
- AI 问答优先走 Qdrant 向量检索
- 按 tenant_id + status + product_type + fault_type 过滤
- Python 不可用时 Java 降级 SQL LIKE 兜底
- 手动批量同步向量接口

**新增组件：** Qdrant (Docker)、`vector_service.py`

---

## V0.2.3 — 测试与 CI

**目标：** 建立基础自动化验证，保证代码质量。

**功能：**
- Java 单元测试：TicketStatus 状态机 (23)、TicketPriority (12)、KnowledgeStatus (13)、RoleChecker (12)
- Python 测试：Mock Embedding 确定性/维度/归一化 (5)
- GitHub Actions CI：backend-java + agent-python 两个 Job
- Flyway 迁移文件存在性检查
- .gitignore 完善（__pycache__、egg-info、pytest_cache）

---

## V0.2.4 — 文档与面试沉淀

**目标：** 整理项目文档，适合 GitHub 展示和面试讲解。

**功能：**
- README 重构（项目简介、架构、功能、亮点）
- docs/architecture.md 系统架构（含 Mermaid 图）
- docs/api.md API 文档
- docs/interview.md 面试表达
- docs/roadmap.md 版本路线

---

## V0.3.0 — 文档上传与解析

**目标：** 支持管理员上传文档，自动解析为知识条目并同步向量库。

**功能：**
- 数据库迁移 V5：knowledge_document 表 + knowledge_item.document_id
- 文档上传接口（multipart/form-data，仅支持 .txt / .md，10MB 限制）
- 文档自动解析：按空行切分段落，每段生成一条 knowledge_item
- 自动生成条目后自动同步 Qdrant（fire-and-forget）
- 文档删除：逻辑删除 + 关联条目标记 INACTIVE + Qdrant 同步
- 文档重解析：先删旧条目再重新生成
- 文件安全：UUID 文件名、路径穿越防护、白名单后缀
- 多租户隔离：所有查询带 tenant_id
- Flyway V5 迁移检查
- 文档更新（README、api.md、architecture.md、roadmap.md、interview.md）

---

## V0.3.1 — 前端管理后台 ✅

**目标：** 管理员可通过 Web 界面管理知识库和查看 AI 对话。

**功能：**
- Vite + React 18 + TypeScript + Ant Design 5 + React Router 6
- 登录页（企业编码 + 用户名 + 密码）
- AdminLayout 侧边栏布局（Sider + Sticky Header）
- 知识库管理页（列表、新增、编辑、启用/停用）
- 知识条目管理页（列表、搜索、新增、编辑、禁用、来源标记、向量同步）
- 文档上传页（上传 .txt/.md，流程图展示，状态筛选，重解析，删除）
- AI 对话记录页（列表 + 聊天气泡详情）
- Dashboard 仪表盘（实时统计 + 模块入口）
- Axios 封装（Bearer token、401 重定向、ApiResponse 解包）
- antd 主题 Token 统一配置（ConfigProvider）
- antd 废弃 API 迁移（destroyOnHidden、styles.content、App.useApp()）
- UI 设计优化：企业风格设计系统，非默认模板感
- 浏览器验收（Playwright 全页验证）
- CI 新增 frontend job（Node.js 20 + npm ci + npm run build）
- 文档更新（CLAUDE.md 前端规则、README、api.md）

---

## V0.3.2 — 企业服务门户 ✅

**目标：** 企业客户可通过公开门户自助咨询 AI、提交报修。

**功能：**
- 门户首页（/portal/:tenantCode）：对话式入口 + 3 服务卡片
- AI 智能客服（/portal/:tenantCode/chat）：对话式交互，知识库匹配提示，报修引导
- 提交报修（/portal/:tenantCode/repair）：表单提交，复用已有公开报修 API，成功页展示工单编号
- 查询进度占位（/portal/:tenantCode/query）：后续版本开放提示
- PortalLayout 顶栏导航（区别于管理后台侧边栏）
- 移动端响应式适配
- 无效 tenantCode 错误处理（不白屏，友好提示）
- antd 静态 message 警告修复（MessageBridge + setMessageApi 模式）
- 浏览器验收（Playwright 全流程）
- 文档更新（README、api.md、roadmap.md、interview.md）

---

## V0.4 — 功能增强（规划中）

**规划功能：**
- 师傅端 H5（移动端适配）
- Redis 限流（滑动窗口，防刷）
- 基础统计看板
- Docker Compose 一键部署

---

## V1.0 — 可收费版本（规划中）

**目标：** 可以收取 99～299 元/月。

**规划功能：**
- 稳定多租户隔离
- 套餐限制（员工数量、工单数量）
- 企业名称/Logo 配置
- 基础订阅管理
- 数据备份方案
- API 文档（Knife4j / Swagger）
- 短信/微信通知
