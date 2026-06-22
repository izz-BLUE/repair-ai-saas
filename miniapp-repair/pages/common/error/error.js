Page({
  data: {
    message: '页面加载失败',
  },

  onLoad(options) {
    if (options.message) {
      this.setData({ message: decodeURIComponent(options.message) });
    }
  },

  goHome() {
    const tenantCode = wx.getStorageSync('tenantCode');
    if (tenantCode) {
      wx.redirectTo({ url: `/pages/portal/home/home?tenantCode=${tenantCode}` });
    } else {
      wx.showToast({ title: '无法获取企业信息', icon: 'none' });
    }
  },
});
