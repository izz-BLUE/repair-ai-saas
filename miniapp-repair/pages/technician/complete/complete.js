/**
 * 完成工单页
 * 填写维修结果并完成工单（IN_PROGRESS → COMPLETED）
 */
const request = require('../../../utils/request');
const config = require('../../../utils/config');
const auth = require('../../../utils/auth');

Page({
  data: {
    ticketId: null,
    ticketNo: '',
    repairResult: '',
    partsNote: '',
    costNote: '',
    remark: '',
    errors: {},
    submitting: false,
  },

  onLoad(options) {
    if (!auth.isLoggedIn() || !auth.hasRole('TECHNICIAN')) {
      wx.redirectTo({ url: '/pages/technician/login/login' });
      return;
    }

    if (!options.id) {
      wx.showToast({ title: '工单 ID 缺失', icon: 'none' });
      wx.navigateBack();
      return;
    }

    this.setData({
      ticketId: parseInt(options.id),
      ticketNo: options.ticketNo || '',
    });

    // 如果已经知道工单号，不需要额外请求
    if (!options.ticketNo) {
      this.loadTicketInfo();
    }
  },

  loadTicketInfo() {
    const url = config.api.technicianTicketDetail.replace('{id}', this.data.ticketId);
    request.get(url)
      .then(data => {
        if (data && data.ticket) {
          this.setData({ ticketNo: data.ticket.ticketNo || '' });
        }
      })
      .catch(() => {});
  },

  onFieldInput(e) {
    const { field } = e.currentTarget.dataset;
    this.setData({ [field]: e.detail.value });
    this.clearError(field);
  },

  clearError(field) {
    const errors = { ...this.data.errors };
    delete errors[field];
    this.setData({ errors });
  },

  validate() {
    const errors = {};
    if (!this.data.repairResult.trim()) {
      errors.repairResult = '请填写维修结果';
    }
    this.setData({ errors });
    return Object.keys(errors).length === 0;
  },

  submitComplete() {
    if (!this.validate()) return;

    wx.showModal({
      title: '确认完成',
      content: '确认提交维修结果吗？提交后工单将标记为已完成。',
      success: (res) => {
        if (res.confirm) {
          this.doSubmit();
        }
      },
    });
  },

  doSubmit() {
    this.setData({ submitting: true });
    const url = config.api.technicianComplete.replace('{id}', this.data.ticketId);

    request.put(url, {
      repairResult: this.data.repairResult.trim(),
      partsNote: this.data.partsNote.trim(),
      costNote: this.data.costNote.trim(),
      remark: this.data.remark.trim() || '维修完成',
    }, true)
      .then(() => {
        wx.showToast({ title: '工单已完成', icon: 'success' });
        // 返回工单详情
        setTimeout(() => {
          wx.redirectTo({
            url: `/pages/technician/ticket-detail/ticket-detail?id=${this.data.ticketId}`,
          });
        }, 1000);
      })
      .catch(() => {
        this.setData({ submitting: false });
      });
  },
});
