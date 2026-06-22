/**
 * 提交报修页
 * 表单提交到公开报修接口
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

Page({
  data: {
    productTypes: PRODUCT_TYPES,
    productTypeIndex: -1,
    customerName: '',
    customerPhone: '',
    serviceAddress: '',
    faultDescription: '',
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

  onProductTypeChange(e) {
    this.setData({ productTypeIndex: parseInt(e.detail.value) });
    this.clearError('productType');
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

  /** 表单校验 */
  validate() {
    const errors = {};
    if (this.data.productTypeIndex === -1) {
      errors.productType = '请选择产品类型';
    }
    if (!this.data.customerName.trim()) {
      errors.customerName = '请输入联系人姓名';
    }
    const phone = this.data.customerPhone.trim();
    if (!phone) {
      errors.customerPhone = '请输入手机号';
    } else if (!/^1[3-9]\d{9}$/.test(phone)) {
      errors.customerPhone = '请输入正确的手机号';
    }
    if (!this.data.serviceAddress.trim()) {
      errors.serviceAddress = '请输入服务地址';
    }
    if (!this.data.faultDescription.trim()) {
      errors.faultDescription = '请描述设备故障';
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

    this.setData({ submitting: true });

    const productType = PRODUCT_TYPES[this.data.productTypeIndex];
    const url = tenant.buildPublicUrl(config.api.repairRequest);

    request.post(url, {
      tenantCode: code,
      productType: productType.value,
      customerName: this.data.customerName.trim(),
      customerPhone: this.data.customerPhone.trim(),
      serviceAddress: this.data.serviceAddress.trim(),
      faultDescription: this.data.faultDescription.trim(),
    }, true)
      .then(data => {
        this.setData({
          submitting: false,
          submitted: true,
          ticketNo: (data && data.ticketNo) || '获取中...',
        });
      })
      .catch(() => {
        this.setData({ submitting: false });
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
