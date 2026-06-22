/**
 * 师傅登录页
 * 仅允许 TECHNICIAN 角色登录
 */
const request = require('../../../utils/request');
const config = require('../../../utils/config');
const auth = require('../../../utils/auth');

Page({
  data: {
    tenantCode: '',
    username: '',
    password: '',
    loggingIn: false,
  },

  onLoad(options) {
    // 预填 tenantCode（从 storage 读取）
    const cached = wx.getStorageSync('tenantCode');
    if (cached) {
      this.setData({ tenantCode: cached });
    }
    if (options.tenantCode) {
      this.setData({ tenantCode: options.tenantCode });
    }

    // 已登录的师傅直接进入工作台
    if (auth.isLoggedIn() && auth.hasRole('TECHNICIAN')) {
      wx.redirectTo({ url: '/pages/technician/tickets/tickets' });
    }
  },

  onFieldInput(e) {
    const { field } = e.currentTarget.dataset;
    this.setData({ [field]: e.detail.value });
  },

  handleLogin() {
    const tenantCode = this.data.tenantCode.trim();
    const username = this.data.username.trim();
    const password = this.data.password;

    if (!tenantCode) {
      wx.showToast({ title: '请输入企业编码', icon: 'none' });
      return;
    }
    if (!username) {
      wx.showToast({ title: '请输入用户名', icon: 'none' });
      return;
    }
    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' });
      return;
    }

    this.setData({ loggingIn: true });

    request.post(config.api.login, {
      tenantCode,
      username,
      password,
    }, true)
      .then(data => {
        this.setData({ loggingIn: false });

        if (!data) {
          wx.showToast({ title: '登录失败', icon: 'none' });
          return;
        }

        // 仅允许 TECHNICIAN 进入
        if (!auth.checkTechnicianAccess(data)) {
          return;
        }

        // 保存认证信息
        auth.saveAuth(data.token, {
          userId: data.userId,
          username: data.username,
          realName: data.realName,
          role: data.role,
          tenantId: data.tenantId,
          tenantCode: tenantCode,
        });

        // 保存 tenantCode
        wx.setStorageSync('tenantCode', tenantCode);

        // 跳转工作台
        wx.redirectTo({ url: '/pages/technician/tickets/tickets' });
      })
      .catch(err => {
        this.setData({ loggingIn: false });
        // 错误已由 request 层处理
      });
  },

  goHome() {
    const tenantCode = wx.getStorageSync('tenantCode');
    if (tenantCode) {
      wx.redirectTo({ url: `/pages/portal/home/home?tenantCode=${tenantCode}` });
    } else {
      wx.redirectTo({ url: '/pages/portal/home/home' });
    }
  },
});
