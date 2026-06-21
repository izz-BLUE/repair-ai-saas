import http from './http';
import type { PageResult } from './knowledge';

/** 工单 */
export interface Ticket {
  id: number;
  tenantId: number;
  ticketNo: string;
  customerId: number;
  customerName: string;
  customerPhone: string;
  serviceAddress: string;
  productType: string;
  faultType: string;
  faultDescription: string;
  priority: string;
  status: string;
  technicianId: number | null;
  scheduledTime: string | null;
  startTime: string | null;
  completionTime: string | null;
  repairResult: string | null;
  costNote: string | null;
  partsNote: string | null;
  createdAt: string;
  updatedAt: string;
}

/** 工单状态日志 */
export interface TicketStatusLog {
  id: number;
  tenantId: number;
  ticketId: number;
  fromStatus: string | null;
  toStatus: string;
  operatorId: number | null;
  remark: string;
  createdAt: string;
}

/** Dashboard 统计 */
export interface DashboardStats {
  todayNewTickets: number;
  pendingTickets: number;
  processingTickets: number;
  completedTickets: number;
  todayAiChats: number;
}

/** 工单列表 */
export const listTickets = (params: {
  page?: number;
  size?: number;
  status?: string;
  priority?: string;
  keyword?: string;
}): Promise<PageResult<Ticket>> =>
  http.get('/api/admin/tickets', { params });

/** 工单详情（含状态日志） */
export const getTicket = (id: number): Promise<{ ticket: Ticket; statusLogs: TicketStatusLog[] }> =>
  http.get(`/api/admin/tickets/${id}`);

/** 派单 */
export const assignTicket = (id: number, data: { technicianId: number; scheduledTime?: string }): Promise<void> =>
  http.put(`/api/admin/tickets/${id}/assign`, data);

/** 关闭工单 */
export const closeTicket = (id: number): Promise<void> =>
  http.put(`/api/admin/tickets/${id}/close`);

/** 取消工单 */
export const cancelTicket = (id: number, remark?: string): Promise<void> =>
  http.put(`/api/admin/tickets/${id}/cancel`, { remark });

/** Dashboard 统计 */
export const getDashboardStats = (): Promise<DashboardStats> =>
  http.get('/api/admin/dashboard/stats');

/** 师傅列表（用于派单下拉） */
export interface Technician {
  id: number;
  realName: string;
  phone: string;
}

export const listTechnicians = (): Promise<Technician[]> =>
  http.get('/api/admin/technicians');
