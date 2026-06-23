import React, { useEffect, useState, useCallback } from 'react';
import {
  Table, Button, Select, Input, Drawer, Tag, App, Typography, Timeline, Divider, Modal, Form, DatePicker,
} from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  listTickets, getTicket, assignTicket, closeTicket, cancelTicket,
  listTechnicians,
  type Ticket, type TicketStatusLog, type Technician,
} from '../api/tickets';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';

const { Title, Text } = Typography;

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待处理', color: 'orange' },
  ASSIGNED: { label: '已派单', color: 'blue' },
  IN_PROGRESS: { label: '处理中', color: 'purple' },
  COMPLETED: { label: '已完成', color: 'green' },
  FOLLOWED_UP: { label: '已回访', color: 'cyan' },
  CLOSED: { label: '已关闭', color: 'default' },
  CANCELLED: { label: '已取消', color: 'red' },
};

const PRIORITY_MAP: Record<string, { label: string; color: string }> = {
  LOW: { label: '低', color: 'default' },
  NORMAL: { label: '普通', color: 'blue' },
  HIGH: { label: '高', color: 'orange' },
  URGENT: { label: '紧急', color: 'red' },
};

const STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  ...Object.entries(STATUS_MAP).map(([k, v]) => ({ value: k, label: v.label })),
];

const PRIORITY_OPTIONS = [
  { value: '', label: '全部优先级' },
  ...Object.entries(PRIORITY_MAP).map(([k, v]) => ({ value: k, label: v.label })),
];

const formatDate = (d: string | null | undefined): string => {
  if (!d) return '-';
  const date = new Date(d);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const TicketPage: React.FC = () => {
  const { message } = App.useApp();

  // 列表状态
  const [data, setData] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size] = useState(20);
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [keyword, setKeyword] = useState('');

  // 详情抽屉
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [currentTicket, setCurrentTicket] = useState<Ticket | null>(null);
  const [currentLogs, setCurrentLogs] = useState<TicketStatusLog[]>([]);
  const [drawerLoading, setDrawerLoading] = useState(false);

  // 派单 Modal
  const [assignOpen, setAssignOpen] = useState(false);
  const [technicians, setTechnicians] = useState<Technician[]>([]);
  const [assignForm] = Form.useForm();

  const fetchData = useCallback(async (p = page) => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page: p, size };
      if (statusFilter) params.status = statusFilter;
      if (priorityFilter) params.priority = priorityFilter;
      if (keyword.trim()) params.keyword = keyword.trim();
      const res = await listTickets(params as { page: number; size: number; status?: string; priority?: string; keyword?: string });
      setData(res.records || []);
      setTotal(res.total || 0);
    } catch {
      // handled by http interceptor
    } finally {
      setLoading(false);
    }
  }, [page, size, statusFilter, priorityFilter, keyword]);

  useEffect(() => { fetchData(1); }, [statusFilter, priorityFilter]);

  const handleSearch = () => {
    setPage(1);
    fetchData(1);
  };

  const handlePageChange = (p: number) => {
    setPage(p);
    fetchData(p);
  };

  const openDrawer = async (record: Ticket) => {
    setDrawerOpen(true);
    setDrawerLoading(true);
    try {
      const res = await getTicket(record.id);
      setCurrentTicket(res.ticket);
      setCurrentLogs(res.statusLogs || []);
    } catch {
      // handled
    } finally {
      setDrawerLoading(false);
    }
  };

  const refreshDrawer = async () => {
    if (!currentTicket) return;
    try {
      const res = await getTicket(currentTicket.id);
      setCurrentTicket(res.ticket);
      setCurrentLogs(res.statusLogs || []);
      fetchData();
    } catch {
      // handled
    }
  };

  // 派单
  const handleAssign = async () => {
    if (!currentTicket) return;
    try {
      const values = await assignForm.validateFields();
      const data: { technicianId: number; scheduledTime?: string } = {
        technicianId: values.technicianId,
      };
      if (values.scheduledTime) {
        data.scheduledTime = (values.scheduledTime as Dayjs).format('YYYY-MM-DDTHH:mm:ss');
      }
      await assignTicket(currentTicket.id, data);
      message.success('派单成功');
      setAssignOpen(false);
      assignForm.resetFields();
      refreshDrawer();
    } catch {
      // handled
    }
  };

  const openAssignModal = async () => {
    try {
      const list = await listTechnicians();
      setTechnicians(list || []);
      assignForm.resetFields();
      setAssignOpen(true);
    } catch {
      // handled
    }
  };

  // 取消
  const handleCancel = async () => {
    if (!currentTicket) return;
    try {
      await cancelTicket(currentTicket.id);
      message.success('工单已取消');
      refreshDrawer();
    } catch {
      // handled
    }
  };

  // 关闭
  const handleClose = async () => {
    if (!currentTicket) return;
    try {
      await closeTicket(currentTicket.id);
      message.success('工单已关闭');
      refreshDrawer();
    } catch {
      // handled
    }
  };

  const columns: ColumnsType<Ticket> = [
    {
      title: '工单号', dataIndex: 'ticketNo', width: 160,
      render: (v: string) => <Text style={{ fontFamily: 'monospace', color: '#0f172a' }}>{v}</Text>,
    },
    { title: '客户', dataIndex: 'customerName', width: 100 },
    { title: '产品类型', dataIndex: 'productType', width: 100, render: (v: string) => v || '-' },
    {
      title: '优先级', dataIndex: 'priority', width: 80,
      render: (v: string) => {
        const p = PRIORITY_MAP[v];
        return p ? <Tag color={p.color}>{p.label}</Tag> : <Tag>{v}</Tag>;
      },
    },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (v: string) => {
        const s = STATUS_MAP[v];
        return s ? <Tag color={s.color}>{s.label}</Tag> : <Tag>{v}</Tag>;
      },
    },
    {
      title: '师傅', dataIndex: 'technicianId', width: 80,
      render: (v: number | null) => v ? <span>{v}</span> : <span style={{ color: '#94a3b8' }}>-</span>,
    },
    {
      title: '创建时间', dataIndex: 'createdAt', width: 150,
      render: (v: string) => formatDate(v),
    },
    {
      title: '操作', width: 80, fixed: 'right',
      render: (_: unknown, record: Ticket) => (
        <Button type="link" size="small" onClick={() => openDrawer(record)}>详情</Button>
      ),
    },
  ];

  const renderActions = () => {
    if (!currentTicket) return null;
    const s = currentTicket.status;
    return (
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        {s === 'PENDING' && (
          <Button type="primary" onClick={openAssignModal}>派单</Button>
        )}
        {(s === 'PENDING' || s === 'ASSIGNED') && (
          <Button danger onClick={handleCancel}>取消</Button>
        )}
        {s === 'COMPLETED' && (
          <Button type="primary" onClick={handleClose}>关闭</Button>
        )}
      </div>
    );
  };

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={4} style={{ margin: 0, color: '#0f172a' }}>工单管理</Title>
        <Text style={{ color: '#64748b' }}>管理客户报修工单，跟踪处理进度</Text>
      </div>

      {/* 筛选栏 */}
      <div style={{
        display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap', alignItems: 'center',
      }}>
        <Select
          value={statusFilter}
          onChange={(v) => { setStatusFilter(v); setPage(1); }}
          options={STATUS_OPTIONS}
          style={{ width: 130 }}
          size="middle"
        />
        <Select
          value={priorityFilter}
          onChange={(v) => { setPriorityFilter(v); setPage(1); }}
          options={PRIORITY_OPTIONS}
          style={{ width: 130 }}
          size="middle"
        />
        <Input.Search
          placeholder="工单号/客户名/手机号"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={handleSearch}
          style={{ width: 240 }}
          size="middle"
          enterButton={<SearchOutlined />}
        />
        <Button icon={<ReloadOutlined />} onClick={() => { setKeyword(''); setStatusFilter(''); setPriorityFilter(''); setPage(1); fetchData(1); }}>
          重置
        </Button>
      </div>

      {/* 工单表格 */}
      <Table
        dataSource={data}
        columns={columns}
        rowKey="id"
        loading={loading}
        scroll={{ x: 960 }}
        pagination={{
          current: page, pageSize: size, total, showTotal: (t) => `共 ${t} 条`,
          onChange: handlePageChange, showSizeChanger: false,
        }}
        size="middle"
      />

      {/* 详情抽屉 */}
      <Drawer
        title="工单详情"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={560}
        destroyOnHidden
      >
        {drawerLoading ? (
          <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8' }}>加载中...</div>
        ) : currentTicket ? (
          <div>
            {/* 操作按钮 */}
            <div style={{ marginBottom: 20 }}>{renderActions()}</div>
            <Divider style={{ margin: '16px 0' }} />

            {/* 基本信息 */}
            <h4 style={{ fontSize: 14, fontWeight: 600, color: '#0f172a', margin: '0 0 12px' }}>基本信息</h4>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px 16px', marginBottom: 16 }}>
              <InfoCell label="工单号" value={currentTicket.ticketNo} mono />
              <InfoCell label="状态" value={
                <Tag color={STATUS_MAP[currentTicket.status]?.color || 'default'}>
                  {STATUS_MAP[currentTicket.status]?.label || currentTicket.status}
                </Tag>
              } />
              <InfoCell label="优先级" value={
                <Tag color={PRIORITY_MAP[currentTicket.priority]?.color || 'default'}>
                  {PRIORITY_MAP[currentTicket.priority]?.label || currentTicket.priority}
                </Tag>
              } />
              <InfoCell label="产品类型" value={currentTicket.productType || '-'} />
              <InfoCell label="故障类型" value={currentTicket.faultType || '-'} />
              <InfoCell label="创建时间" value={formatDate(currentTicket.createdAt)} />
              {currentTicket.scheduledTime && <InfoCell label="预约时间" value={formatDate(currentTicket.scheduledTime)} />}
              {currentTicket.startTime && <InfoCell label="开始处理" value={formatDate(currentTicket.startTime)} />}
              {currentTicket.completionTime && <InfoCell label="完成时间" value={formatDate(currentTicket.completionTime)} />}
            </div>

            {/* 故障描述 */}
            {currentTicket.faultDescription && (
              <div style={{ marginBottom: 16 }}>
                <Text type="secondary" style={{ fontSize: 12 }}>故障描述</Text>
                <div style={{ fontSize: 13, color: '#0f172a', marginTop: 4, lineHeight: 1.6 }}>
                  {currentTicket.faultDescription}
                </div>
              </div>
            )}

            {/* 维修结果 */}
            {currentTicket.repairResult && (
              <div style={{ marginBottom: 16 }}>
                <Text type="secondary" style={{ fontSize: 12 }}>维修结果</Text>
                <div style={{ fontSize: 13, color: '#0f172a', marginTop: 4, lineHeight: 1.6 }}>
                  {currentTicket.repairResult}
                </div>
              </div>
            )}

            <Divider style={{ margin: '16px 0' }} />

            {/* 客户信息 */}
            <h4 style={{ fontSize: 14, fontWeight: 600, color: '#0f172a', margin: '0 0 12px' }}>客户信息</h4>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px 16px', marginBottom: 16 }}>
              <InfoCell label="客户姓名" value={currentTicket.customerName} />
              <InfoCell label="手机号" value={currentTicket.customerPhone} />
              <InfoCell label="地址" value={currentTicket.serviceAddress || '-'} gridSpan />
            </div>

            <Divider style={{ margin: '16px 0' }} />

            {/* 处理记录 */}
            <h4 style={{ fontSize: 14, fontWeight: 600, color: '#0f172a', margin: '0 0 12px' }}>处理记录</h4>
            {currentLogs.length === 0 ? (
              <Text type="secondary">暂无记录</Text>
            ) : (
              <Timeline
                items={currentLogs.map((log) => {
                  const s = STATUS_MAP[log.toStatus];
                  return {
                    color: s?.color || 'gray',
                    children: (
                      <div>
                        <div style={{ fontSize: 13, fontWeight: 600, color: '#0f172a' }}>
                          {s?.label || log.toStatus}
                          {log.fromStatus && log.fromStatus !== log.toStatus && (
                            <span style={{ fontWeight: 400, color: '#94a3b8', marginLeft: 8 }}>
                              (从 {STATUS_MAP[log.fromStatus]?.label || log.fromStatus})
                            </span>
                          )}
                        </div>
                        {log.remark && <div style={{ fontSize: 12, color: '#64748b' }}>{log.remark}</div>}
                        <div style={{ fontSize: 11, color: '#94a3b8' }}>{formatDate(log.createdAt)}</div>
                      </div>
                    ),
                  };
                })}
              />
            )}
          </div>
        ) : null}
      </Drawer>

      {/* 派单 Modal */}
      <Modal
        title="派单"
        open={assignOpen}
        onOk={handleAssign}
        onCancel={() => setAssignOpen(false)}
        okText="确认派单"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={assignForm} layout="vertical">
          <Form.Item name="technicianId" label="选择师傅" rules={[{ required: true, message: '请选择师傅' }]}>
            <Select placeholder="请选择师傅">
              {technicians.map((t) => (
                <Select.Option key={t.id} value={t.id}>
                  {t.realName}（{t.phone}）
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="scheduledTime" label="预约时间（选填）">
            <DatePicker showTime style={{ width: '100%' }} placeholder="选择预约时间" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

/** 信息单元格 */
const InfoCell: React.FC<{ label: string; value: React.ReactNode; mono?: boolean; gridSpan?: boolean }> = ({
  label, value, mono, gridSpan,
}) => (
  <div style={gridSpan ? { gridColumn: '1 / -1' } : undefined}>
    <div style={{ fontSize: 12, color: '#94a3b8', marginBottom: 2 }}>{label}</div>
    <div style={{ fontSize: 13, color: '#0f172a', fontFamily: mono ? 'monospace' : undefined }}>{value}</div>
  </div>
);

export default TicketPage;
