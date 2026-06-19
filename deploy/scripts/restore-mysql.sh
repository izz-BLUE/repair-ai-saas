#!/usr/bin/env bash
# ============================================
# MySQL 数据库恢复脚本
# 用法: bash deploy/scripts/restore-mysql.sh <备份文件路径>
# ⚠️ 警告：恢复将覆盖当前数据库所有数据！
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

# 参数检查
BACKUP_FILE="${1:-}"
if [[ -z "$BACKUP_FILE" ]]; then
    echo "[ERROR] 用法: bash deploy/scripts/restore-mysql.sh <备份文件路径>"
    echo ""
    echo "可用的备份文件:"
    BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups}"
    ls -lh "$BACKUP_DIR"/mysql_*.sql.gz 2>/dev/null || echo "  (无备份文件)"
    exit 1
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
    echo "[ERROR] 备份文件不存在: $BACKUP_FILE"
    exit 1
fi

# 显示文件信息
FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo ""
echo "============================================"
echo "  ⚠️  MySQL 数据库恢复"
echo "============================================"
echo ""
echo "  备份文件: $BACKUP_FILE"
echo "  文件大小: $FILE_SIZE"
echo "  目标数据库: ${MYSQL_DATABASE:-repair_ai_saas}"
echo ""
echo "  ⚠️  警告：此操作将覆盖当前数据库中的所有数据！"
echo "  ⚠️  建议先执行一次备份: bash deploy/scripts/backup-mysql.sh"
echo ""

# 二次确认
read -r -p "  请输入 yes 确认恢复（其他输入将取消）: " CONFIRM
if [[ "$CONFIRM" != "yes" ]]; then
    echo "[INFO] 操作已取消"
    exit 0
fi

echo ""
echo "[INFO] 开始恢复数据库..."

gunzip -c "$BACKUP_FILE" | docker compose -f "$PROJECT_ROOT/docker-compose.prod.yml" exec -T \
    -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" \
    mysql mysql -u root "${MYSQL_DATABASE:-repair_ai_saas}"

echo "[INFO] 数据库恢复完成"
echo "[INFO] 建议重启后端服务:"
echo "  docker compose -f docker-compose.prod.yml restart backend"
