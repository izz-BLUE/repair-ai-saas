# 生产部署验证文档

> 版本：V0.6.0
> 创建日期：2026-06-22
> 最后更新：2026-06-23

## 部署前验证

### 1. 基础环境

```bash
# 检查 Docker 运行状态
docker --version
docker compose version

# 检查端口占用
# 生产部署默认端口: 80 (Nginx), 3306 (MySQL), 6379 (Redis), 6333 (Qdrant), 8080 (Backend), 8090 (Agent)
```

### 2. 环境变量检查

```bash
# 确保 .env 已配置（从 .env.example 复制）
cp .env.example .env
# 必须修改:
#   MYSQL_ROOT_PASSWORD
#   JWT_SECRET (openssl rand -base64 32)
#   LLM_API_KEY (如需 AI 功能)
#   EMBEDDING_API_KEY (如需向量检索)
```

### 3. Docker Compose 配置检查

```bash
docker compose -f docker-compose.prod.yml config --quiet
# 返回 0 = 配置正确
```

### 4. 安全绑定验证

```bash
docker compose -f docker-compose.prod.yml config | grep "host_ip:"
# 预期输出 5 行 127.0.0.1 (mysql/redis/qdrant/backend/agent-python)
# nginx 不出现 host_ip 限制（绑定 0.0.0.0 供外部访问）
```

## 构建与启动

### 1. 构建前端

```bash
cd frontend
npm install
npm run build
# 生成 dist/ 目录
```

### 2. 构建镜像

```bash
# 构建 backend（Maven 多阶段构建）
docker compose -f docker-compose.prod.yml build backend

# 构建 agent-python
docker compose -f docker-compose.prod.yml build agent-python

# 预期:
#   backend: Maven 下载依赖 + 编译打包 → JRE 运行时镜像
#   agent-python: pip install → uvicorn 运行镜像
```

### 3. 启动服务

```bash
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps
```

### 4. 等待健康检查

```bash
# MySQL healthcheck 需要 ~10 秒
docker compose -f docker-compose.prod.yml ps mysql
# Healthy = 启动成功
```

## 启动后验证清单

### 基础设施

| # | 检查项 | 命令/操作 | 预期 |
|---|--------|----------|------|
| 1 | MySQL 可连接 | `docker compose exec mysql mysqladmin ping -u root -p` | `mysqld is alive` |
| 2 | Redis 可连接 | `docker compose exec redis redis-cli PING` | `PONG` |
| 3 | Qdrant 可连接 | `curl http://localhost:6333/healthz` | `ok` |
| 4 | Backend 启动 | `docker compose logs backend \| grep -i "Started"` | `Started Application in ...` |
| 5 | Agent 启动 | `docker compose logs agent-python \| grep -i "Uvicorn"` | `Uvicorn running on ...` |
| 6 | Flyway 迁移成功 | `docker compose logs backend \| grep -i flyway` | 无 ERROR |
| 7 | Nginx 代理正常 | 浏览器访问根路径 | 出现前端页面 |
| 8 | API 可访问 | `curl http://localhost/api/public/PLATFORM/portal-settings` | 返回 JSON |
| 9 | Backend 健康检查 | `curl http://localhost/api/health` | `{"status":"UP","service":"repair-ai-saas-backend"}` |

### 安全验证

| # | 检查项 | 操作 | 预期 |
|---|--------|------|------|
| 1 | MySQL 不暴露公网 | 外部访问 `telnet <host> 3306` | 拒绝连接 |
| 2 | Redis 不暴露公网 | 外部访问 `telnet <host> 6379` | 拒绝连接 |
| 3 | Qdrant 不暴露公网 | 外部访问 `telnet <host> 6333` | 拒绝连接 |
| 4 | Backend 不暴露公网 | 外部访问 `telnet <host> 8080` | 拒绝连接 |
| 5 | Agent 不暴露公网 | 外部访问 `telnet <host> 8090` | 拒绝连接 |
| 6 | 仅 Nginx 暴露 | 外部访问 `telnet <host> 80` | 连接成功 |

### 功能冒烟测试

> ⚠️ 以下为完整业务流程 smoke test 清单。
> 状态：🟢 V0.5.5 本地环境已执行（2026-06-23），V0.5.6 小程序师傅端 DevTools 已执行。

#### V0.5.5 本地端到端 Smoke Test 结果

| # | 操作 | 预期 | 结果 |
|---|------|------|------|
| 1 | 访问门户 `/portal/{tenantCode}` | 显示企业门户首页 | ✅ PASS |
| 2 | 访问管理后台 `/admin/login` | 显示登录页面 | ✅ PASS |
| 3 | 使用默认平台管理员登录 | 登录成功，跳转 Dashboard | ✅ PASS |
| 4 | 平台管理创建租户 | 返回 tenantCode + 随机密码 | ✅ PASS |
| 5 | 使用新租户管理员登录 | 登录成功 | ✅ PASS |
| 6 | 提交 AI 对话 | 返回 AI 回答（或降级兜底回答） | ✅ PASS（降级兜底） |
| 7 | 客户提交报修 | 返回工单编号 | ✅ PASS（TK202606230001） |
| 8 | 查询工单进度 | 返回工单详情（脱敏） | ✅ PASS（手机号 138****1234） |
| 9 | 创建师傅账号（TECHNICIAN 角色） | 创建成功 | ✅ PASS（id=47） |
| 10 | 后台派单给师傅 | 工单状态变为 ASSIGNED | ✅ PASS |
| 11 | 师傅登录小程序/H5 | 登录成功，跳转工单列表 | ✅ PASS |
| 12 | 师傅查看分配给自己的工单 | 列表仅显示本人工单 | ✅ PASS |
| 13 | 师傅开始处理（ASSIGNED → IN_PROGRESS） | 状态变更，时间线更新 | ✅ PASS |
| 14 | 师傅完成工单（IN_PROGRESS → COMPLETED） | 状态变更，维修结果已记录 | ✅ PASS |
| 15 | 客户查询工单 → 状态同步为"已完成" | 返回 COMPLETED + 完整时间线 | ✅ PASS（4 条时间线） |
| 16 | AI 对话降级验证（如 Agent 未启动） | 返回 SQL LIKE 兜底回答，不报 500 | ✅ PASS |

#### V0.5.6 小程序师傅端 DevTools 验收结果

| # | 操作 | 预期 | 结果 |
|---|------|------|------|
| 1 | 师傅登录（technician1） | 登录成功，跳转工单列表 | ✅ PASS |
| 2 | 工单列表 Tab 筛选 | Tab 切换正常，仅显示本人工单 | ✅ PASS |
| 3 | 工单详情查看 | 客户信息+工单信息+时间线完整显示 | ✅ PASS |
| 4 | 开始处理（ASSIGNED → IN_PROGRESS） | 状态变更成功 | ✅ PASS |
| 5 | 完成工单（IN_PROGRESS → COMPLETED） | 维修结果提交成功 | ✅ PASS |
| 6 | 客户查询状态同步 | 查询页状态变为"已完成"，时间线完整 | ✅ PASS |

**验收工单：** TK202606230005，tenantCode: TA7P55N

**已知问题（已修复 V0.5.7）：**
- ~~Web 后台派单弹窗曾提示服务器内部错误 / 500，但工单实际已进入已派单状态。初步怀疑 appointmentTime 格式、前端错误处理或后端响应异常。~~ → V0.5.7 已修复：前端改为 ISO-8601 T 分隔格式，后端添加 @JsonFormat 兼容 + HttpMessageNotReadableException 处理器。该问题不影响小程序师傅端验收结论，需后续单独排查修复。

### 限流验证

> V0.5.5 本地已全项通过（4/4）。

| # | 操作 | 预期 | 结果 |
|---|------|------|------|
| 1 | 30 秒内连续登录失败 6 次 | 第 6 次返回 429 | ✅ PASS |
| 2 | 1 分钟内提交报修 4 次 | 第 4 次返回 429 | ✅ PASS |
| 3 | 1 分钟内查询工单 11 次 | 第 11 次返回 429 | ✅ PASS |
| 4 | 1 分钟内 AI 对话 6 次 | 第 6 次返回 429 | ✅ PASS |

### V0.6.0 生产 Docker 部署验证

> 🟢 2026-06-23 生产 Docker 环境全栈验证通过。

#### 构建与启动

| # | 检查项 | 命令/操作 | 结果 |
|---|--------|----------|------|
| 1 | docker compose config 有效 | `docker compose --env-file .env.prod.local -f docker-compose.prod.yml config --quiet` | ✅ PASS |
| 2 | backend 镜像构建 | `docker compose build backend` | ✅ PASS |
| 3 | agent-python 镜像构建 | `docker compose build agent-python` | ✅ PASS |
| 4 | 全栈启动（6 service） | `docker compose up -d` | ✅ 全部 Running |
| 5 | MySQL healthy | `docker compose ps mysql` | ✅ healthy |
| 6 | Redis PING | `docker compose exec redis redis-cli PING` | ✅ PONG |
| 7 | Qdrant healthz | `curl http://localhost:6333/healthz` | ✅ ok |
| 8 | Backend Started | `docker compose logs backend` | ✅ Flyway 无 ERROR |
| 9 | Agent Uvicorn | `docker compose logs agent-python` | ✅ Uvicorn running |
| 10 | Agent health | agent `/health` 端点 | ✅ qdrant connected |
| 11 | Nginx 前端 | `curl http://localhost` | ✅ 返回 HTML |
| 12 | `/api/health` | `curl http://localhost/api/health` | ✅ `{"status":"UP"}` |
| 13 | `/api/public/...` | `curl http://localhost/api/public/PLATFORM/portal-settings` | ✅ 200 JSON |
| 14 | init-demo-data.sh | `bash deploy/scripts/init-demo-data.sh` | ✅ 8 步全部成功 |

#### 生产 Smoke Test

| # | 操作 | 预期 | 结果 |
|---|------|------|------|
| 1 | 平台管理员登录 | 返回 token | ✅ PASS |
| 2 | 创建演示租户 | 返回 tenantCode + 随机密码 | ✅ PASS |
| 3 | 租户管理员登录 | 返回 token | ✅ PASS |
| 4 | 客户提交报修 | 返回工单编号 | ✅ PASS |
| 5 | 后台派单给师傅 | 工单状态变为 ASSIGNED | ✅ PASS |
| 6 | 师傅开始处理 | ASSIGNED → IN_PROGRESS | ✅ PASS |
| 7 | 师傅完成工单 | IN_PROGRESS → COMPLETED | ✅ PASS |
| 8 | 客户查询状态同步 | 返回 COMPLETED + 时间线完整 | ✅ PASS |

#### V0.6.0 已修复的生产问题

| # | 问题 | 根因 | 修复 |
|---|------|------|------|
| 1 | Backend 一直 Restarting，日志 `Access denied for user '-root'` | `application-prod.yml` 使用 bash 语法 `${DB_USERNAME:-root}`，Spring 解析默认值为 `-root` | 4 处 `${VAR:-default}` → `${VAR:default}` |
| 2 | `curl /api/health` 返回 401 UNAUTHORIZED | `JwtAuthenticationFilter.PUBLIC_PATHS` 未包含 `/api/health` | 加入白名单 + 新增 `HealthController` |
| 3 | agent-python Dockerfile 构建失败 | `COPY pyproject.toml ./` 后 `pip install .` 时 `app/` 源码缺失 | 改为 `COPY . .` 后 `pip install .` |
| 4 | init-demo-data.sh 依赖 python3 | JSON 解析用 `python3 -c` | 改为 `jq -r`，启动时检查 curl + jq |

#### 环境声明

- 验证环境：Linux Docker（非 Windows Docker Desktop）
- 未涉及：HTTPS 证书、公网域名、云服务器
- 未验证：微信小程序生产发布

## 首次部署后必须操作

1. **立即修改平台管理员密码**（用户下拉菜单 → 修改密码）
2. **清理 .env.example 中的默认密码**输出
3. **配置定时备份**（`deploy/scripts/backup-mysql.sh` + crontab）
4. **检查 DNS 解析**（如有域名）
5. **配置 HTTPS**（`deploy/nginx/repair-ai.conf` SSL 段）

## 常见故障排查

| 现象 | 可能原因 | 解决方案 |
|------|---------|---------|
| `docker compose build` 失败 | Docker Hub 网络不通 | 配置镜像加速器或代理 |
| MySQL 容器不停重启 | 数据卷权限不足 | `chmod -R 777 mysql_data/` |
| Backend 启动后立即退出 | JWT_SECRET 未配置 | 编辑 .env 设置 JWT_SECRET |
| AI 回答返回"当前知识库中没有足够信息" | Python Agent 未启动 | `docker compose up -d agent-python` |
| 429 错误频繁出现 | 限流阈值过于严格 | 检查日志，确认是否被误拦 |
| Backend 启动后一直 Restarting，日志 `Access denied for user '-root'` | `application-prod.yml` 占位符用 bash 语法 `${VAR:-default}` 而非 Spring `${VAR:default}` | 所有 `:-` 改为 `:`（V0.6.0 已修复） |
| `/api/health` 返回 401 | JWT Filter 白名单未包含 `/api/health` | 加入 `PUBLIC_PATHS`（V0.6.0 已修复） |
| agent-python 构建失败 | Dockerfile 先 `COPY pyproject.toml` 后 `pip install .`，源码缺失 | 改为 `COPY . .` 后安装（V0.6.0 已修复） |

## Docker 镜像加速

如果 Docker Hub 连接不稳定，可配置镜像加速：

```json
// /etc/docker/daemon.json (Linux) 或 Docker Desktop Settings (Windows/Mac)
{
  "registry-mirrors": [
    "https://docker.1panel.live",
    "https://hub.rat.dev"
  ]
}
```
