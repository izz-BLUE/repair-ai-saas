import http from './http';
import type { PageResult } from './knowledge';

export interface KnowledgeDocument {
  id: number;
  tenantId: number;
  knowledgeBaseId: number;
  originalFilename: string;
  storedFilename: string;
  contentType: string | null;
  fileSize: number;
  storagePath: string;
  parseStatus: string;
  itemCount: number;
  errorMessage: string | null;
  createdBy: number | null;
  createdAt: string;
  updatedAt: string;
}

export function listDocuments(params: {
  page?: number;
  size?: number;
  knowledgeBaseId?: number;
  parseStatus?: string;
}): Promise<PageResult<KnowledgeDocument>> {
  return http.get('/api/admin/knowledge-documents', { params });
}

export function getDocument(id: number): Promise<KnowledgeDocument> {
  return http.get(`/api/admin/knowledge-documents/${id}`);
}

export function uploadDocument(
  knowledgeBaseId: number,
  file: File,
): Promise<KnowledgeDocument> {
  const formData = new FormData();
  formData.append('knowledgeBaseId', String(knowledgeBaseId));
  formData.append('file', file);
  return http.post('/api/admin/knowledge-documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function deleteDocument(id: number): Promise<void> {
  return http.delete(`/api/admin/knowledge-documents/${id}`);
}

export function reparseDocument(id: number): Promise<KnowledgeDocument> {
  return http.post(`/api/admin/knowledge-documents/${id}/reparse`);
}
