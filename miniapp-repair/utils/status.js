/**
 * 售后维修小程序 — 工单状态常量和映射
 * 与后端 TicketStatus 枚举保持一致
 */

// 状态枚举值
const STATUS = {
  PENDING: 'PENDING',
  ASSIGNED: 'ASSIGNED',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  FOLLOWED_UP: 'FOLLOWED_UP',
  CLOSED: 'CLOSED',
  CANCELLED: 'CANCELLED',
};

// 状态中文标签
const STATUS_LABELS = {
  PENDING: '待处理',
  ASSIGNED: '已派单',
  IN_PROGRESS: '处理中',
  COMPLETED: '已完成',
  FOLLOWED_UP: '已回访',
  CLOSED: '已关闭',
  CANCELLED: '已取消',
};

// 状态颜色（CSS 类后缀）
const STATUS_COLORS = {
  PENDING: 'pending',
  ASSIGNED: 'assigned',
  IN_PROGRESS: 'in-progress',
  COMPLETED: 'completed',
  FOLLOWED_UP: 'followed-up',
  CLOSED: 'closed',
  CANCELLED: 'cancelled',
};

// 师傅端状态筛选 Tab
const TECHNICIAN_TABS = [
  { key: '', label: '全部' },
  { key: 'ASSIGNED', label: '已派单' },
  { key: 'IN_PROGRESS', label: '处理中' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CLOSED', label: '已关闭' },
];

// 优先级枚举
const PRIORITY = {
  LOW: 'LOW',
  NORMAL: 'NORMAL',
  HIGH: 'HIGH',
  URGENT: 'URGENT',
};

// 优先级中文标签
const PRIORITY_LABELS = {
  LOW: '低',
  NORMAL: '普通',
  HIGH: '高',
  URGENT: '紧急',
};

// 产品类型枚举 → 中文标签映射
const PRODUCT_TYPE_LABELS = {
  AIR_CONDITIONER: '空调',
  REFRIGERATOR: '冰箱',
  WASHING_MACHINE: '洗衣机',
  WATER_HEATER: '热水器',
  WATER_PURIFIER: '净水器',
  RANGE_HOOD: '油烟机',
  GAS_STOVE: '燃气灶',
  TELEVISION: '电视',
  OTHER: '其他',
};

/**
 * 获取状态标签
 */
function getStatusLabel(status) {
  return STATUS_LABELS[status] || status || '未知';
}

/**
 * 获取状态颜色类名后缀
 */
function getStatusColor(status) {
  return STATUS_COLORS[status] || 'closed';
}

/**
 * 获取优先级标签
 */
function getPriorityLabel(priority) {
  return PRIORITY_LABELS[priority] || priority || '未知';
}

/**
 * 获取产品类型中文标签
 * 后端返回 AIR_CONDITIONER，显示为"空调"
 */
function getProductTypeLabel(productType) {
  return PRODUCT_TYPE_LABELS[productType] || productType || '未填写';
}

module.exports = {
  STATUS,
  STATUS_LABELS,
  STATUS_COLORS,
  TECHNICIAN_TABS,
  PRIORITY,
  PRIORITY_LABELS,
  PRODUCT_TYPE_LABELS,
  getStatusLabel,
  getStatusColor,
  getPriorityLabel,
  getProductTypeLabel,
};
