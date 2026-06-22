/**
 * 售后维修小程序 — 统一请求封装
 * 基于 wx.request 封装，自动处理 token、401、业务错误
 */
const config = require('./config');
const auth = require('./auth');

/**
 * 发起 HTTP 请求
 * @param {Object} options - { url, method, data, params, showLoading }
 * @returns {Promise<any>} 直接返回 ApiResponse.data
 */
function request(options) {
  const {
    url,
    method = 'GET',
    data = {},
    params = {},
    showLoading = false,
    hideLoading = true,
  } = options;

  if (showLoading) {
    wx.showLoading({ title: '加载中...', mask: true });
  }

  return new Promise((resolve, reject) => {
    const token = auth.getToken();

    // 拼接 query 参数
    let fullUrl = config.baseUrl + url;
    const queryKeys = Object.keys(params);
    if (queryKeys.length > 0) {
      const queryStr = queryKeys
        .filter(k => params[k] !== undefined && params[k] !== null)
        .map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`)
        .join('&');
      if (queryStr) {
        fullUrl += '?' + queryStr;
      }
    }

    // 替换路径参数 {xxx}
    if (data && typeof data === 'object') {
      Object.keys(data).forEach(key => {
        fullUrl = fullUrl.replace(`{${key}}`, encodeURIComponent(data[key]));
      });
    }

    wx.request({
      url: fullUrl,
      method,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      },
      data: method === 'GET' ? undefined : JSON.parse(JSON.stringify(data)),
      success(res) {
        if (hideLoading) {
          wx.hideLoading();
        }

        // 401 未授权 — 清 token 并跳转师傅登录
        if (res.statusCode === 401) {
          auth.clearAuth();
          const pages = getCurrentPages();
          const currentPath = pages.length > 0 ? pages[pages.length - 1].route : '';
          // 只在师傅端页面才跳转登录
          if (currentPath.includes('technician') && !currentPath.includes('login')) {
            wx.redirectTo({ url: '/pages/technician/login/login' });
          }
          wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
          reject(new Error('UNAUTHORIZED'));
          return;
        }

        // 5xx 服务端错误
        if (res.statusCode >= 500) {
          wx.showToast({ title: '服务器繁忙，请稍后重试', icon: 'none' });
          reject(new Error('SERVER_ERROR'));
          return;
        }

        const body = res.data;

        // 业务成功
        if (body && body.code === 'SUCCESS') {
          resolve(body.data);
          return;
        }

        // 业务错误
        const errorMsg = (body && body.message) || '请求失败';
        wx.showToast({ title: errorMsg, icon: 'none', duration: 2500 });
        reject(new Error(errorMsg));
      },
      fail(err) {
        if (hideLoading) {
          wx.hideLoading();
        }
        wx.showToast({ title: '网络错误，请检查网络后重试', icon: 'none' });
        reject(err);
      },
    });
  });
}

/**
 * GET 请求
 */
function get(url, params) {
  return request({ url, method: 'GET', params });
}

/**
 * POST 请求
 */
function post(url, data, showLoading) {
  return request({ url, method: 'POST', data, showLoading });
}

/**
 * PUT 请求
 */
function put(url, data, showLoading) {
  return request({ url, method: 'PUT', data, showLoading });
}

module.exports = { request, get, post, put };
