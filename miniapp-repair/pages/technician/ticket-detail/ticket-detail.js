/**
 * 师傅工单详情页
 * 展示客户信息、工单信息、时间线，支持操作
 */
const request = require('../../../utils/request');
const config = require('../../../utils/config');
const auth = require('../../../utils/auth');
const statusUtils = require('../../../utils/status');

Page({
  data: {
    ticketId: null,
    ticket: {},
    statusLogs: [],
    loading: true,
    error: null,
    acting: false,
  },

  onLoad(options) {
    if (!auth.isLoggedIn() || !auth.hasRole('TECHNICIAN')) {
      wx.redirectTo({ url: '/pages/technician/login/login' });
      return;
    }

    if (!options.id) {
      this.setData({ loading: false, error: '工单 ID 缺失' });
      return;
    }

    this.setData({ ticketId: parseInt(options.id) });
    this.loadTicket();
  },

  loadTicket() {
    if (!this.data.ticketId) return;
    this.setData({ loading: true, error: null });

    const url = config.api.technicianTicketDetail.replace('{id}', this.data.ticketId);
    request.get(url)
      .then(data => {
        if (data && data.ticket) {
          const ticket = {
            ...data.ticket,
            statusLabel: statusUtils.getStatusLabel(data.ticket.status),
            statusColor: statusUtils.getStatusColor(data.ticket.status),
            priorityLabel: statusUtils.getPriorityLabel(data.ticket.priority),
            productTypeLabel: statusUtils.getProductTypeLabel(data.ticket.productType),
          };
          const logs = (data.statusLogs || []).map(log => ({
            ...log,
            toStatusLabel: statusUtils.getStatusLabel(log.toStatus),
          }));
          this.setData({
            loading: false,
            ticket,
            statusLogs: logs,
          });
        } else {
          this.setData({ loading: false, error: '工单不存在' });
        }
      })
      .catch(() => {
        this.setData({ loading: false, error: '加载失败，请检查网络后重试' });
      });
  },

  /** 拨打客户电话 */
  callCustomer() {
    const phone = this.data.ticket.customerPhone;
    if (phone) {
      wx.makePhoneCall({ phoneNumber: phone.replace(/\*/g, '') });
    }
  },

  /** 复制地址 */
  copyAddress() {
    const address = this.data.ticket.serviceAddress;
    if (address) {
      wx.setClipboardData({
        data: address,
        success: () => {
          wx.showToast({ title: '地址已复制', icon: 'success' });
        },
      });
    }
  },

  /** 开始处理（ASSIGNED → IN_PROGRESS） */
  startProcess() {
    wx.showModal({
      title: '确认操作',
      content: '确定要开始处理此工单吗？',
      success: (res) => {
        if (res.confirm) {
          this.doStartProcess();
        }
      },
    });
  },

  doStartProcess() {
    this.setData({ acting: true });
    const url = config.api.technicianStart.replace('{id}', this.data.ticketId);
    request.put(url, {}, true)
      .then(() => {
        wx.showToast({ title: '已开始处理', icon: 'success' });
        this.setData({ acting: false });
        setTimeout(() => {
          this.loadTicket();
        }, 800);
      })
      .catch(() => {
        this.setData({ acting: false });
      });
  },

  /** 跳转完成工单页面 */
  goComplete() {
    wx.navigateTo({
      url: `/pages/technician/complete/complete?id=${this.data.ticketId}`,
    });
  },
});
