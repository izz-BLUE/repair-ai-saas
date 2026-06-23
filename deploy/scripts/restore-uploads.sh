#!/usr/bin/env bash
# ============================================
# 上传文件恢复脚本
# 用法: bash deploy/scripts/restore-uploads.sh <备份文件路径>
# ⚠️ 警告：恢复将覆盖当前上传目录中的所有文件！
# ============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"

if [[ -f "$ENV_FILE" ]]; then
    # shellcheck source=/dev/null
    source "$ENV_FILE"
fi

# 参数检查
BACKUP_FILE="${1:-}"
if [[ -z "$BACKUP_FILE" ]]; then
    echo "[ERROR] 用法: bash deploy/scripts/restore-uploads.sh <备份文件路径>"
    echo ""
    echo "可用的备份文件:"
    BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups}"
    ls -lh "$BACKUP_DIR"/uploads_*.tar.gz 2>/dev/null || echo "  (无备份文件)"
    exit 1
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
    echo "[ERROR] 备份文件不存在: $BACKUP_FILE"
    exit 1
fi

# 显示文件信息
FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
UPLOADS_DIR="${UPLOADS_DIR:-$PROJECT_ROOT/data/uploads}"

echo ""
echo "============================================"
echo "  ⚠️  上传文件恢复"
echo "============================================"
echo ""
echo "  备份文件: $BACKUP_FILE"
echo "  文件大小: $FILE_SIZE"
echo "  目标目录: $UPLOADS_DIR"
echo ""
echo "  ⚠️  警告：此操作将覆盖当前上传目录中的所有文件！"
echo "  ⚠️  建议先执行一次备份: bash deploy/scripts/backup-uploads.sh"
echo ""

# 二次确认
read -r -p "  请输入 yes 确认恢复（其他输入将取消）: " CONFIRM
if [[ "$CONFIRM" != "yes" ]]; then
    echo "[INFO] 操作已取消"
    exit 0
fi

echo ""
echo "[INFO] 开始恢复上传文件..."

# 创建目标目录
mkdir -p "$UPLOADS_DIR"

# 恢复
tar -xzf "$BACKUP_FILE" -C "$(dirname "$UPLOADS_DIR")"

echo "[INFO] 上传文件恢复完成"
echo "[INFO] 恢复后的文件:"
ls -lh "$UPLOADS_DIR" 2>/dev/null || echo "  (上传目录为空)"
