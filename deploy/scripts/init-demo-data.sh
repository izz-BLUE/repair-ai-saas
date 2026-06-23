#!/usr/bin/env bash
# ============================================
# 演示数据初始化脚本（纯 API 调用，不直接操作数据库）
# 用法: bash deploy/scripts/init-demo-data.sh
#
# ⚠️ 本脚本仅用于演示/验收环境，上线前应清理演示数据。
# 所有操作通过后端 API 完成，保证密码加密、默认字段、向量同步等逻辑正确执行。
# ============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

BASE_URL="${BASE_URL:-http://localhost:8080}"
MAX_WAIT=60

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()  { echo -e "${CYAN}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
err()   { echo -e "${RED}[ERROR]${NC} $1"; }

echo "============================================"
echo "  演示数据初始化"
echo "  后端地址: $BASE_URL"
echo "============================================"
echo ""

# ---------- 0. 依赖检查 ----------
if ! command -v curl &>/dev/null; then
    echo -e "${RED}[ERROR]${NC} curl 未安装，请先安装 curl"
    exit 1
fi
if ! command -v jq &>/dev/null; then
    echo -e "${RED}[ERROR]${NC} jq 未安装，请先安装 jq"
    exit 1
fi

# ---------- 1. 等待后端就绪 ----------
info "等待后端服务就绪..."
WAITED=0
while [[ $WAITED -lt $MAX_WAIT ]]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/public/PLATFORM/portal-settings" 2>/dev/null || echo "000")
    if [[ "$HTTP_CODE" == "200" ]]; then
        ok "后端服务已就绪"
        break
    fi
    sleep 2
    WAITED=$((WAITED + 2))
    echo -ne "  等待中... ${WAITED}s / ${MAX_WAIT}s\r"
done

if [[ $WAITED -ge $MAX_WAIT ]]; then
    err "后端服务未在 ${MAX_WAIT}s 内就绪，退出"
    exit 1
fi

# ---------- 2. SUPER_ADMIN 登录 ----------
info "平台管理员登录..."
SUPER_ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/public/login" \
    -H "Content-Type: application/json" \
    -d '{"tenantCode":"PLATFORM","username":"superadmin","password":"Admin@2024"}' \
    | jq -r '.data.token')

if [[ -z "$SUPER_ADMIN_TOKEN" ]]; then
    err "平台管理员登录失败，请检查默认密码是否已修改"
    exit 1
fi
ok "平台管理员登录成功"

# ---------- 3. 创建演示租户 ----------
info "创建演示租户: 顺德XX维修服务..."
CREATE_RESULT=$(curl -s -X POST "$BASE_URL/api/platform/tenants" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $SUPER_ADMIN_TOKEN" \
    -d '{"name":"顺德XX维修服务","contactName":"张经理","contactPhone":"13800001234"}')

TENANT_CODE=$(echo "$CREATE_RESULT" | jq -r '.data.tenant.tenantCode')
TENANT_ID=$(echo "$CREATE_RESULT" | jq -r '.data.tenant.id')
ADMIN_USERNAME=$(echo "$CREATE_RESULT" | jq -r '.data.adminUsername')
ADMIN_PASSWORD=$(echo "$CREATE_RESULT" | jq -r '.data.adminPassword')

if [[ -z "$TENANT_CODE" ]]; then
    err "创建租户失败: $CREATE_RESULT"
    exit 1
fi
ok "租户创建成功: $TENANT_CODE (ID: $TENANT_ID)"

# ---------- 4. 设置租户限额 ----------
info "设置租户限额..."
curl -s -X PUT "$BASE_URL/api/platform/tenants/$TENANT_ID" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $SUPER_ADMIN_TOKEN" \
    -d '{"maxKnowledgeBases":5,"maxDocuments":50,"maxAiDailyCalls":100}' \
    > /dev/null
ok "限额设置完成: 知识库=5, 文档=50, AI日调用=100"

# ---------- 5. ADMIN 登录 ----------
info "租户管理员登录..."
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/public/login" \
    -H "Content-Type: application/json" \
    -d "{\"tenantCode\":\"$TENANT_CODE\",\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}" \
    | jq -r '.data.token')

if [[ -z "$ADMIN_TOKEN" ]]; then
    err "租户管理员登录失败"
    exit 1
fi
ok "租户管理员登录成功"

# ---------- 6. 创建知识库 ----------
info "创建知识库: 空调维修FAQ..."
KB_RESULT=$(curl -s -X POST "$BASE_URL/api/admin/knowledge-bases" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d '{"name":"空调维修FAQ","description":"常见空调故障排查与维修指南"}')

KB_ID=$(echo "$KB_RESULT" | jq -r '.data.id')
ok "知识库创建成功 (ID: $KB_ID)"

# ---------- 7. 创建示例知识条目 ----------
info "创建示例知识条目..."

declare -a TITLES=("空调不制冷" "冰箱不制冷" "洗衣机不排水")
declare -a QUESTIONS=("空调开了很久但不制冷怎么办" "冰箱冷藏室不制冷是什么原因" "洗衣机排水管不出水怎么处理")
declare -a ANSWERS=(
    "空调不制冷常见原因：1. 滤网堵塞，建议清洗滤网；2. 制冷剂不足，需专业师傅加氟；3. 温度设置过高，建议设置 24-26°C；4. 室外机散热不良，清理室外机周围杂物。如以上方法无效，建议提交报修申请。"
    "冰箱不制冷常见原因：1. 温控器设置不当，建议调至 3-4 档；2. 门封条老化漏冷，检查门封是否严密；3. 冷凝器积灰，清理冰箱背部灰尘；4. 压缩机故障，需专业维修。如仍无法解决，建议提交报修。"
    "洗衣机不排水常见原因：1. 排水管扭曲或堵塞，检查并疏通管道；2. 排水泵滤网堵塞，清理滤网（通常在底部面板内）；3. 排水泵故障，需更换；4. 水位传感器异常。如以上方法无效，建议提交报修。"
)
declare -a PRODUCTS=("空调" "冰箱" "洗衣机")
declare -a FAULTS=("不制冷" "不制冷" "不排水")

for i in 0 1 2; do
    curl -s -X POST "$BASE_URL/api/admin/knowledge-items" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -d "{
            \"knowledgeBaseId\":$KB_ID,
            \"title\":\"${TITLES[$i]}\",
            \"question\":\"${QUESTIONS[$i]}\",
            \"answer\":\"${ANSWERS[$i]}\",
            \"productType\":\"${PRODUCTS[$i]}\",
            \"faultType\":\"${FAULTS[$i]}\",
            \"status\":\"ACTIVE\"
        }" > /dev/null
    ok "  条目 $((i+1)): ${TITLES[$i]}"
done

# ---------- 7.5. 创建师傅测试账号 ----------
info "创建师傅账号: 李师傅..."
CREATE_TECH_RESULT=$(curl -s -X POST "$BASE_URL/api/admin/users" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d '{"realName":"李师傅","phone":"13900005678","username":"technician1","password":"Tech@2024","role":"TECHNICIAN"}')

TECH_ID=$(echo "$CREATE_TECH_RESULT" | jq -r '.data.id')
if [[ -z "$TECH_ID" ]]; then
    err "创建师傅账号失败: $CREATE_TECH_RESULT"
    exit 1
fi
ok "师傅账号创建成功 (ID: $TECH_ID, 用户名: technician1, 密码: Tech@2024)"

# ---------- 8. 输出汇总 ----------
echo ""
echo "============================================"
echo -e "  ${GREEN}演示数据初始化完成${NC}"
echo "============================================"
echo ""
echo "  租户名称: 顺德XX维修服务"
echo "  租户编码: $TENANT_CODE"
echo "  管理后台: $BASE_URL/admin/login"
echo "  管理账号: $ADMIN_USERNAME / $ADMIN_PASSWORD"
echo "  师傅账号: technician1 / Tech@2024 (角色: TECHNICIAN)"
echo ""
echo "  门户地址: $BASE_URL/portal/$TENANT_CODE"
echo "  AI 客服:  $BASE_URL/portal/$TENANT_CODE/chat"
echo "  报修入口: $BASE_URL/portal/$TENANT_CODE/repair"
echo ""
echo "  平台管理: $BASE_URL/platform/tenants"
echo "  平台账号: superadmin / Admin@2024"
echo ""
echo -e "  ${YELLOW}⚠️ 以上为演示数据，上线前请清理或修改密码。${NC}"
echo ""
