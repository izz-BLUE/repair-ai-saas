import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Typography, Tag, Spin, App, Segmented } from 'antd';
import {
  ClockCircleOutlined,
  EnvironmentOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { listMyTickets } from '../../api/technician';
import type { Ticket } from '../../api/tickets';

const { Text } = Typography;

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待处理', color: '#d97706' },
  ASSIGNED: { label: '已派单', color: '#2563eb' },
  IN_PROGRESS: { label: '处理中', color: '#7c3aed' },
  COMPLETED: { label: '已完成', color: '#059669' },
  FOLLOWED_UP: { label: '已回访', color: '#0891b2' },
  CLOSED: { label: '已关闭', color: '#6b7280' },
  CANCELLED: { label: '已取消', color: '#dc2626' },
};

const STATUS_OPTIONS = [
  { label: '全部', value: '' },
  { label: '已派单', value: 'ASSIGNED' },
  { label: '处理中', value: 'IN_PROGRESS' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已关闭', value: 'CLOSED' },
];

const TechnicianTicketsPage: React.FC = () => {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const navigate = useNavigate();
  const { message } = App.useApp();

  const fetchTickets = useCallback(async () => {
    setLoading(true);
    try {
      const params: { page?: number; size?: number; status?: string } = { page: 1, size: 100 };
      if (statusFilter) {
        params.status = statusFilter;
      }
      const result = await listMyTickets(params);
      setTickets(result.records || []);
    } catch {
      message.error('加载工单失败，请下拉重试');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, message]);

  useEffect(() => {
    fetchTickets();
  }, [fetchTickets]);

  return (
    <div style={{ maxWidth: 480, margin: '0 auto' }}>
      {/* 状态筛选 */}
      <div style={{ marginBottom: 16, overflowX: 'auto', whiteSpace: 'nowrap', paddingBottom: 4 }}>
        <Segmented
          block
          options={STATUS_OPTIONS}
          value={statusFilter}
          onChange={(val) => setStatusFilter(val as string)}
          style={{ width: '100%' }}
        />
      </div>

      {/* 工单计数 */}
      <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 12 }}>
        共 {tickets.length} 个工单
      </Text>

      {/* 工单列表 */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: 80 }}>
          <Spin size="large" />
          <div style={{ marginTop: 12, color: '#94a3b8', fontSize: 13 }}>加载中...</div>
        </div>
      ) : tickets.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 80 }}>
          <SearchOutlined style={{ fontSize: 48, color: '#cbd5e1' }} />
          <div style={{ marginTop: 16, color: '#94a3b8', fontSize: 14 }}>
            {statusFilter ? '暂无该状态的工单' : '暂无工单'}
          </div>
          <div style={{ marginTop: 4, color: '#cbd5e1', fontSize: 12 }}>
            {statusFilter ? '尝试切换筛选条件' : '新工单将在这里显示'}
          </div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {tickets.map((ticket) => {
            const statusInfo = STATUS_MAP[ticket.status] || { label: ticket.status, color: '#6b7280' };
            return (
              <div
                key={ticket.id}
                onClick={() => navigate(`/technician/tickets/${ticket.id}`)}
                style={{
                  background: '#fff',
                  borderRadius: 12,
                  padding: '16px',
                  border: '1px solid #e2e8f0',
                  cursor: 'pointer',
                  transition: 'box-shadow 0.15s, border-color 0.15s',
                  boxShadow: '0 1px 2px rgba(0,0,0,0.03)',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.06)';
                  e.currentTarget.style.borderColor = '#cbd5e1';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.boxShadow = '0 1px 2px rgba(0,0,0,0.03)';
                  e.currentTarget.style.borderColor = '#e2e8f0';
                }}
              >
                {/* 第一行：工单号 + 状态 */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                  <Text
                    copyable
                    style={{ fontFamily: 'monospace', fontSize: 13, fontWeight: 600, color: '#0f172a' }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    {ticket.ticketNo}
                  </Text>
                  <Tag
                    color={statusInfo.color}
                    style={{ margin: 0, borderRadius: 6, fontSize: 12, padding: '0 8px' }}
                  >
                    {statusInfo.label}
                  </Tag>
                </div>

                {/* 第二行：客户 + 产品 */}
                <div style={{ marginBottom: 8 }}>
                  <Text style={{ fontSize: 14, color: '#0f172a' }}>
                    {ticket.customerName}
                  </Text>
                  <Text style={{ fontSize: 12, color: '#94a3b8', marginLeft: 8 }}>
                    {ticket.productType || '未指定产品'}
                  </Text>
                </div>

                {/* 第三行：故障描述 */}
                {ticket.faultDescription && (
                  <div style={{
                    background: '#f8fafc',
                    borderRadius: 6,
                    padding: '6px 10px',
                    marginBottom: 8,
                    fontSize: 12,
                    color: '#475569',
                    lineHeight: '18px',
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical' as const,
                    overflow: 'hidden',
                  }}>
                    {ticket.faultDescription}
                  </div>
                )}

                {/* 第四行：地址 + 时间 */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  {ticket.serviceAddress && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                      <EnvironmentOutlined style={{ fontSize: 11, color: '#94a3b8' }} />
                      <Text style={{ fontSize: 11, color: '#94a3b8' }} ellipsis>
                        {ticket.serviceAddress}
                      </Text>
                    </div>
                  )}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <ClockCircleOutlined style={{ fontSize: 11, color: '#94a3b8' }} />
                    <Text style={{ fontSize: 11, color: '#94a3b8' }}>
                      {ticket.createdAt ? new Date(ticket.createdAt).toLocaleString('zh-CN', {
                        month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit',
                      }) : '-'}
                    </Text>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default TechnicianTicketsPage;
