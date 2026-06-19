import http from './http';
import type { PageResult } from './knowledge';

export interface AiConversation {
  id: number;
  tenantId: number;
  customerPhone: string | null;
  customerName: string | null;
  source: string;
  question: string;
  answer: string;
  matchedItemCount: number | null;
  shouldCreateTicket: number | null;
  model: string | null;
  traceId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AiMessage {
  id: number;
  tenantId: number;
  conversationId: number;
  role: string;
  content: string;
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
