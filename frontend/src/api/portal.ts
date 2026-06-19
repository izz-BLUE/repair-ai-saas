import http from './http';

export interface AiChatRequest {
  question: string;
  customerName?: string;
  customerPhone?: string;
  productType?: string;
  faultType?: string;
}

export interface AiChatResponse {
  answer: string;
  shouldCreateTicket: boolean;
  matchedItemCount: number;
  conversationId: number;
  traceId: string;
}

export interface RepairRequest {
  name: string;
  phone: string;
  address?: string;
  productType?: string;
  faultDescription: string;
}

export interface RepairResponse {
  ticketNo: string;
  status: string;
  message: string;
}

export interface PortalConfig {
  name: string;
  portalTitle: string;
  portalDescription: string;
  contactPhone: string;
  logoUrl: string;
  themeColor: string;
  portalEnabled: boolean;
  expired?: boolean;
}

/** 获取门户配置（公开接口，无需登录） */
export const getPortalSettings = (tenantCode: string): Promise<PortalConfig> =>
  http.get(`/api/public/${tenantCode}/portal-settings`);

/** AI 智能客服 */
export const aiChat = (tenantCode: string, data: AiChatRequest): Promise<AiChatResponse> =>
  http.post(`/api/public/${tenantCode}/ai/chat`, data);

/** 提交报修 */
export const submitRepair = (tenantCode: string, data: RepairRequest): Promise<RepairResponse> =>
  http.post(`/api/public/${tenantCode}/repair-requests`, data);
