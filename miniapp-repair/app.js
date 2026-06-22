/**
 * 售后维修小程序 — 应用入口
 * repair-ai-saas V0.5.0
 */
App({
  globalData: {
    baseUrl: 'http://localhost:8080', // TODO: 生产环境替换为 HTTPS 域名
    tenantCode: null,
    themeColor: '#1677ff',
    tenantInfo: null,
  },

  onLaunch(options) {
    // 解析 tenantCode：优先 query 参数，其次 scene 参数
    const tenantCode = this.parseTenantCode(options);
    if (tenantCode) {
      this.globalData.tenantCode = tenantCode;
      wx.setStorageSync('tenantCode', tenantCode);
    } else {
      // 尝试从 storage 恢复
      const cached = wx.getStorageSync('tenantCode');
      if (cached) {
        this.globalData.tenantCode = cached;
      }
    }
  },

  onShow(options) {
    // 每次进入前台时重新解析（扫码进入场景）
    const tenantCode = this.parseTenantCode(options);
    if (tenantCode && tenantCode !== this.globalData.tenantCode) {
      this.globalData.tenantCode = tenantCode;
      wx.setStorageSync('tenantCode', tenantCode);
      // 清空旧的租户信息，触发重新加载
      this.globalData.tenantInfo = null;
      this.globalData.themeColor = '#1677ff';
    }
  },

  parseTenantCode(options) {
    // 方式一：URL query 参数（开发调试用）
    if (options.query && options.query.tenantCode) {
      return decodeURIComponent(options.query.tenantCode);
    }
    // 方式二：scene 参数（生产推荐）
    if (options.query && options.query.scene) {
      const scene = decodeURIComponent(options.query.scene);
      const match = scene.match(/tenantCode[=%]3D([^&]+)/);
      if (match) {
        return decodeURIComponent(match[1]);
      }
    }
    return null;
  },
});
