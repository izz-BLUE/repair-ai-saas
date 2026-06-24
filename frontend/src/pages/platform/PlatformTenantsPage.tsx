import React, { useEffect, useState } from 'react';
import {
  Table, Button, Tag, Space, Modal, Form, Input, App, Popconfirm, Descriptions,
  Dropdown,
} from 'antd';
import { PlusOutlined, ReloadOutlined, DownOutlined } from '@ant-design/icons';
import {
  listTenants, createTenant, resetAdminPassword,
  activateTenant, suspendTenant, restoreTenant, closeTenant, applyPlan,
  type PlatformTenant,
} from '../../api/platform';

const STATUS_MAP: Record<string, { color: string; label: string }> = {
  TRIAL: { color: 'blue', label: '试用中' },
  ACTIVE: { color: 'green', label: '正式' },
  EXPIRED: { color: 'red', label: '已到期' },
  SUSPENDED: { color: 'orange', label: '已暂停' },
  CLOSED: { color: 'default', label: '已关闭' },
};

const PLAN_OPTIONS = [
  { key: 'TRIAL', label: '试用版 (TRIAL)' },
  { key: 'STARTER', label: '入门版 (STARTER)' },
  { key: 'PRO', label: '专业版 (PRO)' },
  { key: 'LEGACY', label: '历史版 (LEGACY)' },
];

function formatDate(d: string | null): string {
  if (!d) return '-';
  return d.slice(0, 19)?.replace('T', ' ') || d;
}

function formatDateShort(d: string | null): string {
  if (!d) return '-';
  return d.slice(0, 10);
}

function formatNullable(v: number | null): string {
  if (v == null) return '不限';
  return String(v);
}

const PlatformTenantsPage: React.FC = () => {
  const [data, setData] = useState<PlatformTenant[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createResult, setCreateResult] = useState<{ tenantCode: string; adminUsername: string; adminPassword: string } | null>(null);
  const [form] = Form.useForm();
  const { message, modal } = App.useApp();

  const fetchData = async (p = 1) => {
    setLoading(true);
    try {
      const res = await listTenants({ page: p, size: 20 });
      setData(res.records);
      setTotal(res.total);
      setPage(p);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const withAction = async (id: number, fn: () => Promise<void>, okMsg: string) => {
    setActionLoading(id);
    try {
      await fn();
      message.success(okMsg);
      fetchData(page);
    } catch {
      // error already shown by http.ts
    } finally {
      setActionLoading(null);
    }
  };

  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      const res = await createTenant(values);
      setCreateResult({
        tenantCode: res.tenant.tenantCode,
        adminUsername: res.adminUsername,
        adminPassword: res.adminPassword,
      });
      form.resetFields();
      fetchData();
    } catch { /* validation */ }
  };

  const handleSuspend = (record: PlatformTenant) => {
    modal.confirm({
      title: '确认暂停该租户？',
      content: '暂停后该租户将无法登录和使用业务功能。',
      okText: '确认暂停',
      okType: 'danger',
      onOk: () => withAction(record.id, () => suspendTenant(record.id), '已暂停'),
    });
  };

  const handleActivate = (record: PlatformTenant) => {
    withAction(record.id, () => activateTenant(record.id), '已启用');
  };

  const handleRestore = (record: PlatformTenant) => {
    modal.confirm({
      title: '确认恢复该租户？',
      content: '恢复后该租户将重新获得访问和使用权限。',
      okText: '确认恢复',
      onOk: () => withAction(record.id, () => restoreTenant(record.id), '已恢复'),
    });
  };

  const handleClose = (record: PlatformTenant) => {
    modal.confirm({
      title: '确认关闭租户？',
      content: '关闭后该租户将无法登录和使用任何功能，此操作不可随意恢复。如需恢复请联系开发人员。',
      okText: '确认关闭',
      okType: 'danger',
      onOk: () => withAction(record.id, () => closeTenant(record.id), '已关闭'),
    });
  };

  const handleApplyPlan = (record: PlatformTenant, planCode: string) => {
    const planLabel = PLAN_OPTIONS.find(p => p.key === planCode)?.label || planCode;
    modal.confirm({
      title: '确认应用套餐？',
      content: `应用「${planLabel}」将覆盖当前所有额度设置，确认继续？`,
      okText: '确认应用',
      onOk: () => withAction(record.id, () => applyPlan(record.id, planCode), '套餐已应用'),
    });
  };

  const handleResetPwd = async (id: number) => {
    const res = await resetAdminPassword(id);
    Modal.info({
      title: '密码已重置',
      content: <div><p>新密码：<code>{res.newPassword}</code></p><p>请及时通知企业管理员修改密码。</p></div>,
    });
  };

  const columns = [
    { title: '企业名称', dataIndex: 'name', key: 'name', width: 140, render: (v: string) => v || '-' },
    { title: '租户编码', dataIndex: 'tenantCode', key: 'tenantCode', width: 100, render: (v: string) => <code>{v}</code> },
    { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 80, render: (v: string) => v || '-' },
    { title: '电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 110, render: (v: string) => v || '-' },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80,
      render: (v: string) => {
        const m = STATUS_MAP[v];
        return m ? <Tag color={m.color}>{m.label}</Tag> : <Tag>{v || '-'}</Tag>;
      },
    },
    {
      title: '套餐', dataIndex: 'planName', key: 'planName', width: 90,
      render: (v: string, record: PlatformTenant) => {
        const name = v || record.planCode;
        return name ? <Tag color="purple">{name}</Tag> : '-';
      },
    },
    {
      title: '试用到期', dataIndex: 'trialEndAt', key: 'trialEndAt', width: 100,
      render: (v: string, record: PlatformTenant) =>
        record.status === 'TRIAL' && v ? formatDateShort(v) : '-',
    },
    {
      title: '到期时间', dataIndex: 'expiredAt', key: 'expiredAt', width: 100,
      render: (v: string) => v ? formatDateShort(v) : '-',
    },
    {
      title: '门户', dataIndex: 'portalEnabled', key: 'portalEnabled', width: 70,
      render: (v: boolean) => v ? <Tag color="blue">已启用</Tag> : <Tag>已关闭</Tag>,
    },
    {
      title: '员工', dataIndex: 'maxUsers', key: 'maxUsers', width: 60,
      render: (v: number | null) => formatNullable(v),
    },
    {
      title: '师傅', dataIndex: 'maxTechnicians', key: 'maxTechnicians', width: 60,
      render: (v: number | null) => formatNullable(v),
    },
    {
      title: 'AI限额', dataIndex: 'maxAiDailyCalls', key: 'maxAiDailyCalls', width: 70,
      render: (v: number | null) => formatNullable(v),
    },
    {
      title: '月工单', dataIndex: 'ticketMonthlyLimit', key: 'ticketMonthlyLimit', width: 70,
      render: (v: number | null) => formatNullable(v),
    },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 140, render: (v: string) => formatDate(v) },
    {
      title: '操作', key: 'action', width: 280, fixed: 'right' as const,
      render: (_: unknown, record: PlatformTenant) => {
        const isLoading = actionLoading === record.id;
        const isClosed = record.status === 'CLOSED';
        const isSuspended = record.status === 'SUSPENDED';
        const isExpired = record.status === 'EXPIRED';
        const isActive = record.status === 'ACTIVE';

        return (
          <Space size="small" wrap>
            {/* 启用/暂停 */}
            {!isClosed && (
              isActive ? (
                <Button size="small" danger loading={isLoading}
                  onClick={() => handleSuspend(record)}>
                  暂停
                </Button>
              ) : (
                <Button size="small" type="primary" loading={isLoading}
                  onClick={() => handleActivate(record)}>
                  启用
                </Button>
              )
            )}

            {/* 恢复 */}
            {(isSuspended || isExpired) && (
              <Button size="small" loading={isLoading}
                onClick={() => handleRestore(record)}>
                恢复
              </Button>
            )}

            {/* 关闭 */}
            {!isClosed && (
              <Button size="small" danger loading={isLoading}
                onClick={() => handleClose(record)}>
                关闭
              </Button>
            )}

            {/* 应用套餐 */}
            {!isClosed && (
              <Dropdown
                menu={{
                  items: PLAN_OPTIONS.map(p => ({
                    key: p.key,
                    label: p.label,
                    onClick: () => handleApplyPlan(record, p.key),
                  })),
                }}
                disabled={isLoading}
              >
                <Button size="small" loading={isLoading}>
                  套餐 <DownOutlined />
                </Button>
              </Dropdown>
            )}

            {/* 重置密码 */}
            <Popconfirm title="确认重置该租户管理员密码？" onConfirm={() => handleResetPwd(record.id)}>
              <Button size="small" loading={isLoading}>重置密码</Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600, color: '#0f172a' }}>平台租户管理</h2>
          <p style={{ margin: '4px 0 0', color: '#64748b', fontSize: 14 }}>管理所有企业租户</p>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => fetchData(page)}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setCreateResult(null); setCreateOpen(true); }}>
            创建租户
          </Button>
        </Space>
      </div>

      <Table
        dataSource={data}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{ current: page, total, pageSize: 20, onChange: fetchData }}
        size="middle"
        scroll={{ x: 1600 }}
      />

      <Modal
        title={createResult ? '租户创建成功' : '创建租户'}
        open={createOpen}
        onCancel={() => { setCreateOpen(false); setCreateResult(null); }}
        footer={createResult ? [
          <Button key="close" onClick={() => { setCreateOpen(false); setCreateResult(null); }}>关闭</Button>,
        ] : undefined}
        onOk={createResult ? undefined : handleCreate}
        destroyOnHidden
      >
        {createResult ? (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="租户编码"><code>{createResult.tenantCode}</code></Descriptions.Item>
            <Descriptions.Item label="管理员用户名"><code>{createResult.adminUsername}</code></Descriptions.Item>
            <Descriptions.Item label="管理员密码"><code>{createResult.adminPassword}</code></Descriptions.Item>
          </Descriptions>
        ) : (
          <Form form={form} layout="vertical">
            <Form.Item name="name" label="企业名称" rules={[{ required: true, message: '请输入企业名称' }]}>
              <Input placeholder="例如：XX 维修公司" />
            </Form.Item>
            <Form.Item name="contactName" label="联系人">
              <Input placeholder="联系人姓名" />
            </Form.Item>
            <Form.Item name="contactPhone" label="联系电话">
              <Input placeholder="联系电话" />
            </Form.Item>
          </Form>
        )}
      </Modal>
    </div>
  );
};

export default PlatformTenantsPage;
