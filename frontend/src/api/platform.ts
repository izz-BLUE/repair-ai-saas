import http from './http';

export interface PlatformTenant {
  id: number;
  tenantCode: string;
  name: string;
  contactName: string;
  contactPhone: string;
  address: string;
  portalTitle: string;
  portalDescription: string;
  logoUrl: string;
  themeColor: string;
  portalEnabled: boolean;
  maxKnowledgeBases: number | null;
  maxDocuments: number | null;
  maxAiDailyCalls: number | null;
  maxUsers: number | null;
  maxTechnicians: number | null;
  ticketMonthlyLimit: number | null;
  planCode: string;
  planName: string;
  trialEndAt: string | null;
  expiredAt: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTenantRequest {
  name: string;
  contactName?: string;
  contactPhone?: string;
}

export interface UpdateTenantRequest {
  name?: string;
  contactName?: string;
  contactPhone?: string;
  address?: string;
  maxKnowledgeBases?: number;
  maxDocuments?: number;
  maxAiDailyCalls?: number;
}

export interface CreateTenantResult {
  tenantId: number;
  tenantCode: string;
  tenantName: string;
  adminUsername: string;
  initialPassword: string;
  planCode: string;
  planName: string;
  status: string;
  trialEndAt: string | null;
  expiredAt: string | null;
}

export interface ResetPasswordResult {
  tenantId: number;
  tenantCode: string;
  adminUsername: string;
  newPassword: string;
}

/** 租户列表（分页） */
export const listTenants = (params: { page?: number; size?: number }): Promise<{ total: number; page: number; size: number; records: PlatformTenant[] }> =>
  http.get('/api/platform/tenants', { params });

/** 创建租户 */
export const createTenant = (data: CreateTenantRequest): Promise<CreateTenantResult> =>
  http.post('/api/platform/tenants', data);

/** 租户详情 */
export const getTenant = (id: number): Promise<PlatformTenant> =>
  http.get(`/api/platform/tenants/${id}`);

/** 编辑租户 */
export const updateTenant = (id: number, data: UpdateTenantRequest): Promise<void> =>
  http.put(`/api/platform/tenants/${id}`, data);

// ==================== 状态操作 ====================

/** 启用租户（设为 ACTIVE） */
export const activateTenant = (id: number): Promise<void> =>
  http.post(`/api/platform/tenants/${id}/activate`);

/** 暂停租户（设为 SUSPENDED） */
export const suspendTenant = (id: number): Promise<void> =>
  http.post(`/api/platform/tenants/${id}/suspend`);

/** 恢复租户（SUSPENDED/EXPIRED → ACTIVE） */
export const restoreTenant = (id: number): Promise<void> =>
  http.post(`/api/platform/tenants/${id}/restore`);

/** 关闭租户（终态 CLOSED） */
export const closeTenant = (id: number): Promise<void> =>
  http.post(`/api/platform/tenants/${id}/close`);

/** 应用套餐预设 */
export const applyPlan = (id: number, planCode: string): Promise<void> =>
  http.put(`/api/platform/tenants/${id}/plan`, { planCode });

// ==================== @Deprecated 兼容 ====================

/** @deprecated 使用 activateTenant */
export const enableTenant = (id: number): Promise<void> =>
  http.post(`/api/platform/tenants/${id}/enable`);

/** @deprecated 使用 suspendTenant */
export const disableTenant = (id: number): Promise<void> =>
  http.post(`/api/platform/tenants/${id}/disable`);

/** 重置管理员密码 */
export const resetAdminPassword = (id: number): Promise<ResetPasswordResult> =>
  http.post(`/api/platform/tenants/${id}/reset-admin-password`);
