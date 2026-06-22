/**
 * AI 智能咨询页
 * 调用公开 AI 接口，聊天气泡式交互
 */
const tenant = require('../../../utils/tenant');
const request = require('../../../utils/request');
const config = require('../../../utils/config');

Page({
  data: {
    messages: [],
    inputValue: '',
    sending: false,
    scrollToView: 'chat-bottom',
    themeColor: '#1677ff',
    loading: true,
  },

  onLoad() {
    const code = tenant.requireTenantCode();
    if (!code) return;

    // 读取主题色
    const app = getApp();
    if (app && app.globalData && app.globalData.themeColor) {
      this.setData({ themeColor: app.globalData.themeColor });
    }

    // 初始问候
    this.setData({
      loading: false,
      messages: [{
        id: 'welcome',
        role: 'ai',
        content: '您好！我是 AI 售后助手。请描述您遇到的设备故障，我会尽力帮您解决。',
        knowledgeItems: [],
        shouldCreateTicket: false,
      }],
    });
  },

  onInput(e) {
    this.setData({ inputValue: e.detail.value });
  },

  sendMessage() {
    const value = this.data.inputValue.trim();
    if (!value || this.data.sending) return;

    const messages = [...this.data.messages];

    // 添加用户消息
    messages.push({
      id: `user-${Date.now()}`,
      role: 'user',
      content: value,
    });

    // 添加 loading
    messages.push({
      id: `loading-${Date.now()}`,
      role: 'loading',
    });

    this.setData({
      messages,
      inputValue: '',
      sending: true,
      scrollToView: 'chat-bottom',
    });

    // 调用 AI 接口
    const url = tenant.buildPublicUrl(config.api.aiChat);
    request.post(url, { question: value })
      .then(data => {
        // 移除 loading
        const finalMessages = this.data.messages.filter(m => m.role !== 'loading');

        if (data) {
          finalMessages.push({
            id: `ai-${Date.now()}`,
            role: 'ai',
            content: data.answer || '抱歉，我暂时无法处理您的问题。',
            knowledgeItems: data.matchedItems || [],
            shouldCreateTicket: data.shouldCreateTicket || false,
            repairPrompt: data.repairPrompt || '',
          });
        } else {
          finalMessages.push({
            id: `ai-${Date.now()}`,
            role: 'ai',
            content: '抱歉，系统繁忙，请稍后重试。',
            shouldCreateTicket: true,
            repairPrompt: '建议您直接提交报修，我们尽快安排师傅处理。',
          });
        }

        this.setData({
          messages: finalMessages,
          sending: false,
          scrollToView: 'chat-bottom',
        });
      })
      .catch(() => {
        const finalMessages = this.data.messages.filter(m => m.role !== 'loading');
        finalMessages.push({
          id: `ai-${Date.now()}`,
          role: 'ai',
          content: '网络连接失败，请检查网络后重试。',
          shouldCreateTicket: true,
          repairPrompt: '建议您直接提交报修或拨打客服电话。',
        });
        this.setData({
          messages: finalMessages,
          sending: false,
          scrollToView: 'chat-bottom',
        });
      });
  },

  /** 跳转提交报修 */
  goRepair() {
    wx.navigateTo({ url: '/pages/portal/repair/repair' });
  },
});
