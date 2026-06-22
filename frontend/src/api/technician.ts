import http from './http';
import type { Ticket, TicketStatusLog } from './tickets';
import type { PageResult } from './knowledge';

/** 师傅端：获取我的工单列表 */
export const listMyTickets = (params: {
  page?: number;
  size?: number;
  status?: string;
}): Promise<PageResult<Ticket>> =>
  http.get('/api/technician/tickets', { params });

/** 师傅端：获取工单详情 */
export const getMyTicket = (id: number): Promise<{ ticket: Ticket; statusLogs: TicketStatusLog[] }> =>
  http.get(`/api/technician/tickets/${id}`);

/** 师傅端：开始处理工单 */
export const startProcess = (id: number): Promise<void> =>
  http.put(`/api/technician/tickets/${id}/start`);

/** 师傅端：完成工单 */
export const completeTicket = (id: number, data: {
  repairResult: string;
  costNote?: string;
  partsNote?: string;
  remark?: string;
}): Promise<void> =>
  http.put(`/api/technician/tickets/${id}/complete`, data);
