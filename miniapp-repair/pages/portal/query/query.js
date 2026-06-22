/**
 * 查询进度页
 * 按工单号 + 手机号查询维修状态
 */
const tenant = require('../../../utils/tenant');
const request = require('../../../utils/request');
const config = require('../../../utils/config');
const statusUtils = require('../../../utils/status');

Page({
  data: {
    ticketNo: '',
    phone: '',
    querying: false,
    queryError: '',
    result: null,
    themeColor: '#1677ff',
  },

  onLoad() {
    const code = tenant.requireTenantCode();
    if (!code) return;

    const app = getApp();
    if (app && app.globalData && app.globalData.themeColor) {
      this.setData({ themeColor: app.globalData.themeColor });
    }
  },

  onFieldInput(e) {
    const { field } = e.currentTarget.dataset;
    this.setData({ [field]: e.detail.value });
  },

  doQuery() {
    const ticketNo = this.data.ticketNo.trim();
    const phone = this.data.phone.trim();

    if (!ticketNo) {
      wx.showToast({ title: '请输入工单号', icon: 'none' });
      return;
    }
    if (!phone) {
      wx.showToast({ title: '请输入手机号', icon: 'none' });
      return;
    }
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }

    this.setData({ querying: true, queryError: '' });

    const code = tenant.getTenantCode();
    const url = tenant.buildPublicUrl(config.api.ticketQuery);

    request.get(url, { ticketNo, phone })
      .then(data => {
        if (data) {
          this.setData({
            querying: false,
            result: {
              ...data,
              statusLabel: statusUtils.getStatusLabel(data.status) || '',
              statusColor: statusUtils.getStatusColor(data.status) || 'pending',
            },
          });
        } else {
          this.setData({
            querying: false,
            queryError: '未查询到工单信息',
          });
        }
      })
      .catch(err => {
        this.setData({
          querying: false,
          queryError: err.message === '网络错误，请检查网络后重试'
            ? err.message
            : '工单不存在或手机号不匹配',
        });
      });
  },

  resetQuery() {
    this.setData({
      ticketNo: '',
      phone: '',
      result: null,
      queryError: '',
    });
  },
});
