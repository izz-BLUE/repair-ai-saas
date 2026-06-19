#!/usr/bin/env bash
# ============================================
# Qdrant 向量数据备份脚本
# 用法: bash deploy/scripts/backup-qdrant.sh
# ============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"

if [[ -f "$ENV_FILE" ]]; then
    # shellcheck source=/dev/null
    source "$ENV_FILE"
fi

BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/qdrant_${TIMESTAMP}.tar.gz"

echo "[INFO] 开始备份 Qdrant 向量数据"

# 自动创建备份目录
mkdir -p "$BACKUP_DIR"

docker compose -f "$PROJECT_ROOT/docker-compose.prod.yml" exec -T \
    qdrant tar -czf - -C / qdrant/storage > "$BACKUP_FILE"

FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "[INFO] 备份完成: $BACKUP_FILE ($FILE_SIZE)"
