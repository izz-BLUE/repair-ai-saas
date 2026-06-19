import http from './http';

// ========== Knowledge Base ==========

export interface KnowledgeBase {
  id: number;
  tenantId: number;
  name: string;
  description: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageResult<T> {
  total: number;
  page: number;
  size: number;
  records: T[];
}

export function listKnowledgeBases(params: {
  page?: number;
  size?: number;
  keyword?: string;
}): Promise<PageResult<KnowledgeBase>> {
  return http.get('/api/admin/knowledge-bases', { params });
}

export function createKnowledgeBase(data: {
  name: string;
  description?: string;
}): Promise<KnowledgeBase> {
  return http.post('/api/admin/knowledge-bases', data);
}

export function updateKnowledgeBase(
  id: number,
  data: { name?: string; description?: string },
): Promise<void> {
  return http.put(`/api/admin/knowledge-bases/${id}`, data);
}

export function updateKnowledgeBaseStatus(
  id: number,
  status: string,
): Promise<void> {
  return http.put(`/api/admin/knowledge-bases/${id}/status`, { status });
}

// ========== Knowledge Item ==========

export interface KnowledgeItem {
  id: number;
  tenantId: number;
  knowledgeBaseId: number;
  title: string;
  question: string;
  answer: string;
  productType: string | null;
  faultType: string | null;
  keywords: string | null;
  documentId: number | null;
  sortOrder: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export function listKnowledgeItems(params: {
  page?: number;
  size?: number;
  knowledgeBaseId?: number;
  keyword?: string;
}): Promise<PageResult<KnowledgeItem>> {
  return http.get('/api/admin/knowledge-items', { params });
}

export function createKnowledgeItem(data: {
  knowledgeBaseId: number;
  title: string;
  question: string;
  answer: string;
  productType?: string;
  faultType?: string;
  keywords?: string;
  sortOrder?: number;
}): Promise<KnowledgeItem> {
  return http.post('/api/admin/knowledge-items', data);
}

export function updateKnowledgeItem(
  id: number,
  data: {
    knowledgeBaseId?: number;
    title?: string;
    question?: string;
    answer?: string;
    productType?: string;
    faultType?: string;
    keywords?: string;
    sortOrder?: number;
  },
): Promise<void> {
  return http.put(`/api/admin/knowledge-items/${id}`, data);
}

export function updateKnowledgeItemStatus(
  id: number,
  status: string,
): Promise<void> {
  return http.put(`/api/admin/knowledge-items/${id}/status`, { status });
}

export function syncVectors(): Promise<{ total: number; synced: number; failed: number }> {
  return http.post('/api/admin/knowledge-items/sync-vectors');
}
