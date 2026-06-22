/**
 * 售后维修小程序 — 配置
 *
 * ⚠️ 安全警告：不提交真实域名、密钥到代码仓库
 * 生产环境请替换 baseUrl 为 HTTPS 域名
 */
const config = {
  // TODO: 生产环境替换为 HTTPS 域名
  baseUrl: 'http://localhost:8080',

  // API 路径
  api: {
    // 公开接口（客户侧）
    portalSettings: '/api/public/{tenantCode}/portal-settings',
    aiChat: '/api/public/{tenantCode}/ai/chat',
    repairRequest: '/api/public/{tenantCode}/repair-requests',
    ticketQuery: '/api/public/{tenantCode}/tickets/query',

    // 认证
    login: '/api/public/login',

    // 师傅端
    technicianTickets: '/api/technician/tickets',
    technicianTicketDetail: '/api/technician/tickets/{id}',
    technicianStart: '/api/technician/tickets/{id}/start',
    technicianComplete: '/api/technician/tickets/{id}/complete',
  },

  // 禁止使用的 tenantCode
  forbiddenTenantCodes: ['PLATFORM'],
};

module.exports = config;
