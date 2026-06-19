import http from './http';

export interface TenantSettings {
  tenantCode: string;
  name: string;
  portalTitle: string;
  portalDescription: string;
  contactPhone: string;
  logoUrl: string;
  themeColor: string;
  portalEnabled: boolean;
}

export interface UpdateSettingsRequest {
  portalTitle?: string;
  portalDescription?: string;
  contactPhone?: string;
  logoUrl?: string;
  themeColor?: string;
  portalEnabled?: boolean;
}

/** 获取当前租户门户配置 */
export const getSettings = (): Promise<TenantSettings> =>
  http.get('/api/admin/settings');

/** 更新当前租户门户配置 */
export const updateSettings = (data: UpdateSettingsRequest): Promise<void> =>
  http.put('/api/admin/settings', data);
