/**
 * 师傅工单列表页
 * 展示分配给当前师傅的工单，支持状态筛选
 */
const request = require('../../../utils/request');
const config = require('../../../utils/config');
const auth = require('../../../utils/auth');
const statusUtils = require('../../../utils/status');

Page({
  data: {
    statusTabs: statusUtils.TECHNICIAN_TABS,
    activeStatus: '',
    tickets: [],
    page: 1,
    pageSize: 20,
    loading: true,
  },

  onLoad() {
    // 权限检查：未登录或非 TECHNICIAN 跳转登录
    if (!auth.isLoggedIn() || !auth.hasRole('TECHNICIAN')) {
      wx.redirectTo({ url: '/pages/technician/login/login' });
      return;
    }
    this.loadTickets();
  },

  onShow() {
    // 从其他页面返回时，如果是从详情页回来（可能状态变了），刷新列表
    if (!this.data.loading && this.data.tickets.length > 0) {
      this.refreshTickets();
    }
  },

  /** 加载工单列表 */
  loadTickets() {
    this.setData({ loading: true });

    const params = { page: this.data.page, size: this.data.pageSize };
    if (this.data.activeStatus) {
      params.status = this.data.activeStatus;
    }

    request.get(config.api.technicianTickets, params)
      .then(data => {
        const list = (data && data.records) || [];
        const enriched = list.map(ticket => ({
          ...ticket,
          statusLabel: statusUtils.getStatusLabel(ticket.status),
          statusColor: statusUtils.getStatusColor(ticket.status),
        }));
        this.setData({
          loading: false,
          tickets: enriched,
        });
      })
      .catch(() => {
        this.setData({ loading: false, tickets: [] });
      });
  },

  /** 下拉刷新 */
  refreshTickets() {
    this.setData({ page: 1 });
    this.loadTickets();
  },

  /** 切换状态筛选 */
  onStatusChange(e) {
    const status = e.currentTarget.dataset.status;
    if (status === this.data.activeStatus) return;
    this.setData({ activeStatus: status, page: 1, tickets: [] });
    this.loadTickets();
  },

  /** 跳转工单详情 */
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/technician/ticket-detail/ticket-detail?id=${id}` });
  },

  /** 退出登录 */
  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          auth.clearAuth();
          wx.redirectTo({ url: '/pages/technician/login/login' });
        }
      },
    });
  },
});
