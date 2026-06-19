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

/** 租户列表（分页） */
export const listTenants = (params: { page?: number; size?: number }): Promise<{ total: number; page: number; size: number; records: PlatformTenant[] }> =>
  http.get('/api/platform/tenants', { params });

/** 创建租户 */
export const createTenant = (data: CreateTenantRequest): Promise<{ tenant: PlatformTenant; adminUsername: string; adminPassword: string }> =>
  http.post('/api/platform/tenants', data);

/** 租户详情 */
export const getTenant = (id: number): Promise<PlatformTenant> =>
  http.get(`/api/platform/tenants/${id}`);

/** 编辑租户 */
export const updateTenant = (id: number, data: UpdateTenantRequest): Promise<void> =>
  http.put(`/api/platform/tenants/${id}`, data);

/** 启用租户 */
export const enableTenant = (id: number): Promise<void> =>
  http.post(`/api/platform/tenants/${id}/enable`);

/** 禁用租户 */
export const disableTenant = (id: number): Promise<void> =>
  http.post(`/api/platform/tenants/${id}/disable`);

/** 重置管理员密码 */
export const resetAdminPassword = (id: number): Promise<{ newPassword: string }> =>
  http.post(`/api/platform/tenants/${id}/reset-admin-password`);
