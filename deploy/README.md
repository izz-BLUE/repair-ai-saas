# 部署脚本说明

> ⚠️ 所有脚本仅支持 Linux / macOS。Windows 用户请使用 WSL。

## 脚本列表

| 脚本 | 用途 | 用法 |
|------|------|------|
| `scripts/check-env.sh` | 部署前环境检查 | `bash deploy/scripts/check-env.sh` |
| `scripts/init-demo-data.sh` | 初始化演示数据（API 调用） | `bash deploy/scripts/init-demo-data.sh` |
| `scripts/backup-mysql.sh` | MySQL 数据库备份 | `bash deploy/scripts/backup-mysql.sh [保留份数]` |
| `scripts/backup-uploads.sh` | 上传文件备份 | `bash deploy/scripts/backup-uploads.sh` |
| `scripts/backup-qdrant.sh` | Qdrant 向量数据备份 | `bash deploy/scripts/backup-qdrant.sh` |
| `scripts/restore-mysql.sh` | MySQL 数据库恢复 | `bash deploy/scripts/restore-mysql.sh <备份文件>` |
| `nginx/repair-ai.conf` | Nginx 反向代理配置（参考模板） | 部署时复制到 Nginx 配置目录 |

## 快速开始

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env，修改所有密码和 API Key

# 2. 检查环境
bash deploy/scripts/check-env.sh

# 3. 构建前端
cd frontend && npm install && npm run build && cd ..

# 4. 启动服务（参考模板，需根据实际环境调整）
docker compose -f docker-compose.prod.yml up -d

# 5. 初始化演示数据（可选）
bash deploy/scripts/init-demo-data.sh

# 6. 定时备份（建议加入 crontab）
# 每天凌晨 3 点备份数据库
# 0 3 * * * cd /path/to/repair-ai-saas && bash deploy/scripts/backup-mysql.sh
```

## 安全提醒

- 所有脚本从 `.env` 读取配置，**不硬编码密码**
- 备份脚本不会将密码打印到日志
- `restore-mysql.sh` 有二次确认机制，防止误操作
- `init-demo-data.sh` 仅用于演示验收，上线前应清理演示数据
