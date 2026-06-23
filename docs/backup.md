# 备份与恢复

> 所有备份/恢复脚本位于 `deploy/scripts/`，支持 MySQL、上传文件、Qdrant 三种数据类型。
> 定时备份 crontab 模板见 `deploy/scripts/crontab.example`。

## MySQL 备份

### 手动备份

```bash
bash deploy/scripts/backup-mysql.sh
# 默认保留 7 份，输出到 backups/mysql_YYYYMMDD_HHMMSS.sql.gz
```

### 定时备份（crontab）

```bash
# 每天凌晨 3 点备份，保留 7 份
0 3 * * * cd /path/to/repair-ai-saas && bash deploy/scripts/backup-mysql.sh 7
```

### 恢复

```bash
bash deploy/scripts/restore-mysql.sh backups/mysql_20240101_030000.sql.gz
```

> 恢复前有二次确认提示，需输入 `yes` 确认。不会自动备份当前数据，建议恢复前先手动备份。

## 上传文件备份

### 手动备份

```bash
bash deploy/scripts/backup-uploads.sh
# 输出到 backups/uploads_YYYYMMDD_HHMMSS.tar.gz
```

### 定时备份

```bash
# 每周日凌晨 4 点备份
0 4 * * 0 cd /path/to/repair-ai-saas && bash deploy/scripts/backup-uploads.sh
```

### 恢复

```bash
bash deploy/scripts/restore-uploads.sh backups/uploads_20240101_040000.tar.gz
```

> 恢复前有二次确认，需输入 `yes`。恢复直接覆盖 `data/uploads/` 目录。

## Qdrant 备份

### 手动备份

```bash
bash deploy/scripts/backup-qdrant.sh
# 输出到 backups/qdrant_YYYYMMDD_HHMMSS.tar.gz
```

### 定时备份

```bash
# 每周日凌晨 4:30 备份
30 4 * * 0 cd /path/to/repair-ai-saas && bash deploy/scripts/backup-qdrant.sh
```

### 恢复

```bash
bash deploy/scripts/restore-qdrant.sh backups/qdrant_20240101_043000.tar.gz
```

> 恢复会先停止 Qdrant 容器，恢复完成后重启。恢复前有二次确认。恢复后需验证：
> ```bash
> curl http://localhost:6333/healthz
> ```

## 完整恢复流程

1. 恢复 MySQL 数据：
   ```bash
   bash deploy/scripts/restore-mysql.sh backups/mysql_YYYYMMDD.sql.gz
   ```

2. 恢复上传文件：
   ```bash
   bash deploy/scripts/restore-uploads.sh backups/uploads_YYYYMMDD.tar.gz
   ```

3. 恢复 Qdrant 数据：
   ```bash
   bash deploy/scripts/restore-qdrant.sh backups/qdrant_YYYYMMDD.tar.gz
   ```

4. 重启后端服务：
   ```bash
   docker compose -f docker-compose.prod.yml restart backend
   ```

## 定时备份配置

备份 crontab 模板：`deploy/scripts/crontab.example`

```bash
# 查看模板
cat deploy/scripts/crontab.example

# 编辑 crontab
crontab -e
# 复制模板内容，修改项目路径后保存

# 验证
crontab -l
```

**建议备份保留策略：**
| 数据类型 | 频率 | 保留 |
|---------|------|------|
| MySQL | 每天 3:00 | 7 天日备 |
| MySQL | 每周日 | 4 周周备（手动留存） |
| 上传文件 | 每周日 4:00 | 4 周 |
| Qdrant | 每周日 4:30 | 4 周 |

**每月至少执行一次恢复演练**，确保备份文件可用于恢复。

## 备份验证

```bash
# 检查最新备份文件
ls -lh backups/ | tail -5

# 验证 MySQL 备份（检查文件头）
gunzip -c backups/mysql_*.sql.gz | head -20

# 验证上传文件备份
tar -tzf backups/uploads_*.tar.gz | head -5

# 验证 Qdrant 备份
tar -tzf backups/qdrant_*.tar.gz | head -5
```
