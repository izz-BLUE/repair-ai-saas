# 售后维修小程序化方案

> 创建日期：2026-06-22
> 版本：V0.4.3（方案阶段，不实现代码）
> 状态：📋 方案设计中

## 1. 小程序定位

### 核心决策

| 问题 | 决策 |
|------|------|
| 是否塞进 eat-what 小程序？ | **否。** eat-what（今天吃啥）是独立产品，不应耦合维修系统 |
| 是否需要独立小程序？ | **是。** 新建"售后维修"独立小程序，复用 repair-ai-saas 后端 API |
| Web 管理后台是否保留？ | **保留。** 管理员和平台管理员继续使用 Web 后台 |
| H5 客户端是否保留？ | **保留。** 作为备选入口和降级方案 |
| 一个小程序服务多个租户？ | **是。** 通过二维码参数（scene/tenantCode）区分企业 |

### 产品矩阵

```
┌──────────────────────────────────────────────┐
│            repair-ai-saas 后端 API           │
├────────────┬────────────┬────────────────────┤
│  Web 管理后台 │  H5 客户端   │   🆕 独立小程序     │
│  (Admin)    │  (Portal)  │   (Miniapp)       │
├────────────┼────────────┼────────────────────┤
│ 管理员后台   │ 客户门户     │  客户 + 师傅       │
│ 平台管理     │ 报修/查询   │  移动端主入口       │
│ 桌面为主     │ 移动备用    │  扫码进入          │
└────────────┴────────────┴────────────────────┘
```

## 2. 角色入口

| 角色 | 小程序入口 | 说明 |
|------|-----------|------|
| **客户** | 扫码进入 → 首页 | 无需登录，使用手机号验证身分 |
| **维修师傅** | 首页 → 师傅登录 → 工作台 | 使用 tenantCode + 账号密码登录 |
| **企业管理员** | **不进入小程序** | 继续使用 Web 管理后台 |
| **平台管理员** | **不进入小程序** | 继续使用 Web 平台管理 |

### 客户流程图

```
客户扫码 → 企业首页
  ├── AI 智能咨询（输入问题，AI 回答）
  ├── 提交报修（姓名/电话/地址/产品/故障描述）
  └── 查询进度（工单号 + 手机号）
```

### 师傅流程图

```
师傅扫码 → 首页 → 点击"师傅入口" → 登录
  → 我的工单列表（状态筛选）
  → 工单详情
    ├── 开始处理（ASSIGNED → IN_PROGRESS）
    └── 完成工单（填写维修结果 → COMPLETED）
```

## 3. 页面结构

### 3.1 客户侧页面

| 页面路径 | 功能 | 复用 API | 优先级 |
|---------|------|----------|--------|
| `pages/portal/home` | 企业首页：服务入口卡片（AI 咨询/报修/查询） | `GET /api/public/{tc}/portal-settings` | P0 |
| `pages/portal/chat` | AI 智能客服会话页 | `POST /api/public/{tc}/ai/chat` | P0 |
| `pages/portal/repair` | 提交报修表单 | `POST /api/public/{tc}/repair-requests` | P0 |
| `pages/portal/query` | 查询进度（输入工单号+手机号） | `GET /api/public/{tc}/tickets/query` | P0 |
| `pages/portal/ticket-detail` | 工单详情展示（从查询结果跳转） | 从 query 结果展示 | P1 |

### 3.2 师傅侧页面

| 页面路径 | 功能 | 复用 API | 优先级 |
|---------|------|----------|--------|
| `pages/technician/login` | 师傅登录页（企业编码+用户名+密码） | `POST /api/public/login` | P0 |
| `pages/technician/tickets` | 我的工单列表（卡片式+状态筛选） | `GET /api/technician/tickets` | P0 |
| `pages/technician/ticket-detail` | 工单详情（客户信息+时间线+操作按钮） | `GET /api/technician/tickets/{id}` | P0 |
| `pages/technician/complete` | 完成工单表单 | `PUT /api/technician/tickets/{id}/complete` | P0 |

### 3.3 通用页面

| 页面路径 | 功能 | 优先级 |
|---------|------|--------|
| `pages/common/error` | 错误页面（企业不存在/服务不可用/网络错误） | P0 |
| `pages/common/loading` | 全局 Loading 组件 | P0 |

## 4. tenantCode 传递方案

### 4.1 扫码进入

客户通过扫描企业专属二维码进入小程序：

```
# 方式一：URL 参数（开发调试用）
/pages/portal/home?tenantCode=shunde001

# 方式二：微信 scene 参数（生产推荐）
scene=tenantCode%3Dshunde001
```

### 4.2 存储与使用

```
扫码进入 → 解析 tenantCode
  → 存入 wx.storage.setSync('tenantCode', xxx)
  → 所有公开接口 URL 中注入 tenantCode
  → 首页展示时调用 portal-settings 获取企业品牌信息
```

### 4.3 安全约束

- `tenantCode` 仅用于公开客户接口
- 不允许使用 `PLATFORM` 作为门户 tenantCode
- 师傅登录后 token 中自带 tenantCode，不需要显式传递
- 切换企业需要重新扫码

## 5. 登录态方案

### 5.1 客户侧（第一版）

**不强制登录。**

- 报修：手机号作为身份标识（与现有逻辑一致）
- 查询：工单号 + 手机号双重匹配（与现有逻辑一致）
- 后续版本可接入微信手机号授权增强体验

### 5.2 师傅侧

**复用现有 JWT 登录。**

```
登录流程：
1. 输入 tenantCode + username + password
2. 调用 POST /api/public/login
3. 获取 token + role + realName
4. 存储：
   wx.setStorageSync('token', token)
   wx.setStorageSync('user', JSON.stringify({role, realName, ...}))
5. 后续请求注入 Authorization: Bearer <token>
6. token 过期 → 清除 storage → 回到登录页
```

### 5.3 请求拦截器伪代码

```javascript
// 小程序请求封装
const request = (options) => {
  const token = wx.getStorageSync('token')
  
  return new Promise((resolve, reject) => {
    wx.request({
      ...options,
      url: BASE_URL + options.url,
      header: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
        ...options.header,
      },
      success(res) {
        if (res.statusCode === 401) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('user')
          wx.redirectTo({ url: '/pages/technician/login' })
          return
        }
        const data = res.data
        if (data.code === 'SUCCESS') {
          resolve(data.data)
        } else {
          wx.showToast({ title: data.message || '请求失败', icon: 'none' })
          reject(data)
        }
      },
      fail(err) {
        wx.showToast({ title: '网络错误，请重试', icon: 'none' })
        reject(err)
      },
    })
  })
}
```

## 6. 生产上线注意事项

| 事项 | 说明 |
|------|------|
| HTTPS 域名 | 后端必须使用 HTTPS（申请 SSL 证书，配置 Nginx） |
| request 合法域名 | 微信小程序后台 → 开发 → 开发管理 → 开发设置 → 服务器域名 |
| uploadFile 合法域名 | 如需图片上传，需要单独配置 |
| downloadFile 合法域名 | 如需下载文件，需要单独配置 |
| 备案要求 | 域名必须完成 ICP 备案，否则小程序无法正式上线 |
| 审核类目 | 应选择"生活服务 > 家政服务"或"工具 > 企业管理"相关类目 |
| 类目隔离 | **不要与 eat-what 共用类目**，避免审核混淆 |
| AppID 管理 | 不提交真实 AppID、密钥、域名到代码仓库 |
| 体验版 | 先发布体验版给试点客户内测，确认稳定后再正式发布 |
| 版本管理 | 客户侧和师傅侧同步发版，设置最低基础库版本 |

## 7. 与现有 H5 的关系

| 入口 | 定位 | 保留 |
|------|------|------|
| H5 客户门户 | 备用入口（微信外浏览器/PC浏览器） | ✅ |
| H5 师傅端 | 备用入口（非微信环境/测试调试） | ✅ |
| 小程序客户端 | 主要移动端入口（微信内） | 🆕 |
| 小程序师傅端 | 主要移动端入口（微信内） | 🆕 |
| Web 管理后台 | 管理员唯一入口 | ✅ 不变 |
| Web 平台管理 | 平台管理员唯一入口 | ✅ 不变 |

> 小程序与 H5 共享同一套后端 API，无需额外开发后端接口。小程序上线后，H5 入口仍保留供非微信场景使用。
