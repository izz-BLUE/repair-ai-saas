#!/usr/bin/env bash
# ============================================
# Qdrant 向量数据恢复脚本
# 用法: bash deploy/scripts/restore-qdrant.sh <备份文件路径>
# ⚠️ 警告：恢复将覆盖当前 Qdrant 向量数据！
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
    echo "[ERROR] 用法: bash deploy/scripts/restore-qdrant.sh <备份文件路径>"
    echo ""
    echo "可用的备份文件:"
    BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups}"
    ls -lh "$BACKUP_DIR"/qdrant_*.tar.gz 2>/dev/null || echo "  (无备份文件)"
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
echo "  ⚠️  Qdrant 向量数据恢复"
echo "============================================"
echo ""
echo "  备份文件: $BACKUP_FILE"
echo "  文件大小: $FILE_SIZE"
echo "  目标容器: qdrant"
echo ""
echo "  ⚠️  警告：此操作将覆盖当前 Qdrant 向量数据！"
echo "  ⚠️  恢复后需重启 Qdrant 容器才能生效"
echo "  ⚠️  建议先执行一次备份: bash deploy/scripts/backup-qdrant.sh"
echo ""

# 二次确认
read -r -p "  请输入 yes 确认恢复（其他输入将取消）: " CONFIRM
if [[ "$CONFIRM" != "yes" ]]; then
    echo "[INFO] 操作已取消"
    exit 0
fi

echo ""
echo "[INFO] 停止 Qdrant 容器..."
docker compose -f "$PROJECT_ROOT/docker-compose.prod.yml" stop qdrant

echo "[INFO] 恢复 Qdrant 数据..."
cat "$BACKUP_FILE" | docker compose -f "$PROJECT_ROOT/docker-compose.prod.yml" exec -T \
    qdrant tar -xzf - -C / 2>/dev/null || {
    # 如果容器未运行，使用 docker cp + run 方式
    echo "[INFO] 使用容器内 tar 恢复..."
    docker compose -f "$PROJECT_ROOT/docker-compose.prod.yml" up -d qdrant
    sleep 2
    cat "$BACKUP_FILE" | docker compose -f "$PROJECT_ROOT/docker-compose.prod.yml" exec -T \
        qdrant tar -xzf - -C /
}

echo "[INFO] 启动 Qdrant 容器..."
docker compose -f "$PROJECT_ROOT/docker-compose.prod.yml" start qdrant

echo "[INFO] Qdrant 数据恢复完成"
echo "[INFO] 验证恢复结果:"
echo "  curl http://localhost:6333/healthz"
