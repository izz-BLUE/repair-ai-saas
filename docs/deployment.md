# 部署文档

## 本地开发

### 启动基础设施

```bash
docker compose up -d
# MySQL :3307, Redis :6379, Qdrant :6333
```

### 启动 Java 后端

```bash
cd backend-java
mvn spring-boot:run
# http://localhost:8080
```

### 启动 Python Agent（可选）

```bash
cd agent-python
pip install -e .
uvicorn app.main:app --host 0.0.0.0 --port 8090
```

### 启动前端

```bash
cd frontend
npm install
npm run dev
# http://localhost:3000
```

## 生产部署

### 前置条件

- Docker + Docker Compose
- 域名 + SSL 证书（可选）

### 步骤

1. 复制环境变量：

```bash
cp .env.example .env
# 编辑 .env，修改所有密码和 API Key
```

2. 构建前端：

```bash
cd frontend
npm install
npm run build
# 生成 dist/ 目录
```

3. 启动服务：

```bash
docker compose -f docker-compose.prod.yml up -d
```

4. 验证：

```bash
curl http://localhost:8080/api/public/PLATFORM/portal-settings
```

5. **首次部署后立即修改平台管理员密码！**

默认账号：
- 企业编码：`PLATFORM`
- 用户名：`superadmin`
- 密码：`Admin@2024`

> **⚠️ 安全警告：** 默认密码仅用于首次初始化。生产部署后必须立即通过管理后台修改密码，否则存在严重安全风险。

### 生产部署安全检查清单

| 项目 | 说明 | 风险等级 |
|------|------|---------|
| 修改默认密码 | 平台管理员 + 所有租户管理员首次登录后必须修改密码 | 🔴 高 |
| 替换 JWT_SECRET | `.env` 中的 `JWT_SECRET` 必须替换为随机 Base64 密钥 | 🔴 高 |
| 配置 LLM_API_KEY | 未配置时 AI 服务降级为 SQL LIKE 兜底，功能受限 | 🟡 中 |
| 配置 HTTPS | Nginx 反向代理必须启用 SSL，防止 JWT 和密码明文传输 | 🔴 高 |
| 数据库备份 | MySQL + Qdrant + 上传文件必须配置定时备份 | 🔴 高 |
| 租户到期管理 | 通过平台管理设置 `expired_at`，到期后自动拒绝登录和公开接口 | 🟡 中 |
| AI 调用限额 | 通过平台管理设置 `max_ai_daily_calls`，防止 API 滥用 | 🟡 中 |

### 首次部署流程

1. 部署完成后，使用默认账号 `PLATFORM / superadmin / Admin@2024` 登录平台管理后台
2. 立即修改平台管理员密码（用户下拉菜单 → 修改密码）
3. 创建租户时，系统自动生成随机临时密码（一次性显示，需安全转交给租户管理员）
4. 租户管理员首次登录后应立即修改密码
5. 根据需要设置租户到期时间、AI 调用限额、知识库数量上限

### Nginx 反向代理参考

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 环境变量说明

| 变量 | 说明 | 默认值 |
|------|------|--------|
| MYSQL_ROOT_PASSWORD | MySQL root 密码 | root |
| MYSQL_DATABASE | 数据库名 | repair_ai_saas |
| JWT_SECRET | JWT 签名密钥（Base64） | 测试用，生产必须修改 |
| AI_SERVICE_URL | Python Agent 地址 | http://agent-python:8090 |
| LLM_API_KEY | LLM API Key | - |
| LLM_BASE_URL | LLM API 地址 | https://api.deepseek.com |
| LLM_MODEL | LLM 模型名 | deepseek-chat |
| EMBEDDING_API_KEY | Embedding API Key | - |
| EMBEDDING_MODEL | Embedding 模型名 | text-embedding-3-small |

## 数据卷说明

| 卷名 | 挂载路径 | 说明 |
|------|---------|------|
| mysql_data | /var/lib/mysql | MySQL 数据 |
| redis_data | /data | Redis 持久化 |
| qdrant_data | /qdrant/storage | Qdrant 向量数据 |
| upload_data | /app/data/uploads | 上传文档文件 |

## 常见故障

### 后端启动失败：端口被占用

```bash
# 查找占用端口的进程
netstat -ano | findstr :8080
# 终止进程
taskkill /PID <pid> /F
```

### Flyway 迁移失败

检查 `backend-java/src/main/resources/db/migration/` 目录下迁移文件是否完整。

### Qdrant 连接失败

确认 Qdrant 容器已启动：

```bash
docker compose ps qdrant
curl http://localhost:6333/healthz
```

### AI 问答返回兜底回答

Python Agent 未启动或不可用时，Java 后端自动降级为 SQL LIKE 搜索。这是预期行为。
