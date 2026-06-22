# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

AI 售后维修工单 SaaS。Java 后端 + Python AI 服务 + React 管理后台。使用简体中文编写文档和注释。

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

# 启动前端开发服务器（端口 3000，代理 /api → localhost:8080）
cd frontend
npm install
npm run dev

# 前端构建
npm run build
```

项目无单元测试，无 lint 工具配置。数据库 Schema 由 Flyway 在首次启动时自动迁移（`backend-java/src/main/resources/db/migration/`）。

## 技术栈

**后端：**
- Java 17 + Spring Boot 3.2.7
- MyBatis-Plus 3.5.7（ORM，自动填充、逻辑删除、分页）
- MySQL 8.0（utf8mb4）+ Redis 7
- Flyway（数据库迁移）
- JWT 认证（jjwt 0.12.6），**不使用 Spring Security 框架**，仅引入 spring-security-crypto 做 BCrypt
- Lombok

**前端：**
- React 18 + TypeScript
- Vite（构建工具，端口 3000）
- Ant Design 5（UI 组件库）
- React Router 6（路由）
- Axios（HTTP 请求，自动携带 Bearer token）

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
    ├── tenant/      # 企业注册、租户码生成、门户配置管理
    ├── user/        # 员工 CRUD、登录
    ├── customer/    # 客户管理（同租户手机号自动合并）
    ├── ticket/      # 工单（核心模块），含 enums/ (TicketStatus, TicketPriority)
    ├── knowledge/   # FAQ 知识库 + 文档上传解析
    ├── ai/          # AI 问答 + 对话记录
    ├── operation/   # 操作日志，含 enums/ (OperationType, Role)
    └── platform/    # 平台管理（SUPER_ADMIN 专用）

frontend/src/
├── api/             # Axios 封装（http.ts）+ API 函数（auth/knowledge/documents/ai）
├── layouts/         # AdminLayout（固定侧边栏 + sticky 顶栏）
└── pages/           # 登录、仪表盘、知识库、条目、文档、AI 对话
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
- 角色：SUPER_ADMIN（平台管理员）、ADMIN（租户管理员）、DISPATCHER（客服/调度）、TECHNICIAN（师傅）
- API 路径按角色分组：`/api/platform/`（SUPER_ADMIN）、`/api/admin/`、`/api/technician/`、`/api/common/`、`/api/public/`

**统一响应**：所有接口返回 `ApiResponse<T>`，包含 code/message/data。

**数据库**：核心表含 `deleted` 逻辑删除字段，`created_at`/`updated_at` 由 MyBatis-Plus 自动填充。

## 环境配置

- 主配置：`backend-java/src/main/resources/application.yml`
- 开发配置：`backend-java/src/main/resources/application-dev.yml`（MySQL localhost:3307/root/root）
- JWT 过期时间：24 小时
- 时区：Asia/Shanghai

## 前端开发规则

### 设计工具

- **所有前端 UI、页面、布局、视觉优化必须使用 `frontend-design` skill。** 不允许绕过该 skill 直接写 UI 代码。
- **所有前端页面开发、联调、验收必须使用 `webapp-testing` skill。** 如果当前环境无法安装 `webapp-testing`，则使用 `playwright` plugin 作为等效替代，但验收标准不降低。

### 验收标准

每次前端改动后，不能只跑 `npm run build`，必须实际浏览器验收以下全部项目：

- [ ] 页面不白屏
- [ ] console 无错误（antd 废弃警告也算错误，必须修复）
- [ ] 路由跳转正常（菜单点击、登录跳转、404 重定向）
- [ ] 登录 / token 持久化 / 退出登录正常
- [ ] 表单可输入、提交、校验提示正常
- [ ] 表格分页 / 筛选 / 排序可用
- [ ] 上传、删除、重解析、详情弹窗等关键交互可用
- [ ] loading / empty / error 状态正常显示
- [ ] 输出 Playwright 或 webapp-testing 验收摘要

验收流程：Playwright 导航到每个页面 → 检查 console errors → 截图 → 测试关键交互 → 输出报告。

### 设计标准

- **避免默认 Ant Design 模板感**：不能是白底表格堆砌，必须有设计意图
- **保持企业售后 AI 服务平台风格**：专业、可信、清爽、智能
- **可优化方向**：品牌感、状态体系、空状态、微交互、信息层级
- **不为了视觉牺牲后台效率和可维护性**：不引入重依赖、不过度动效、保持代码简洁

### 技术规范

- 使用 antd `App.useApp()` 获取 `message` / `notification` 实例（不使用静态函数）
- 使用 `destroyOnHidden` 替代废弃的 `destroyOnClose`
- 使用 `styles.content` 替代废弃的 `valueStyle`
- 不使用废弃的 `List` 组件（用 div + map 替代）
- 全局主题 Token 统一在 `App.tsx` 的 `ConfigProvider` 中配置

## 小程序开发规则（V0.5.0+）

### 设计工具

- **所有小程序 UI、页面、布局、视觉优化必须使用 `wechat-miniapp-ui-design` skill。** 不允许绕过该 skill 直接写小程序 UI 代码。
- 如果 skill 不可用，必须至少遵守 `docs/miniapp-plan.md` 中的小程序 UI 规范。
- **小程序代码目录：** `miniapp-repair/`，使用原生微信小程序 WXML / WXSS / JavaScript，不使用 Taro / uni-app。

### 安全规则

- **不提交真实 AppID**：`project.config.json` 使用 `touristappid` 或 `TODO_APPID` 占位
- **不提交真实域名**：`utils/config.js` 使用 `http://localhost:8080` 占位
- **不提交真实密钥/secret**：永远不写入代码，用注释标注 `<!-- TODO: 生产环境配置 -->`
- **不调用 admin/platform API**：小程序只调用 `/api/public/` 和 `/api/technician/` 接口
- **不合并进 eat-what 小程序**：售后维修是独立小程序

### 验收标准

每次小程序改动后：

- [ ] 目录结构完整（app.js / app.json / app.wxss / project.config.json / sitemap.json）
- [ ] app.json 页面注册完整
- [ ] 所有页面四件套（WXML / WXSS / JS / JSON）存在
- [ ] utils/request.js 统一封装 wx.request
- [ ] 无真实 AppID / 密钥 / 域名（grep 检查）
- [ ] npm run build 成功（前端 Web 不受影响）
- [ ] mvn clean test 成功（后端不受影响）

> 当前开发环境可能无法运行微信开发者工具。如果未做微信开发者工具验收，必须明确说明，不要假装通过。
