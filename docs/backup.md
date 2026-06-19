# 备份与恢复

## MySQL 备份

### 手动备份

```bash
docker compose exec mysql mysqldump -uroot -p$MYSQL_ROOT_PASSWORD repair_ai_saas > backup_$(date +%Y%m%d).sql
```

### 定时备份（crontab）

```bash
# 每天凌晨 3 点备份
0 3 * * * cd /path/to/repair-ai-saas && docker compose exec -T mysql mysqldump -uroot -proot repair_ai_saas | gzip > backups/db_$(date +\%Y\%m\%d).sql.gz
```

### 恢复

```bash
docker compose exec -T mysql mysql -uroot -proot repair_ai_saas < backup_20240101.sql
```

## Qdrant 备份

Qdrant 数据存储在 Docker volume 中。

### 备份

```bash
docker compose exec qdrant tar czf /tmp/qdrant_backup.tar.gz /qdrant/storage
docker compose cp qdrant:/tmp/qdrant_backup.tar.gz ./backups/qdrant_backup_$(date +%Y%m%d).tar.gz
```

### 恢复

```bash
docker compose cp ./backups/qdrant_backup.tar.gz qdrant:/tmp/
docker compose exec qdrant tar xzf /tmp/qdrant_backup.tar.gz -C /
docker compose restart qdrant
```

## 上传文件备份

文档上传文件存储在 `data/uploads/` 目录下，按 `tenantId` 分目录。

### 备份

```bash
tar czf uploads_backup_$(date +%Y%m%d).tar.gz data/uploads/
```

### 恢复

```bash
tar xzf uploads_backup_20240101.tar.gz
```

## 完整恢复流程

1. 恢复 MySQL 数据
2. 恢复 Qdrant 数据
3. 恢复上传文件
4. 重启所有服务：

```bash
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d
```
