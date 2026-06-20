# 共享服务器部署方案：repair-ai-saas + eat-what

> 本文档说明 repair-ai-saas 和 eat-what（今天吃啥）共用同一台服务器的部署方案。
> 两个项目均为独立项目，本方案只做部署层面的协调，不修改任何一方的业务代码。

## 1. 项目概况

| 维度 | repair-ai-saas | eat-what |
|------|---------------|----------|
| 业务 | AI 售后维修工单 SaaS | 今天吃啥（微信小程序） |
| 后端 | Spring Boot 3.2 + Java 17 | Spring Boot 3.2 + Java 17 |
| 前端 | React SPA（Nginx 托管） | 微信小程序（客户端运行） |
| 数据库 | MySQL 8.0 | MySQL 8.0 |
| Redis | Redis 7 | 不使用 |
| 向量库 | Qdrant | 不使用 |
| AI 服务 | Python FastAPI Agent | 不使用 |

## 2. 服务器配置建议

| 项目 | 最低配置 | 推荐配置 |
|------|---------|---------|
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 60 GB SSD | 120 GB SSD |
| 操作系统 | Ubuntu 22.04 LTS | Ubuntu 22.04 LTS |
| Docker | 24.0+ | 最新稳定版 |
| Docker Compose | v2 | 最新稳定版 |

> 两个 Spring Boot 后端 + Python Agent + MySQL + Redis + Qdrant + Nginx，推荐 8GB 内存。
> 4GB 内存下需调小 JVM 堆和 Qdrant 内存限制。

## 3. 端口分配

两个项目各自使用独立端口，互不冲突：

| 端口 | 归属 | 服务 | 是否暴露到宿主机 |
|------|------|------|----------------|
| **80** | 共用 | Nginx（统一入口） | 是 |
| **443** | 共用 | Nginx HTTPS | 是 |
| **3306** | 不暴露 | 各自 MySQL 容器内部使用 | 否 |
| **6379** | 不暴露 | Redis 容器内部使用 | 否 |
| **6333** | 不暴露 | Qdrant 容器内部使用 | 否 |
| **8080** | 不暴露 | 各自后端容器内部使用 | 否 |
| **8090** | 不暴露 | repair-ai-saas Python Agent | 否 |

**关键原则：** 生产环境下，只有 Nginx 的 80/443 暴露到宿主机。所有数据库、缓存、后端服务均在 Docker 网络内部通信，不暴露到宿主机端口。

## 4. 目录结构

两个项目部署在同一台服务器上，各自独立目录：

```
/opt/apps/
├── eat-what/                      # eat-what 项目
│   ├── deploy/
│   │   ├── docker-compose.prod.yml
│   │   ├── .env
│   │   └── nginx/
│   │       └── eat-what.conf
│   └── ...
│
├── repair-ai-saas/                # repair-ai-saas 项目
│   ├── docker-compose.prod.yml
│   ├── .env
│   ├── deploy/
│   │   ├── nginx/
│   │   │   └── repair-ai.conf
│   │   └── scripts/
│   ├── backend-java/
│   ├── agent-python/
│   ├── frontend/
│   │   └── dist/
│   └── data/
│       └── uploads/
│
└── nginx/
    └── conf.d/                    # 统一 Nginx 配置目录
        ├── eat-what.conf          # → 软链接或复制自 eat-what 项目
        └── repair-ai.conf         # → 软链接或复制自 repair-ai-saas 项目
```

## 5. MySQL 隔离方案

两个项目使用**同一个 MySQL 实例**，但通过**不同数据库名 + 不同用户**隔离。

> ⚠️ 共享 MySQL 要求两个后端都能访问同一个 MySQL 容器。如果使用完全独立的 Docker 网络（第 7.2 节），repair-ai-saas 后端将无法连接 eat-what 的 MySQL。此时应选择各自独立 MySQL 容器（第 5.2 节）。

### 5.1 方案：共享 MySQL 容器，独立数据库

推荐使用 eat-what 已有的 MySQL 容器（或新建一个独立 MySQL 容器），两个项目各用各的数据库：

| 维度 | eat-what | repair-ai-saas |
|------|----------|---------------|
| 数据库名 | `eat_what` | `repair_ai_saas` |
| 用户名 | `eatwhat` | `repair` |
| 容器内部访问 | `mysql:3306` | `mysql:3306` |
| 宿主机访问 | 不暴露 | 不暴露 |

**初始化 SQL（在 MySQL 容器内执行一次）：**

```sql
-- eat-what 的库和用户（通常由 Flyway 自动创建）
CREATE DATABASE IF NOT EXISTS eat_what CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'eatwhat'@'%' IDENTIFIED BY '<eat-what-db-password>';
GRANT ALL PRIVILEGES ON eat_what.* TO 'eatwhat'@'%';

-- repair-ai-saas 的库和用户
CREATE DATABASE IF NOT EXISTS repair_ai_saas CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'repair'@'%' IDENTIFIED BY '<repair-db-password>';
GRANT ALL PRIVILEGES ON repair_ai_saas.* TO 'repair'@'%';

FLUSH PRIVILEGES;
```

### 5.2 如果选择各自独立 MySQL 容器

如果希望完全隔离（互不影响），可以各自运行独立的 MySQL 容器，只需确保容器名和内部网络不冲突：

| 项目 | 容器名 | Docker 网络 |
|------|--------|------------|
| eat-what | `eat-what-mysql-prod` | `eat-what-net` |
| repair-ai-saas | `repair-mysql` | `repair-net` |

两个 MySQL 容器均不暴露 3306 到宿主机，各自在自己的 Docker 网络内部通信。

## 6. Redis 隔离

eat-what 不使用 Redis，因此不存在冲突。repair-ai-saas 的 Redis 容器独立运行即可。

## 7. Docker 网络

两个项目共用同一台服务器时，Docker 网络有两种方案，取决于 MySQL 隔离策略。

### 7.1 推荐方案：共享 infra-net + 各自内部网络

如果选择共享 MySQL（第 5.1 节），需要一个跨项目的共享网络让两个后端都能访问 MySQL：

```
┌─ infra-net（共享）─────────────────────────────┐
│  mysql（共享实例）  │  宿主机 Nginx（host 模式） │
└────────────────────────────────────────────────┘

┌─ eat-what-net ─┐    ┌─ repair-net ─────────────┐
│  eat-what 后端  │    │  repair 后端  │  Redis    │
│  （也加入       │    │  Agent        │  Qdrant   │
│   infra-net）   │    │ （也加入 infra-net）      │
└────────────────┘    └──────────────────────────┘
```

**eat-what 后端**加入 `eat-what-net` + `infra-net`（双网络）。
**repair-ai-saas 后端**加入 `repair-net` + `infra-net`（双网络）。
MySQL 只加入 `infra-net`，两个后端通过 `infra-net` 内部 DNS 访问 `mysql:3306`。

repair-ai-saas 的 docker-compose.prod.yml 示例：

```yaml
networks:
  repair-net:
    driver: bridge
  infra-net:
    external: true          # 预先创建：docker network create infra-net

services:
  mysql:
    # ...
    networks:
      - infra-net           # 仅加入 infra-net，供两个后端访问

  redis:
    networks:
      - repair-net

  qdrant:
    networks:
      - repair-net

  backend:
    # ...
    networks:
      - repair-net          # 内部服务通信
      - infra-net           # 访问共享 MySQL

  agent-python:
    networks:
      - repair-net
```

eat-what 的 docker-compose.prod.yml 中，backend 也需加入 `infra-net`：

```yaml
networks:
  eat-what-net:
    driver: bridge
  infra-net:
    external: true

services:
  backend:
    # ...
    networks:
      - eat-what-net
      - infra-net           # 访问共享 MySQL
```

> ⚠️ eat-what 的 docker-compose.prod.yml 需要你自行修改，本文档仅提供参考。修改前请备份原文件。

### 7.2 替代方案：完全独立网络 + 各自独立 MySQL

如果不想跨项目共享网络，应选择各自独立 MySQL 容器（第 5.2 节）。此时两个项目完全隔离，互不影响：

| 项目 | 网络名 | 包含服务 |
|------|--------|---------|
| eat-what | `eat-what-net` | mysql、backend、nginx |
| repair-ai-saas | `repair-net` | mysql、redis、qdrant、backend、agent-python、nginx |

两个项目之间无任何网络连通，各自独立部署和重启。

## 8. Nginx 统一入口方案

两个项目共用一个 Nginx 进程作为统一入口，通过域名区分流量。

### 8.1 方案 A：系统 Nginx（推荐）

在宿主机上直接安装 Nginx（`apt install nginx`），不通过 Docker 运行。
系统 Nginx 可以直接通过 `127.0.0.1` 访问宿主机上映射的端口。

**前提条件：** 各后端容器通过 `127.0.0.1:<宿主机端口>:<容器端口>` 映射到宿主机（见 8.3 节）。

```nginx
# /etc/nginx/conf.d/eat-what.conf
server {
    listen 80;
    server_name eat.example.com;

    location /api/ {
        proxy_pass http://127.0.0.1:18080;  # eat-what 后端
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # eat-what 是微信小程序，不需要 Nginx 托管前端静态文件
}

# /etc/nginx/conf.d/repair-ai.conf
server {
    listen 80;
    server_name repair.example.com;

    client_max_body_size 10M;
    gzip on;
    gzip_types text/plain text/css application/json application/javascript;

    # 前端静态文件
    location / {
        root /opt/apps/repair-ai-saas/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:28080;  # repair-ai-saas 后端
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 120s;
    }
}
```

> ⚠️ `proxy_pass http://127.0.0.1:xxx` 只在系统 Nginx（或 `network_mode: host` 的容器 Nginx）下有效。
> 普通 Docker 容器内的 `127.0.0.1` 指的是容器自身，不是宿主机。

### 8.2 方案 B：Docker Nginx 加入共享网络

如果不想在宿主机上安装 Nginx，可以用 Docker 运行 Nginx 容器，但必须加入共享网络才能通过 Docker 内部 DNS 访问后端服务：

```bash
docker run -d --name nginx-proxy \
  -p 80:80 -p 443:443 \
  --network infra-net \
  -v /opt/apps/nginx/conf.d:/etc/nginx/conf.d:ro \
  -v /opt/apps/repair-ai-saas/frontend/dist:/usr/share/nginx/html/repair-ai:ro \
  --restart always \
  nginx:alpine
```

此时 Nginx 配置中应使用 Docker 服务名而非 `127.0.0.1`：

```nginx
# eat-what 后端（假设 eat-what 的 backend 容器也加入了 infra-net）
location /api/ {
    proxy_pass http://eat-what-backend:8080;  # Docker 内部 DNS
}

# repair-ai-saas 后端
location /api/ {
    proxy_pass http://repair-backend:8080;    # Docker 内部 DNS
}
```

> ⚠️ 使用此方案时，两个项目的后端容器必须都加入 `infra-net`，否则 Nginx 无法解析服务名。

### 8.3 各项目端口映射（宿主机侧）

为避免端口冲突，各项目的容器端口映射到宿主机的不同端口，并**绑定 127.0.0.1** 防止外部直接访问：

| 宿主机端口 | 容器端口 | 归属 | 说明 |
|-----------|---------|------|------|
| 127.0.0.1:18080 | 8080 | eat-what 后端 | 仅本机 Nginx 可访问 |
| 127.0.0.1:28080 | 8080 | repair-ai-saas 后端 | 仅本机 Nginx 可访问 |
| 80 | 80 | 宿主机 Nginx | 对外 |
| 443 | 443 | 宿主机 Nginx | 对外（启用 SSL 后） |

> **安全说明：** 绑定 `127.0.0.1` 意味着这些端口只能被宿主机上的进程（如 Nginx）访问，外部无法直接连接。如果写成 `18080:8080`（不带 127.0.0.1），则任何人都能通过 `http://<服务器IP>:18080` 直接访问后端，绕过 Nginx 的安全策略。

**eat-what 的 docker-compose.prod.yml 端口映射修改：**

```yaml
services:
  backend:
    # ...
    ports:
      - "127.0.0.1:18080:8080"   # 仅本机可访问
```

**repair-ai-saas 的 docker-compose.prod.yml 端口映射修改：**

```yaml
services:
  backend:
    # ...
    ports:
      - "127.0.0.1:28080:8080"   # 仅本机可访问

  agent-python:
    # ...
    expose:
      - "8090"                    # 仅 Docker 内部可访问，不映射到宿主机
    # 如需本地调试，可临时添加：
    # ports:
    #   - "127.0.0.1:28090:8090"

  # 如果使用方案 A（系统 Nginx），不需要自带 Nginx 容器
  # nginx:
  #   # ...（注释掉或删除）
```

## 9. 内存分配建议

8GB 服务器的内存分配：

| 服务 | 内存限制 | 说明 |
|------|---------|------|
| MySQL（共享） | 1 GB | innodb-buffer-pool-size=512M |
| eat-what 后端 | 768 MB | JVM -Xmx768m（已有配置） |
| repair-ai-saas 后端 | 768 MB | JVM -Xmx768m |
| repair-ai-saas Agent | 512 MB | Python + Qdrant 查询 |
| Redis | 256 MB | repair-ai-saas 专用 |
| Qdrant | 512 MB | 向量索引 |
| Nginx（宿主机） | 128 MB | 反向代理 |
| 系统预留 | ~2 GB | OS + 缓冲 |
| **合计** | **~6 GB** | 预留 2GB 给 OS |

4GB 服务器需降低配置：
- JVM 堆调为 `-Xmx512m`
- Qdrant 内存限制调低
- MySQL buffer pool 调为 256M

## 10. 备份策略

两个项目共享同一台服务器，备份统一管理：

| 备份项 | 频率 | 保留份数 | 脚本 |
|--------|------|---------|------|
| eat-what MySQL | 每天 3:00 | 7 | 自行编写（参考 repair-ai-saas 的 backup-mysql.sh） |
| repair-ai-saas MySQL | 每天 3:30 | 7 | `deploy/scripts/backup-mysql.sh` |
| repair-ai-saas 上传文件 | 每周日 4:00 | 4 | `deploy/scripts/backup-uploads.sh` |
| repair-ai-saas Qdrant | 每周日 4:30 | 4 | `deploy/scripts/backup-qdrant.sh` |

**Crontab 示例：**

```bash
# eat-what 数据库备份
0 3 * * * cd /opt/apps/eat-what && docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec -T -e MYSQL_PWD=<password> mysql mysqldump -u root --single-transaction eat_what | gzip > /opt/backups/eat-what/db_$(date +\%Y\%m\%d).sql.gz

# repair-ai-saas 数据库备份
30 3 * * * cd /opt/apps/repair-ai-saas && bash deploy/scripts/backup-mysql.sh

# repair-ai-saas 文件和向量备份
0 4 * * 0 cd /opt/apps/repair-ai-saas && bash deploy/scripts/backup-uploads.sh && bash deploy/scripts/backup-qdrant.sh
```

## 11. SSL / HTTPS 方案

两个项目共享同一台服务器，推荐使用 **Let's Encrypt** 统一管理证书。

### 11.1 申请证书的方式

有三种方式，根据你的 Nginx 部署方式选择：

**方式一：`certbot --nginx`（最简单，仅系统 Nginx）**

Certbot 自动修改 Nginx 配置并添加 SSL：

```bash
apt install certbot python3-certbot-nginx
certbot --nginx -d eat.example.com -d repair.example.com
```

Certbot 会自动在 Nginx 配置中添加 SSL 相关指令，无需手动编辑。

**方式二：`certbot --webroot`（需 Nginx 配合）**

如果用 webroot 验证，Nginx 必须配置 ACME challenge 路径，否则证书申请会失败：

```nginx
# 在每个 server block 中添加
location /.well-known/acme-challenge/ {
    root /var/www/certbot;
}
```

同时确保 `/var/www/certbot` 目录存在且 Nginx 有读权限：

```bash
mkdir -p /var/www/certbot
chown www-data:www-data /var/www/certbot
```

然后申请：

```bash
apt install certbot
certbot certonly --webroot -w /var/www/certbot \
  -d eat.example.com \
  -d repair.example.com
```

> ⚠️ 如果 Nginx 没有配置 `/.well-known/acme-challenge/` 路径，webroot 方式会返回 404 导致验证失败。

**方式三：DNS 验证（适用于无法开放 80 端口的场景）**

```bash
certbot certonly --manual --preferred-challenges dns \
  -d eat.example.com -d repair.example.com
```

需要手动在 DNS 服务商处添加 TXT 记录。适合内网或端口受限的环境。

### 11.2 证书路径

申请成功后，证书默认存储在：

```
/etc/letsencrypt/live/eat.example.com/fullchain.pem
/etc/letsencrypt/live/eat.example.com/privkey.pem
/etc/letsencrypt/live/repair.example.com/fullchain.pem
/etc/letsencrypt/live/repair.example.com/privkey.pem
```

### 11.3 Nginx HTTPS 配置

在宿主机 Nginx 配置中为每个域名添加 HTTPS server block：

```nginx
server {
    listen 443 ssl http2;
    server_name eat.example.com;

    ssl_certificate /etc/letsencrypt/live/eat.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/eat.example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    location / {
        proxy_pass http://127.0.0.1:18080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 443 ssl http2;
    server_name repair.example.com;

    ssl_certificate /etc/letsencrypt/live/repair.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/repair.example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    client_max_body_size 10M;

    location / {
        proxy_pass http://127.0.0.1:28080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 11.4 自动续期

Let's Encrypt 证书有效期 90 天，需配置自动续期：

```bash
# 测试续期是否正常
certbot renew --dry-run

# 添加 crontab 自动续期（每天 2:00 检查，续期后重载 Nginx）
0 2 * * * certbot renew --quiet --post-hook "systemctl reload nginx"
```

> ⚠️ 如果使用 Docker Nginx，post-hook 改为 `docker exec nginx-proxy nginx -s reload`。

## 12. 部署顺序

首次在同一台服务器上部署两个项目（以方案 A 系统 Nginx 为例）：

```bash
# 1. 准备目录
mkdir -p /opt/apps /opt/backups

# 2. 克隆两个项目
cd /opt/apps
git clone <eat-what-repo> eat-what
git clone <repair-ai-saas-repo> repair-ai-saas

# 3. 创建共享 Docker 网络
docker network create infra-net

# 4. 配置 eat-what
cd /opt/apps/eat-what
cp deploy/.env.example deploy/.env
# 编辑 deploy/.env，修改所有密码和密钥
# 修改 deploy/docker-compose.prod.yml：
#   - backend 端口改为 "127.0.0.1:18080:8080"
#   - mysql 和 backend 加入 infra-net

# 5. 配置 repair-ai-saas
cd /opt/apps/repair-ai-saas
cp .env.example .env
# 编辑 .env，修改所有密码和密钥

# 6. 构建 repair-ai-saas 前端
cd /opt/apps/repair-ai-saas/frontend
npm install && npm run build

# 7. 安装系统 Nginx
apt install nginx
# 将 Nginx 配置复制到 /etc/nginx/conf.d/
cp /opt/apps/nginx/conf.d/*.conf /etc/nginx/conf.d/
nginx -t && systemctl reload nginx

# 8. 启动 eat-what
cd /opt/apps/eat-what
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d

# 9. 启动 repair-ai-saas
cd /opt/apps/repair-ai-saas
docker compose -f docker-compose.prod.yml up -d

# 10. 验证
curl http://127.0.0.1:18080/api/health             # eat-what 后端直连
curl http://127.0.0.1:28080/api/public/PLATFORM/portal-settings  # repair 后端直连
curl -H "Host: eat.example.com" http://127.0.0.1/api/health      # 通过 Nginx
curl -H "Host: repair.example.com" http://127.0.0.1/api/public/PLATFORM/portal-settings

# 11. 申请 SSL 证书（见第 11 节）
```

## 13. 运维检查清单

| 检查项 | 命令 | 频率 |
|--------|------|------|
| 容器状态 | `docker ps --format "table {{.Names}}\t{{.Status}}"` | 每天 |
| 磁盘使用 | `df -h /opt` | 每周 |
| 内存使用 | `free -h` | 每天 |
| eat-what 健康 | `curl -s http://127.0.0.1:18080/api/health` | 每天 |
| repair-ai 健康 | `curl -s http://127.0.0.1:28080/api/public/PLATFORM/portal-settings` | 每天 |
| 备份文件 | `ls -lh /opt/backups/` | 每天 |
| SSL 证书到期 | `certbot certificates` | 每月 |

## 14. 常见问题

### Q: 两个项目的 MySQL 数据会互相影响吗？

不会。即使共享同一个 MySQL 容器，两个项目使用不同的数据库名（`eat_what` 和 `repair_ai_saas`）和不同的用户，权限完全隔离。

### Q: 一个项目重启会影响另一个吗？

不会。两个项目使用独立的 Docker Compose 文件和独立的 Docker 网络。重启 eat-what 不会影响 repair-ai-saas 的服务。

### Q: 如果 eat-what 的 MySQL 已经在用了，怎么添加 repair-ai-saas 的数据库？

只需要在已有的 MySQL 中创建新数据库和用户即可，不需要重启 eat-what：

```bash
docker exec -it eat-what-mysql-prod mysql -u root -p
```
```sql
CREATE DATABASE repair_ai_saas CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER 'repair'@'%' IDENTIFIED BY '<password>';
GRANT ALL PRIVILEGES ON repair_ai_saas.* TO 'repair'@'%';
FLUSH PRIVILEGES;
```

repair-ai-saas 的 Flyway 会自动创建表结构。

### Q: 服务器资源不够怎么办？

优先级调整：
1. 降低 JVM 堆（`-Xmx512m`）
2. 关闭 repair-ai-saas 的 Python Agent（Java 会自动降级为 SQL LIKE 兜底）
3. 减小 Qdrant 内存限制
4. 升级服务器配置
