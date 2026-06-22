/**
 * 客户门户首页
 * 展示企业品牌信息和 3 个服务入口
 */
const tenant = require('../../../utils/tenant');
const request = require('../../../utils/request');
const config = require('../../../utils/config');

Page({
  data: {
    loading: true,
    error: null,
    tenantName: '',
    tenantDescription: '',
    contactPhone: '',
    logoUrl: '',
    themeColor: '#1677ff',
    themeColorLight: '#e6f0ff',
  },

  onLoad(options) {
    // 解析并保存 tenantCode
    if (options.tenantCode) {
      tenant.setTenantCode(options.tenantCode);
    }
    const code = tenant.requireTenantCode();
    if (!code) return;

    this.loadPortalSettings();
  },

  onShow() {
    // 每次显示时刷新（可能从其他页面返回或切换租户）
    if (!this.data.loading && !this.data.error) {
      this.loadPortalSettings();
    }
  },

  loadPortalSettings() {
    const code = tenant.getTenantCode();
    if (!code) return;

    this.setData({ loading: true, error: null });

    const url = tenant.buildPublicUrl(config.api.portalSettings);
    request.get(url)
      .then(data => {
        if (data) {
          this.setData({
            loading: false,
            tenantName: data.tenantName || '',
            tenantDescription: data.portalDescription || data.tenantName || '',
            contactPhone: data.contactPhone || '',
            logoUrl: data.logoUrl || '',
            themeColor: data.themeColor || '#1677ff',
            themeColorLight: this.adjustColor(data.themeColor || '#1677ff', 0.15),
          });
          // 保存主题色到全局
          const app = getApp();
          if (app && app.globalData) {
            app.globalData.themeColor = data.themeColor || '#1677ff';
            app.globalData.tenantInfo = data;
          }
        } else {
          this.setData({ loading: false });
        }
      })
      .catch(err => {
        this.setData({
          loading: false,
          error: err.message === 'UNAUTHORIZED'
            ? '企业服务暂时不可用'
            : '加载失败，请检查网络后重试',
        });
      });
  },

  /** 生成主题色的浅色版本 */
  adjustColor(hex, factor) {
    // 简化版：返回带透明度的颜色
    if (hex && hex.startsWith('#')) {
      const r = parseInt(hex.slice(1, 3), 16);
      const g = parseInt(hex.slice(3, 5), 16);
      const b = parseInt(hex.slice(5, 7), 16);
      const lighten = (c) => Math.min(255, Math.round(c + (255 - c) * factor));
      return `rgba(${lighten(r)}, ${lighten(g)}, ${lighten(b)}, 0.85)`;
    }
    return '#e6f0ff';
  },

  /** 跳转 AI 咨询 */
  goChat() {
    wx.navigateTo({ url: '/pages/portal/chat/chat' });
  },

  /** 跳转提交报修 */
  goRepair() {
    wx.navigateTo({ url: '/pages/portal/repair/repair' });
  },

  /** 跳转查询进度 */
  goQuery() {
    wx.navigateTo({ url: '/pages/portal/query/query' });
  },

  /** 拨打联系电话 */
  callPhone() {
    if (this.data.contactPhone) {
      wx.makePhoneCall({ phoneNumber: this.data.contactPhone });
    }
  },

  /** 跳转师傅登录 */
  goTechnicianLogin() {
    wx.navigateTo({ url: '/pages/technician/login/login' });
  },
});
