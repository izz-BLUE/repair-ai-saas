#!/usr/bin/env bash
# ============================================
# MySQL 数据库备份脚本
# 用法: bash deploy/scripts/backup-mysql.sh [保留份数，默认 7]
# ============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"

if [[ ! -f "$ENV_FILE" ]]; then
    echo "[ERROR] .env 文件不存在: $ENV_FILE"
    exit 1
fi
# shellcheck source=/dev/null
source "$ENV_FILE"

KEEP_COUNT="${1:-7}"
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/mysql_${TIMESTAMP}.sql.gz"

echo "[INFO] 开始 MySQL 备份"
echo "[INFO] 数据库: ${MYSQL_DATABASE:-repair_ai_saas}"

# 自动创建备份目录
mkdir -p "$BACKUP_DIR"

# 执行备份（密码通过环境变量传入，不进命令行参数）
docker compose -f "$PROJECT_ROOT/docker-compose.prod.yml" exec -T \
    -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" \
    mysql mysqldump \
    -u root \
    --single-transaction \
    --routines \
    --triggers \
    "${MYSQL_DATABASE:-repair_ai_saas}" \
    | gzip > "$BACKUP_FILE"

FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "[INFO] 备份完成: $BACKUP_FILE ($FILE_SIZE)"

# 清理旧备份，保留最近 N 份
BACKUP_COUNT=$(ls -1 "$BACKUP_DIR"/mysql_*.sql.gz 2>/dev/null | wc -l)
if [[ "$BACKUP_COUNT" -gt "$KEEP_COUNT" ]]; then
    DELETE_COUNT=$((BACKUP_COUNT - KEEP_COUNT))
    ls -1t "$BACKUP_DIR"/mysql_*.sql.gz | tail -n "$DELETE_COUNT" | xargs rm -f
    echo "[INFO] 已清理 $DELETE_COUNT 份旧备份，保留最近 $KEEP_COUNT 份"
fi

echo "[INFO] 当前备份目录:"
ls -lh "$BACKUP_DIR"/mysql_*.sql.gz 2>/dev/null || echo "  (无备份文件)"
