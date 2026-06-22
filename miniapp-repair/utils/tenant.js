/**
 * 售后维修小程序 — 租户码管理
 * 负责 tenantCode 的解析、校验、存储
 */
const config = require('./config');

const STORAGE_KEY = 'tenantCode';

/**
 * 获取当前 tenantCode
 * 优先级：全局变量 > storage > 无
 */
function getTenantCode() {
  const app = getApp();
  if (app && app.globalData && app.globalData.tenantCode) {
    return app.globalData.tenantCode;
  }
  return wx.getStorageSync(STORAGE_KEY) || null;
}

/**
 * 保存 tenantCode
 */
function setTenantCode(code) {
  wx.setStorageSync(STORAGE_KEY, code);
  const app = getApp();
  if (app && app.globalData) {
    app.globalData.tenantCode = code;
  }
}

/**
 * 验证 tenantCode 是否合法
 * @returns {{ valid: boolean, message?: string }}
 */
function validateTenantCode(code) {
  if (!code || code.trim() === '') {
    return { valid: false, message: '缺少企业编码' };
  }

  // 检查禁止使用的 tenantCode
  if (config.forbiddenTenantCodes && config.forbiddenTenantCodes.includes(code)) {
    return { valid: false, message: '无效的企业编码' };
  }

  return { valid: true };
}

/**
 * 确保 tenantCode 合法，否则跳转错误页
 * 每个客户侧页面 onLoad 时应调用
 */
function requireTenantCode() {
  const code = getTenantCode();
  const result = validateTenantCode(code);
  if (!result.valid) {
    wx.redirectTo({
      url: `/pages/common/error/error?message=${encodeURIComponent(result.message || '缺少企业编码')}`,
    });
    return null;
  }
  return code;
}

/**
 * 构建公开接口 URL — 替换路径中的 {tenantCode}
 */
function buildPublicUrl(pathTemplate) {
  const code = getTenantCode();
  return pathTemplate.replace('{tenantCode}', code || 'MISSING');
}

module.exports = {
  getTenantCode,
  setTenantCode,
  validateTenantCode,
  requireTenantCode,
  buildPublicUrl,
};
