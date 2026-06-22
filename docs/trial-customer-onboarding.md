# 试点客户首次部署交付流程

> 本文档适用于第一个试点客户的部署和交付。部署完成后请对照验收清单逐项确认。

## 1. 服务器要求

| 项目 | 最低配置 | 推荐配置 |
|------|---------|---------|
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 40 GB SSD | 100 GB SSD |
| 操作系统 | Ubuntu 22.04 / CentOS 8 | Ubuntu 22.04 LTS |
| Docker | 24.0+ | 最新稳定版 |
| Docker Compose | v2 | 最新稳定版 |

## 2. 部署步骤

### 2.1 准备环境

```bash
# 安装 Docker（Ubuntu）
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 验证
docker --version
docker compose version
```

### 2.2 获取代码

```bash
git clone <仓库地址> repair-ai-saas
cd repair-ai-saas
```

### 2.3 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env`，**必须修改**：

| 变量 | 说明 |
|------|------|
| `MYSQL_ROOT_PASSWORD` | 数据库密码，不要用 `root` |
| `JWT_SECRET` | JWT 密钥，用 `openssl rand -base64 32` 生成 |
| `LLM_API_KEY` | DeepSeek API Key |
| `EMBEDDING_API_KEY` | Embedding API Key（可与 LLM 共用） |
| `DOMAIN_NAME` | 实际域名或 IP |

### 2.4 检查环境

```bash
bash deploy/scripts/check-env.sh
```

确认全部 PASS 后继续。

### 2.5 构建前端

```bash
cd frontend
npm install
npm run build
cd ..
```

### 2.6 启动服务

```bash
docker compose -f docker-compose.prod.yml up -d
```

验证服务状态：

```bash
docker compose -f docker-compose.prod.yml ps
curl http://localhost:8080/api/public/PLATFORM/portal-settings
```

### 2.7 初始化演示数据（可选）

```bash
bash deploy/scripts/init-demo-data.sh
```

## 3. 平台管理员初始化

1. 访问 `http://<服务器IP>/admin/login`
2. 使用默认账号登录：
   - 企业编码：`PLATFORM`
   - 用户名：`superadmin`
   - 密码：`Admin@2024`
3. **立即修改密码**（下拉菜单 → 修改密码）

## 4. 创建客户租户

1. 在平台管理后台点击"创建租户"
2. 填写企业名称、联系人、电话
3. 系统自动生成随机临时密码（一次性显示，务必安全转交）
4. 设置租户限额：
   - 知识库数量上限
   - 文档数量上限
   - AI 日调用上限
   - 到期时间（如适用）

## 5. 配置企业门户

以租户管理员身份登录管理后台：

1. 进入"企业设置"
2. 设置门户标题（如"XX维修售后服务"）
3. 设置门户描述
4. 设置联系电话
5. 选择主题色
6. 确认门户已启用

## 6. 上传知识文档

1. 创建知识库（如"空调维修FAQ"）
2. 上传 FAQ 文档（.txt / .md 格式，最大 10MB）
3. 等待自动解析完成
4. 手动补充或修正知识条目
5. 测试 AI 问答效果

## 7. 测试报修流程

1. 访问门户地址：`http://<服务器IP>/portal/<租户编码>`
2. 测试 AI 客服问答
3. 提交报修工单
4. 在管理后台查看工单
5. 测试工单状态流转（派单 → 处理 → 完成 → 关闭）

### 7.2 测试师傅端流程

1. 在管理后台创建一位 TECHNICIAN 角色的师傅账号
2. 将师傅工号/密码分发给师傅
3. 师傅使用手机浏览器打开 `http://<服务器IP>/admin/login`
4. 输入企业编码、用户名、密码登录
5. 登录后自动跳转 `/technician/tickets` 工作台
6. 在列表页查看分配给自己的工单
7. 使用状态筛选（全部/已派单/处理中/已完成/已关闭）
8. 点击已派单（ASSIGNED）工单进入详情
9. 点击"开始处理"，确认状态变为"处理中"
10. 点击"完成工单"，填写维修结果并提交
11. 确认状态变为"已完成"，操作按钮消失
12. 返回列表，验证筛选功能正常

## 8. 交付客户账号

整理以下信息交付客户：

| 项目 | 内容 |
|------|------|
| 管理后台地址 | `http://<域名>/admin/login` |
| 企业编码 | `<租户编码>` |
| 管理员账号 | `admin / <随机密码>` |
| 门户地址 | `http://<域名>/portal/<租户编码>` |
| AI 客服地址 | `http://<域名>/portal/<租户编码>/chat` |
| 报修地址 | `http://<域名>/portal/<租户编码>/repair` |
| 师傅工作台 | 使用同一登录页面，师傅账号登录自动进入工作台 |

> 提醒客户首次登录后立即修改密码。

## 9. 客户验收清单

- [ ] 管理后台可正常登录
- [ ] 门户页面正常显示企业名称和描述
- [ ] AI 客服能回答知识库中的问题
- [ ] 门户可提交报修工单
- [ ] 管理后台能查看和处理工单
- [ ] 派单后师傅能在手机端查看工单
- [ ] 师傅可开始处理工单（ASSIGNED → IN_PROGRESS）
- [ ] 师傅可完成工单并填写维修结果（IN_PROGRESS → COMPLETED）
- [ ] 客户可通过工单号+手机号查询工单进度
- [ ] 工单状态流转正常（报修 → 派单 → 师傅处理 → 完成 → 关闭）
- [ ] 知识库上传文档正常解析
- [ ] 师傅工作台只能看到自己的工单（权限隔离）
- [ ] 联系电话和主题色显示正确
- [ ] 手机端访问正常（响应式布局）

### 小程序试点验收（V0.5.0+）

- [ ] 小程序可扫码进入企业首页
- [ ] 首页显示企业名称、描述、联系电话
- [ ] 主题色从 portal-settings 正确读取
- [ ] AI 客服可对话，气泡样式正确
- [ ] 报修表单可提交，成功后显示工单号
- [ ] 工单号 + 手机号可查询进度
- [ ] 师傅可登录（仅 TECHNICIAN 可进入）
- [ ] 师傅可查看分配给自己的工单列表
- [ ] 师傅可开始处理工单
- [ ] 师傅可完成工单并填写维修结果
- [ ] 底部操作栏适配 iPhone X+ 安全区
- [ ] 401 正确处理（清 token 跳转登录）
- [ ] 无真实 AppID / 密钥 / 域名提交

## 10. 后续运维

### 定时备份

建议配置 crontab：

```bash
# 每天凌晨 3 点备份数据库
0 3 * * * cd /path/to/repair-ai-saas && bash deploy/scripts/backup-mysql.sh

# 每周日凌晨 4 点备份上传文件和向量数据
0 4 * * 0 cd /path/to/repair-ai-saas && bash deploy/scripts/backup-uploads.sh && bash deploy/scripts/backup-qdrant.sh
```

### 监控建议

- 检查 Docker 容器状态：`docker compose -f docker-compose.prod.yml ps`
- 查看后端日志：`docker compose -f docker-compose.prod.yml logs backend --tail 100`
- 查看 Agent 日志：`docker compose -f docker-compose.prod.yml logs agent-python --tail 100`

### 问题联系

部署和运维问题请联系技术支持。
