/**
 * 售后维修小程序 — 认证管理
 * 管理 token 和用户信息的存储/读取/清除
 */

const STORAGE_KEY_TOKEN = 'token';
const STORAGE_KEY_USER = 'user';

/**
 * 保存认证信息
 */
function saveAuth(token, user) {
  wx.setStorageSync(STORAGE_KEY_TOKEN, token);
  wx.setStorageSync(STORAGE_KEY_USER, JSON.stringify(user));
}

/**
 * 获取 token
 */
function getToken() {
  return wx.getStorageSync(STORAGE_KEY_TOKEN) || null;
}

/**
 * 获取用户信息
 */
function getUser() {
  const raw = wx.getStorageSync(STORAGE_KEY_USER);
  if (raw) {
    try {
      return JSON.parse(raw);
    } catch (e) {
      return null;
    }
  }
  return null;
}

/**
 * 检查是否已登录
 */
function isLoggedIn() {
  return !!getToken();
}

/**
 * 检查是否为指定角色
 */
function hasRole(role) {
  const user = getUser();
  return user && user.role === role;
}

/**
 * 清除认证信息
 */
function clearAuth() {
  wx.removeStorageSync(STORAGE_KEY_TOKEN);
  wx.removeStorageSync(STORAGE_KEY_USER);
}

/**
 * 仅允许 TECHNICIAN 登录进入师傅端
 * 返回 true 表示允许，false 表示拒绝
 */
function checkTechnicianAccess(user) {
  if (!user || user.role !== 'TECHNICIAN') {
    wx.showToast({ title: '仅维修师傅可登录师傅端', icon: 'none' });
    return false;
  }
  return true;
}

module.exports = {
  saveAuth,
  getToken,
  getUser,
  isLoggedIn,
  hasRole,
  clearAuth,
  checkTechnicianAccess,
};
