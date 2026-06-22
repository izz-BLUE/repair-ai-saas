# 生产部署验证文档

> 版本：V0.5.3+
> 创建日期：2026-06-22

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

> ⚠️ 以下为完整业务流程 smoke test 清单，**待实际生产环境执行和确认**。
> 当前状态：📋 已规划，未执行。

| # | 操作 | 预期 |
|---|------|------|
| 1 | 访问门户 `/portal/{tenantCode}` | 显示企业门户首页 |
| 2 | 访问管理后台 `/admin/login` | 显示登录页面 |
| 3 | 使用默认平台管理员登录 | 登录成功，跳转 Dashboard |
| 4 | 平台管理创建租户 | 返回 tenantCode + 随机密码 |
| 5 | 使用新租户管理员登录 | 登录成功 |
| 6 | 提交 AI 对话 | 返回 AI 回答（或降级兜底回答） |
| 7 | 客户提交报修 | 返回工单编号 |
| 8 | 查询工单进度 | 返回工单详情（脱敏） |
| 9 | 创建师傅账号（TECHNICIAN 角色） | 创建成功 |
| 10 | 后台派单给师傅 | 工单状态变为 ASSIGNED |
| 11 | 师傅登录小程序/H5 | 登录成功，跳转工单列表 |
| 12 | 师傅查看分配给自己的工单 | 列表仅显示本人工单 |
| 13 | 师傅开始处理（ASSIGNED → IN_PROGRESS） | 状态变更，时间线更新 |
| 14 | 师傅完成工单（IN_PROGRESS → COMPLETED） | 状态变更，维修结果已记录 |
| 15 | 客户查询工单 → 状态同步为"已完成" | 返回 COMPLETED + 完整时间线 |
| 16 | AI 对话降级验证（如 Agent 未启动） | 返回 SQL LIKE 兜底回答，不报 500 |

### 限流验证

| # | 操作 | 预期 |
|---|------|------|
| 1 | 30 秒内连续登录失败 6 次 | 第 6 次返回 429 |
| 2 | 1 分钟内提交报修 4 次 | 第 4 次返回 429 |
| 3 | 1 分钟内查询工单 11 次 | 第 11 次返回 429 |
| 4 | 1 分钟内 AI 对话 6 次 | 第 6 次返回 429 |

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
