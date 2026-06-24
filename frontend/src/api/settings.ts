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

// ==================== 套餐用量 ====================

export interface UsageLimits {
  maxUsers: number | null;
  maxTechnicians: number | null;
  maxKnowledgeBases: number | null;
  maxDocuments: number | null;
  maxAiDailyCalls: number | null;
  ticketMonthlyLimit: number | null;
}

export interface UsageData {
  planCode: string;
  planName: string;
  status: string;
  expiredAt: string | null;
  trialEndAt: string | null;
  daysUntilTrialEnd: number | null;
  daysUntilExpiry: number | null;
  limits: UsageLimits;
  usage: {
    currentUsers: number;
    currentTechnicians: number;
    currentKnowledgeBases: number;
    currentDocuments: number;
    todayAiCalls: number;
    monthlyTickets: number;
  };
}

/** 获取当前租户套餐用量 */
export const getUsage = (): Promise<UsageData> =>
  http.get('/api/admin/settings/usage');
