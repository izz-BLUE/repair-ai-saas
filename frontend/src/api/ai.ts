import http from './http';
import type { PageResult } from './knowledge';

export interface AiConversation {
  id: number;
  tenantId: number;
  customerName: string | null;
  customerPhone: string | null;
  productType: string | null;
  faultType: string | null;
  lastMessage: string | null;
  messageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface AiMessage {
  id: number;
  conversationId: number;
  role: string;
  content: string;
  model: string | null;
  matchedItemCount: number | null;
  shouldCreateTicket: boolean | null;
  createdAt: string;
}

export function listConversations(params: {
  page?: number;
  size?: number;
}): Promise<PageResult<AiConversation>> {
  return http.get('/api/admin/ai/conversations', { params });
}

export function getConversation(id: number): Promise<{
  conversation: AiConversation;
  messages: AiMessage[];
}> {
  return http.get(`/api/admin/ai/conversations/${id}`);
}
