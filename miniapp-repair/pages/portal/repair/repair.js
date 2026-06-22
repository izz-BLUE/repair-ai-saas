/**
 * 提交报修页
 * 表单提交到公开报修接口 POST /api/public/{tenantCode}/repair-requests
 * 后端 DTO：name, phone, address, productType, faultDescription（均为 String）
 */
const tenant = require('../../../utils/tenant');
const request = require('../../../utils/request');
const config = require('../../../utils/config');

// 产品类型选项（与后端 ProductType 枚举保持一致）
const PRODUCT_TYPES = [
  { label: '空调', value: 'AIR_CONDITIONER' },
  { label: '冰箱', value: 'REFRIGERATOR' },
  { label: '洗衣机', value: 'WASHING_MACHINE' },
  { label: '热水器', value: 'WATER_HEATER' },
  { label: '净水器', value: 'WATER_PURIFIER' },
  { label: '油烟机', value: 'RANGE_HOOD' },
  { label: '燃气灶', value: 'GAS_STOVE' },
  { label: '电视', value: 'TELEVISION' },
  { label: '其他', value: 'OTHER' },
];

/** 将后端字段错误信息翻译为中文 */
const ERROR_CN_MAP = {
  'name': '请输入联系人',
  'phone': '请输入手机号',
  'address': '请输入服务地址',
  'faultDescription': '请填写故障描述',
};

function toCnError(message) {
  if (!message) return '请检查表单填写';
  for (const [field, cn] of Object.entries(ERROR_CN_MAP)) {
    if (message.includes(field)) return cn;
  }
  return message;
}

Page({
  data: {
    productTypes: PRODUCT_TYPES,
    // 表单字段，与后端 DTO 完全一致
    form: {
      productType: '',
      name: '',
      phone: '',
      address: '',
      faultDescription: '',
    },
    errors: {},
    submitting: false,
    submitted: false,
    ticketNo: '',
    themeColor: '#1677ff',
  },

  onLoad() {
    const code = tenant.requireTenantCode();
    if (!code) return;

    // 读取主题色
    const app = getApp();
    if (app && app.globalData && app.globalData.themeColor) {
      this.setData({ themeColor: app.globalData.themeColor });
    }
  },

  /** 产品类型标签点击 */
  onProductTypeTap(e) {
    const { value } = e.currentTarget.dataset;
    this.setData({ 'form.productType': value });
    this.clearError('productType');
  },

  /** 输入框 bindinput */
  handleInput(e) {
    const { field } = e.currentTarget.dataset;
    const { value } = e.detail;
    this.setData({ [`form.${field}`]: value });
    if (this.data.errors[field]) {
      this.clearError(field);
    }
  },

  clearError(field) {
    const errors = { ...this.data.errors };
    delete errors[field];
    this.setData({ errors });
  },

  /** 表单校验（前端拦截） */
  validate() {
    const { form } = this.data;
    const errors = {};
    if (!form.productType) {
      errors.productType = '请选择产品类型';
    }
    if (!form.name.trim()) {
      errors.name = '请输入联系人';
    }
    const phone = form.phone.trim();
    if (!phone) {
      errors.phone = '请输入手机号';
    } else if (!/^1[3-9]\d{9}$/.test(phone)) {
      errors.phone = '请输入正确的手机号';
    }
    if (!form.address.trim()) {
      errors.address = '请输入服务地址';
    }
    if (!form.faultDescription.trim()) {
      errors.faultDescription = '请填写设备故障描述';
    }
    this.setData({ errors });
    return Object.keys(errors).length === 0;
  },

  /** 提交报修 */
  submitRepair() {
    if (!this.validate()) return;

    const code = tenant.getTenantCode();
    if (!code) {
      wx.showToast({ title: '企业信息异常', icon: 'none' });
      return;
    }

    const { form } = this.data;

    // 显式构造 payload，字段与后端 DTO（RepairRequest record）完全一致
    const payload = {
      name: form.name.trim(),
      phone: form.phone.trim(),
      address: form.address.trim(),
      productType: form.productType,
      faultDescription: form.faultDescription.trim(),
    };

    this.setData({ submitting: true });

    const url = tenant.buildPublicUrl(config.api.repairRequest);

    request.post(url, payload, true)
      .then(data => {
        this.setData({
          submitting: false,
          submitted: true,
          ticketNo: (data && data.ticketNo) || '获取中...',
        });
      })
      .catch(err => {
        this.setData({ submitting: false });
        // 如果后端返回字段校验错误，翻译为中文
        if (err && err.message) {
          wx.showToast({ title: toCnError(err.message), icon: 'none', duration: 2500 });
        }
      });
  },

  /** 跳转查询进度 */
  goQuery() {
    wx.navigateTo({ url: '/pages/portal/query/query' });
  },

  /** 返回首页 */
  resetForm() {
    wx.redirectTo({ url: '/pages/portal/home/home' });
  },
});
