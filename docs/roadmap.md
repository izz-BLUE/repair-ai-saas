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

## V0.3.3 — 商业化交付与租户配置 ✅

**目标：** 从功能 Demo 推进到可出租给企业使用的 SaaS MVP。

**功能：**
- tenant 表新增门户配置 + 商业化字段（portalTitle, portalDescription, logoUrl, themeColor, portalEnabled, max_knowledge_bases, max_documents, max_ai_daily_calls, expired_at）
- SUPER_ADMIN 角色 + PLATFORM 租户
- 平台管理接口（/api/platform/tenants）：租户 CRUD、启用/禁用、重置管理员密码
- 租户门户配置接口（GET/PUT /api/admin/settings）：企业管理员自定义门户标题、描述、电话、Logo、主题色、启用开关
- 公开门户配置接口（GET /api/public/{tenantCode}/portal-settings）
- 租户禁用后端强拦：login 拦截 + JwtAuthenticationFilter 实时校验 tenant.status
- 公开业务接口（AI chat、报修）校验 tenant.status + portal_enabled
- 文档上传 max_documents 限额校验
- 前端企业设置页（/admin/settings）
- 门户品牌化改造（动态主题色、企业名称、Logo、描述）
- 门户停用提示页 + 企业不存在错误页
- 部署交付文档（.env.example、docker-compose.prod.yml、deployment.md、backup.md）

---

## V0.3.4 — 商业化安全与交付收口 ✅

**目标：** 修复出租交付前的安全、初始化、运维风险。

**功能：**
- 租户到期（expired_at）全链路拦截：登录拒绝 + JWT 即时失效 + 公开接口拒绝 + 门户"服务已到期"提示
- 平台重置密码改为随机临时密码（12 位，SecureRandom 生成）
- 创建租户也改为随机临时密码（不再硬编码 Admin@2024）
- 前端用户下拉菜单新增"修改密码"入口（AdminLayout + PlatformLayout）
- max_knowledge_bases 限额落地（创建知识库前检查数量上限）
- AI 日调用量限额（ai_usage_daily 表 + max_ai_daily_calls 检查）
- V7 Flyway 迁移（ai_usage_daily 表）
- 平台管理编辑租户支持设置到期时间
- 部署文档安全检查清单 + 首次部署流程说明

---

## V0.3.5 — 试点客户部署包 ✅

**目标：** 将项目整理为可交付给第一个试点客户的部署包。

**功能：**
- `deploy/scripts/` 部署脚本目录：环境检查、演示数据初始化、备份恢复
- `check-env.sh` 部署前检查：.env 配置、Docker 环境、端口占用、目录可写
- `init-demo-data.sh` 演示数据初始化：纯 API 调用，不直接操作数据库
- 备份脚本：MySQL / 上传文件 / Qdrant 分别备份，自动创建目录，不打印密码
- `restore-mysql.sh` 数据库恢复：二次确认机制，防止误操作
- `deploy/nginx/repair-ai.conf` Nginx 反向代理参考模板
- `.env.example` 增强：补充域名/端口/目录等部署变量，标注生产必须替换项
- `docker-compose.prod.yml` 修复为纯 YAML（去掉 markdown 包裹）
- `docs/trial-customer-onboarding.md` 试点客户交付流程文档
- `docs/pricing-and-limits.md` 套餐与限额建议
- README 新增"试点部署""商业化能力""部署脚本"章节

---

## V0.4.0 — 工单闭环增强 ✅

**目标：** 客户报修后可查询进度，后台可管理工单，看板可展示工单状态。

**功能：**
- 公开工单查询接口（GET /api/public/{tenantCode}/tickets/query）
  - 手机号+工单号双重匹配
  - 返回脱敏数据（手机号/地址）
  - 包含状态时间线
  - 跨租户隔离 + 防手机号枚举
- 门户查询进度页落地（/portal/:tenantCode/query）
  - 表单查询 → 结果展示（状态/时间线/师傅信息）
  - 沿用门户 raw HTML/CSS 风格
- 后台工单管理页面（/admin/tickets）
  - 状态/优先级筛选 + 关键词搜索
  - 详情 Drawer + 处理记录时间线
  - 操作按钮：派单/取消/关闭
- Dashboard 工单统计
  - 今日新增 / 待处理 / 处理中 / 已完成
  - 今日 AI 对话数
  - 工单管道统计卡片
- 手机号脱敏工具类（PhoneMasker：138****5678）
- Flyway V0.4 无新迁移（复用现有表结构）

**技术：** Java 17 + Spring Boot 3.2, React 18 + TypeScript + Ant Design 5

**新增表：** 无

---

## V0.4.1+ — 后续规划

---

## V0.4.2 — 维修师傅移动端 H5 ✅

**目标：** 维修师傅可通过手机登录系统，查看分配给自己的工单，开始处理并完成维修，形成生产可用的售后履约闭环。

**功能：**
- 师傅端前端路由：/technician/tickets 列表 + /technician/tickets/:id 详情
- TECHnicianLayout 移动端布局（顶栏 + 角色守卫）
- 卡片式工单列表（状态筛选：全部/已派单/处理中/已完成/已关闭）
- 工单详情（客户信息卡片 + 工单信息卡片 + 维修结果卡片 + 状态时间线）
- 操作按钮：ASSIGNED → 开始处理, IN_PROGRESS → 完成维修
- 完成维修表单（维修结果必填、配件说明选填、费用说明选填、备注选填）
- 电话 click-to-call（tel:）+ 地址复制
- 登录跳转规则：TECHNICIAN → /technician/tickets
- 前端权限守卫：TECHNICIAN 无法访问 /admin/*，ADMIN 无法访问 /technician/*
- 后端 completeTicket 支持 remark 参数（写入状态日志）
- 后端权限完整：requireTechnician() + technicianId 所有权校验 + tenantId 隔离

**新增后端接口：** 无（复用 V0.1 已有 `/api/technician/*`）

**新增前端页面：** 3 个（TechnicianLayout + TicketsPage + TicketDetailPage）

---

## V0.4.3 — 小程序化方案设计 ✅

**目标：** 分析现有 API 对微信小程序的适配情况，输出产品设计方案，不实现代码。

**产出：**
- [docs/miniapp-plan.md](miniapp-plan.md) — 小程序产品设计方案
- [docs/miniapp-api-gap.md](miniapp-api-gap.md) — 接口适配分析与缺口
- README 新增"移动端与小程序规划"章节
- roadmap 新增 V0.5.0+ 小程序开发路线

**核心结论：**
- 不把维修系统塞进 eat-what 小程序
- 新建独立售后维修小程序
- 现有 API 对 MVP 适配度 **100%**（P0 无缺失接口）
- 小程序优先承载客户和师傅两类角色
- 管理员和平台管理员继续使用 Web 后台
- H5 客户端和 H5 师傅端作为备用入口保留

---

## V0.5.0 — 小程序 MVP ✅

**目标：** 上线独立售后维修小程序，客户和师傅可通过微信扫码使用。

**目录：** `miniapp-repair/`（原生微信小程序 WXML/WXSS/JS）

**客户侧 MVP 页面：**
- 企业首页（品牌展示 + 3 服务入口卡片）
- AI 智能咨询（聊天气泡 + 知识匹配 + 报修引导）
- 提交报修表单（产品类型/姓名/手机/地址/故障描述 + 校验 + 成功展示工单号）
- 查询进度（工单号+手机号 → 状态/时间线/师傅信息）

**师傅侧 MVP 页面：**
- 师傅登录（企业编码+用户名+密码，仅允许 TECHNICIAN）
- 我的工单列表（Tab 状态筛选 + 卡片列表）
- 工单详情（客户信息/工单信息/时间线 + 操作按钮）
- 完成工单（维修结果必填 + 配件/费用/备注选填）

**工具模块：** config.js, request.js（统一封装 401/token/业务错误）, auth.js, tenant.js, status.js

**技术要求：**
- 微信小程序原生 WXML/WXSS/JS，不使用 Taro/uni-app
- 复用全部现有后端 API（无新增接口）
- tenantCode 通过二维码 scene 参数传递
- 师傅登录复用 JWT，token 存 wx.storage
- 客户侧不强制登录
- project.config.json 使用 TODO_APPID 占位
- 设计规范遵循 .claude/skills/wechat-miniapp-ui-design/SKILL.md

---

## V0.5.1 — 图片上传（计划中）

- 报修图片上传（客户拍照上传故障照片）
- 完工图片上传（师傅上传维修前后对比照片）
- 新增 `ticket_attachment` 表
- 工单详情返回图片 URL 列表

---

## V0.5.2 — 微信能力接入（计划中）

- 微信手机号授权（客户免填手机号）
- 微信 openid 绑定师傅账号（免密登录）
- 订阅消息：派单提醒 + 状态变更通知

---

## V0.5.3 — 公开接口限流 + 生产部署验证 ✅

**目标：** 对公开接口实施 Redis 滑动窗口限流，保护登录/报修/查询/AI 聊天不被滥用。

**功能：**
- 公开接口 Redis 限流落地（4 个端点）
  - `POST /api/public/login` — IP 限流（1min/5 次）
  - `POST /api/public/{tenantCode}/repair-requests` — IP + 租户限流（1min/3 次 + 10 次）
  - `GET /api/public/{tenantCode}/tickets/query` — IP + 租户限流（1min/10 次 + 30 次）
  - `POST /api/public/{tenantCode}/ai/chat` — IP 限流（1min/5 次，租户日限额由 AiUsageService 负责）
- RateLimiter 组件（ZSET 滑动窗口 + Lua 脚本）
- 429 统一响应：code=`TOO_MANY_REQUESTS`, HTTP 429, 不暴露阈值和维度
- Redis 异常 fail-open（不因 Redis 故障阻塞业务）
- IP 获取工具：X-Forwarded-For → X-Real-IP → remoteAddr
- 新增测试 27 个（RateLimiterTest 20 + GlobalExceptionHandlerTest 7），全部通过
- 生产 docker compose config 验证通过
- 生产部署验证文档（docs/deployment-verification.md）
- 限流设计文档更新（标记 V0.5.3 已实现）

**技术：** Java 17 + Spring Data Redis + Lua 脚本

**新增表：** 无

**新增类：** `common/RateLimiter.java`

---

## V0.5.4 — 验收准备与演示数据补齐 ✅

**目标：** 为生产环境验收和小程序师傅端验收补齐准备材料。

**变更：**
- `init-demo-data.sh` 新增师傅测试账号（`technician1 / Tech@2024`，角色 TECHNICIAN）
- `deployment-verification.md` 补充全流程 smoke test 清单（16 项，标注"待实际执行"）
- `miniapp-devtools-checklist.md` 更新验收版本号 + 补充 V0.5.2/V0.5.3 已修复项

**技术：** 纯文档 + seed 脚本，无代码变更

**新增表：** 无

---

## V0.5.5 — 本地全流程 Smoke Test ✅

**目标：** 本地端到端全流程验证，修 bug 不新增功能。

**执行：**
- 16 项 smoke test 全流程验证通过：客户报修 → 后台派单 → 师傅处理 → 完工 → 客户查询状态同步
- 发现并修复 Redis 7.4 Lua 脚本类型兼容 bug（RateLimiter 限流完全不生效）
- RateLimiter Lua 脚本 → ZSetOperations + SessionCallback（Redis 事务）
- 4 个限流端点全部验证 429 生效（登录/报修/查询/AI 聊天）
- AI Chat 降级验证通过（agent-python 不可用时返回兜底回答，不报 500）
- Java 测试 113/113 全通过

**修复文件：** `common/RateLimiter.java`, `RateLimiterTest.java`

**新增表：** 无

**环境限制：** 小程序师傅端未验收（当前环境无微信开发者工具），Python Agent 因 Windows grpc DLL 兼容问题无法启动

---

## V0.5.6 — 小程序师傅端 DevTools 验收 ✅

**目标：** 在微信开发者工具中完成师傅端全流程验收。

**执行：**
- tenantCode: TA7P55N，technician: technician1 / Zhang Shifu
- 工单号: TK202606230005
- 师傅端全流程通过：登录 → 工单列表 → 工单详情 → 开始处理 → 完成工单
- 客户查询状态同步验证通过：状态变为"已完成"，时间线完整（待处理/已派单/处理中/已完成）
- 纯文档更新，无业务代码变更

**已知问题（待修复）：**
- Web 后台派单弹窗曾提示 500 错误，但工单实际已进入已派单状态。初步怀疑 appointmentTime 格式、前端错误处理或后端响应异常

**修改文件：** 仅文档（deployment-verification.md, miniapp-devtools-checklist.md, roadmap.md, README.md）

---

## V0.5.7 — Web 派单弹窗 500 修复 ✅

**目标：** 修复 Web 后台工单详情点击"派单"时填写预约时间返回 500 的问题。

**修复：**
- 前端 `scheduledTime` 格式从 `YYYY-MM-DD HH:mm:ss`（空格分隔）改为 `YYYY-MM-DDTHH:mm:ss`（ISO-8601 T 分隔）
- 后端 `AssignRequest.scheduledTime` 添加 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 兼容空格分隔格式
- `GlobalExceptionHandler` 新增 `HttpMessageNotReadableException` 处理器，返回 400 而非 500
- `TicketController.assignTicket` 中操作日志记录改为 try-catch，防止日志写入失败影响派单响应

**修改文件：** `TicketPage.tsx`, `TicketController.java`, `GlobalExceptionHandler.java`

**新增表：** 无

---

## V0.6.0 — 生产 Docker 部署验证 ✅

**目标：** 在生产 Docker Compose 环境中完成完整构建、启动、健康检查与业务 smoke test。

**前置修复（V0.6.0 阶段内）：**
- agent-python Dockerfile: 修复 COPY + pip install 顺序，补充 pyproject.toml `[build-system]`，新增 `.dockerignore`
- application-prod.yml: 修复 4 处 Spring 占位符 `${VAR:-default}` → `${VAR:default}`，根因 username 为 `-root` 导致 Flyway 连接被拒
- JwtAuthenticationFilter: `/api/health` 加入 PUBLIC_PATHS 白名单，新增 `HealthController` 返回 `{"status":"UP"}`
- init-demo-data.sh: 8 处 `python3 -c` JSON 解析改为 `jq -r`，新增 curl + jq 依赖检查

**生产 Docker 验证结果：**

| # | 检查项 | 结果 |
|---|--------|------|
| 1 | `docker compose config --quiet` | ✅ PASS |
| 2 | `docker compose build backend` | ✅ PASS |
| 3 | `docker compose build agent-python` | ✅ PASS |
| 4 | `docker compose up -d`（6 个 service） | ✅ 全部启动 |
| 5 | repair-mysql healthy | ✅ |
| 6 | repair-redis PING | ✅ PONG |
| 7 | repair-qdrant healthz | ✅ ok |
| 8 | repair-backend Started | ✅ Flyway 无 ERROR |
| 9 | repair-agent Uvicorn running | ✅ qdrant connected |
| 10 | repair-nginx 前端首页 | ✅ 返回 HTML |
| 11 | `curl /api/health` | ✅ `{"status":"UP"}` |
| 12 | `curl /api/public/PLATFORM/portal-settings` | ✅ 200 JSON |
| 13 | `init-demo-data.sh` 执行 | ✅ 8 步全部成功 |
| 14 | 生产 smoke test（8 步业务闭环） | ✅ 全部通过 |

**修改文件：** `agent-python/Dockerfile`, `agent-python/pyproject.toml`, `agent-python/.dockerignore`, `application-prod.yml`, `JwtAuthenticationFilter.java`, `HealthController.java`, `JwtAuthenticationFilterTest.java`, `init-demo-data.sh`

**新增表：** 无

**环境说明：** 验证环境为 Linux Docker，未涉及 HTTPS 证书、公网域名、云服务器。微信小程序生产发布未验证。

---

## V0.5.5+ — 后续规划

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
