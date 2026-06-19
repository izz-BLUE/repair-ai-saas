#!/usr/bin/env bash
# ============================================
# 上传文件备份脚本
# 用法: bash deploy/scripts/backup-uploads.sh
# ============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"

if [[ -f "$ENV_FILE" ]]; then
    # shellcheck source=/dev/null
    source "$ENV_FILE"
fi

UPLOADS_DIR="${UPLOADS_DIR:-$PROJECT_ROOT/data/uploads}"
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/uploads_${TIMESTAMP}.tar.gz"

echo "[INFO] 开始备份上传文件"
echo "[INFO] 源目录: $UPLOADS_DIR"

if [[ ! -d "$UPLOADS_DIR" ]]; then
    echo "[WARN] 上传目录不存在: $UPLOADS_DIR"
    echo "[WARN] 跳过备份"
    exit 0
fi

# 自动创建备份目录
mkdir -p "$BACKUP_DIR"

tar -czf "$BACKUP_FILE" -C "$(dirname "$UPLOADS_DIR")" "$(basename "$UPLOADS_DIR")"

FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "[INFO] 备份完成: $BACKUP_FILE ($FILE_SIZE)"
