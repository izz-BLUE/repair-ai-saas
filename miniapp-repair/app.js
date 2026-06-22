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
    // 方式一：URL query 参数（开发调试用），优先级最高
    if (options.query) {
      if (options.query.tenantCode) {
        return decodeURIComponent(options.query.tenantCode);
      }
      // 兼容 tc=xxx 短格式
      if (options.query.tc) {
        return decodeURIComponent(options.query.tc);
      }
    }
    // 方式二：scene 参数（生产推荐，二维码扫码进入）
    if (options.query && options.query.scene) {
      const scene = decodeURIComponent(options.query.scene);
      // 支持 scene=tenantCode%3Dxxx (URL encode = 为 %3D)
      let match = scene.match(/tenantCode[=%]3D([^&]+)/i);
      if (match) {
        return decodeURIComponent(match[1]);
      }
      // 支持 scene=tc%3Dxxx 或 scene=tc=xxx 短格式
      match = scene.match(/[&?]?tc[=%]3D([^&]+)/i);
      if (match) {
        return decodeURIComponent(match[1]);
      }
    }
    return null;
  },
});
