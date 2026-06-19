#!/usr/bin/env bash
# ============================================
# 部署前环境检查脚本
# 用法: bash deploy/scripts/check-env.sh
# ============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS_COUNT=0
WARN_COUNT=0
FAIL_COUNT=0

pass()  { echo -e "  ${GREEN}[PASS]${NC} $1"; ((PASS_COUNT++)); }
warn()  { echo -e "  ${YELLOW}[WARN]${NC} $1"; ((WARN_COUNT++)); }
fail()  { echo -e "  ${RED}[FAIL]${NC} $1"; ((FAIL_COUNT++)); }

echo "============================================"
echo "  repair-ai-saas 部署前环境检查"
echo "============================================"
echo ""

# ---------- 1. .env 文件 ----------
echo "▸ 检查 .env 文件"
if [[ -f "$ENV_FILE" ]]; then
    pass ".env 文件存在"
    # shellcheck source=/dev/null
    source "$ENV_FILE"
else
    fail ".env 文件不存在，请先执行: cp .env.example .env"
    echo ""
    echo "============================================"
    echo "  结果: 1 FAIL（.env 缺失，后续检查跳过）"
    echo "============================================"
    exit 1
fi

# ---------- 2. JWT_SECRET ----------
echo ""
echo "▸ 检查安全配置"
DEFAULT_JWT="dGVzdC1zZWNyZXQta2V5LWZvci1yZXBhaXItYWktc2Fhcy12MDEtZGV2ZWxvcG1lbnQ="
if [[ "${JWT_SECRET:-}" == "$DEFAULT_JWT" ]]; then
    fail "JWT_SECRET 使用的是默认值，生产环境必须替换"
elif [[ -z "${JWT_SECRET:-}" ]]; then
    fail "JWT_SECRET 未配置"
else
    pass "JWT_SECRET 已自定义"
fi

# ---------- 3. MYSQL_ROOT_PASSWORD ----------
if [[ "${MYSQL_ROOT_PASSWORD:-}" == "root" ]]; then
    warn "MYSQL_ROOT_PASSWORD 使用默认值 'root'，建议生产环境替换"
else
    pass "MYSQL_ROOT_PASSWORD 已自定义"
fi

# ---------- 4. LLM_API_KEY ----------
if [[ -z "${LLM_API_KEY:-}" || "${LLM_API_KEY:-}" == "sk-xxx" ]]; then
    warn "LLM_API_KEY 未配置或使用占位符，AI 服务将降级为 SQL LIKE 兜底"
else
    pass "LLM_API_KEY 已配置"
fi

# ---------- 5. Docker ----------
echo ""
echo "▸ 检查 Docker 环境"
if command -v docker &>/dev/null; then
    pass "docker 已安装: $(docker --version | head -1)"
else
    fail "docker 未安装"
fi

if docker compose version &>/dev/null; then
    pass "docker compose 已安装: $(docker compose version --short)"
elif command -v docker-compose &>/dev/null; then
    warn "使用旧版 docker-compose，建议升级到 docker compose v2"
else
    fail "docker compose 未安装"
fi

# ---------- 6. 端口占用检查 ----------
echo ""
echo "▸ 检查端口占用"
check_port() {
    local port=$1
    local name=$2
    if command -v netstat &>/dev/null; then
        if netstat -tlnp 2>/dev/null | grep -q ":${port} "; then
            warn "端口 $port ($name) 已被占用"
        else
            pass "端口 $port ($name) 可用"
        fi
    elif command -v ss &>/dev/null; then
        if ss -tlnp 2>/dev/null | grep -q ":${port} "; then
            warn "端口 $port ($name) 已被占用"
        else
            pass "端口 $port ($name) 可用"
        fi
    elif command -v lsof &>/dev/null; then
        if lsof -i ":${port}" &>/dev/null; then
            warn "端口 $port ($name) 已被占用"
        else
            pass "端口 $port ($name) 可用"
        fi
    else
        warn "无可用端口检查工具（netstat/ss/lsof），跳过端口 $port"
    fi
}

check_port "${FRONTEND_PORT:-80}"   "Nginx/前端"
check_port "${BACKEND_PORT:-8080}"  "Java 后端"
check_port "${AGENT_PORT:-8090}"    "Python Agent"

# ---------- 7. 目录检查 ----------
echo ""
echo "▸ 检查数据目录"
UPLOADS_DIR="${UPLOADS_DIR:-$PROJECT_ROOT/data/uploads}"
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups}"

if [[ -d "$UPLOADS_DIR" ]]; then
    if [[ -w "$UPLOADS_DIR" ]]; then
        pass "上传目录存在且可写: $UPLOADS_DIR"
    else
        warn "上传目录存在但不可写: $UPLOADS_DIR"
    fi
else
    warn "上传目录不存在，将在运行时创建: $UPLOADS_DIR"
fi

if [[ -d "$BACKUP_DIR" ]]; then
    if [[ -w "$BACKUP_DIR" ]]; then
        pass "备份目录存在且可写: $BACKUP_DIR"
    else
        warn "备份目录存在但不可写: $BACKUP_DIR"
    fi
else
    warn "备份目录不存在，备份脚本将自动创建: $BACKUP_DIR"
fi

# ---------- 8. 前端构建产物 ----------
echo ""
echo "▸ 检查前端构建"
if [[ -d "$PROJECT_ROOT/frontend/dist" ]]; then
    pass "前端 dist/ 目录存在"
else
    warn "前端 dist/ 目录不存在，请先执行: cd frontend && npm run build"
fi

# ---------- 9. docker compose config ----------
echo ""
echo "▸ 检查 docker compose 配置"
if [[ -f "$PROJECT_ROOT/docker-compose.prod.yml" ]]; then
    # 去掉 markdown 代码块标记（兼容旧格式）
    CLEAN_YAML=$(sed '/^```/d' "$PROJECT_ROOT/docker-compose.prod.yml")
    if echo "$CLEAN_YAML" | docker compose -f - config &>/dev/null; then
        pass "docker-compose.prod.yml 配置有效"
    else
        warn "docker-compose.prod.yml 配置检查失败（可能缺少环境变量）"
    fi
else
    fail "docker-compose.prod.yml 不存在"
fi

# ---------- 汇总 ----------
echo ""
echo "============================================"
echo "  检查完成: ${GREEN}${PASS_COUNT} PASS${NC}  ${YELLOW}${WARN_COUNT} WARN${NC}  ${RED}${FAIL_COUNT} FAIL${NC}"
echo "============================================"

if [[ $FAIL_COUNT -gt 0 ]]; then
    echo -e "${RED}存在 FAIL 项，请修复后再部署。${NC}"
    exit 1
elif [[ $WARN_COUNT -gt 0 ]]; then
    echo -e "${YELLOW}存在 WARN 项，建议检查后继续。${NC}"
    exit 0
else
    echo -e "${GREEN}全部通过，可以开始部署。${NC}"
    exit 0
fi
