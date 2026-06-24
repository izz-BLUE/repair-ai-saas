import React, { useEffect, useState, useMemo } from 'react';
import {
  Table, Button, Tag, Space, Modal, Form, Input, App, Descriptions,
  Dropdown, Select, Row, Col, Drawer, Divider, Alert, Typography,
} from 'antd';
import {
  PlusOutlined, ReloadOutlined, SearchOutlined,
  ClearOutlined, MoreOutlined, EyeOutlined,
} from '@ant-design/icons';
import {
  listTenants, createTenant, resetAdminPassword,
  activateTenant, suspendTenant, restoreTenant, closeTenant, applyPlan,
  type PlatformTenant,
} from '../../api/platform';

const { Text } = Typography;

const STATUS_MAP: Record<string, { color: string; label: string }> = {
  TRIAL: { color: 'blue', label: '试用中' },
  ACTIVE: { color: 'green', label: '正式' },
  EXPIRED: { color: 'red', label: '已到期' },
  SUSPENDED: { color: 'orange', label: '已暂停' },
  CLOSED: { color: 'default', label: '已关闭' },
};

const PLAN_OPTIONS = [
  { value: 'TRIAL', label: '试用版' },
  { value: 'STARTER', label: '入门版' },
  { value: 'PRO', label: '专业版' },
  { value: 'LEGACY', label: '历史版' },
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
  const [createPlan, setCreatePlan] = useState('TRIAL');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerTenant, setDrawerTenant] = useState<PlatformTenant | null>(null);
  const [form] = Form.useForm();
  const { message, modal } = App.useApp();

  // 筛选状态
  const [keyword, setKeyword] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [filterPlan, setFilterPlan] = useState<string>('');

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

  // 前端过滤（后端暂不支持筛选参数）
  const filteredData = useMemo(() => {
    return data.filter((t) => {
      if (keyword.trim()) {
        const kw = keyword.trim().toLowerCase();
        const match =
          (t.name || '').toLowerCase().includes(kw) ||
          (t.tenantCode || '').toLowerCase().includes(kw) ||
          (t.contactName || '').toLowerCase().includes(kw) ||
          (t.contactPhone || '').toLowerCase().includes(kw);
        if (!match) return false;
      }
      if (filterStatus && t.status !== filterStatus) return false;
      if (filterPlan && t.planCode !== filterPlan) return false;
      return true;
    });
  }, [data, keyword, filterStatus, filterPlan]);

  const handleResetFilters = () => {
    setKeyword('');
    setFilterStatus('');
    setFilterPlan('');
  };

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

  // ============ 创建租户 ============

  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      const res = await createTenant(values);
      const tenant = res.tenant;
      // 如果选择了非 TRIAL 套餐，创建后再应用
      if (createPlan !== 'TRIAL') {
        try {
          await applyPlan(tenant.id, createPlan);
          message.success(`租户创建成功（${PLAN_OPTIONS.find(p => p.value === createPlan)?.label || createPlan}）`);
        } catch {
          message.warning('租户已创建，但应用套餐失败，当前为试用版');
        }
      } else {
        message.success('租户创建成功');
      }
      setCreateResult({
        tenantCode: tenant.tenantCode,
        adminUsername: res.adminUsername,
        adminPassword: res.adminPassword,
      });
      form.resetFields();
      setCreatePlan('TRIAL');
      fetchData();
    } catch { /* validation or API error */ }
  };

  const handleCreateCancel = () => {
    setCreateOpen(false);
    setCreateResult(null);
    setCreatePlan('TRIAL');
    form.resetFields();
  };

  // ============ 状态操作 ============

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
      title: '确认永久关闭租户？',
      content: '永久关闭后该租户将无法登录和使用服务，且当前版本不支持在界面恢复。如只是临时停用，请使用"暂停"。',
      okText: '确认永久关闭',
      okType: 'danger',
      onOk: () => withAction(record.id, () => closeTenant(record.id), '已永久关闭'),
    });
  };

  const handleApplyPlan = (record: PlatformTenant, planCode: string) => {
    const planLabel = PLAN_OPTIONS.find(p => p.value === planCode)?.label || planCode;
    modal.confirm({
      title: '确认应用套餐？',
      content: `应用「${planLabel}」将覆盖当前所有额度设置，确认继续？`,
      okText: '确认应用',
      onOk: () => withAction(record.id, () => applyPlan(record.id, planCode), '套餐已应用'),
    });
  };

  // ============ 表格列定义 ============

  const columns = [
    {
      title: '企业名称', dataIndex: 'name', key: 'name', width: 160,
      render: (v: string, record: PlatformTenant) => (
        <a style={{ color: '#2563eb', cursor: 'pointer' }} onClick={() => { setDrawerTenant(record); setDrawerOpen(true); }}>
          {v || '-'}
        </a>
      ),
    },
    { title: '租户编码', dataIndex: 'tenantCode', key: 'tenantCode', width: 100, render: (v: string) => <code>{v}</code> },
    { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 80, render: (v: string) => v || '-' },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80,
      render: (v: string) => {
        const m = STATUS_MAP[v];
        return m ? <Tag color={m.color}>{m.label}</Tag> : <Tag>{v || '-'}</Tag>;
      },
    },
    {
      title: '套餐', dataIndex: 'planName', key: 'planName', width: 80,
      render: (v: string, record: PlatformTenant) => {
        const name = v || record.planCode;
        return name ? <Tag color="purple">{name}</Tag> : '-';
      },
    },
    {
      title: '试用到期', dataIndex: 'trialEndAt', key: 'trialEndAt', width: 100,
      render: (v: string, record: PlatformTenant) =>
        record.status === 'TRIAL' && v ? (
          <Text style={{ fontSize: 13, color: '#f59e0b' }}>{formatDateShort(v)}</Text>
        ) : '-',
    },
    {
      title: '服务到期', dataIndex: 'expiredAt', key: 'expiredAt', width: 100,
      render: (v: string) => v ? formatDateShort(v) : '-',
    },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 140, render: (v: string) => formatDate(v) },
    {
      title: '操作', key: 'action', width: 140, fixed: 'right' as const,
      render: (_: unknown, record: PlatformTenant) => {
        const isLoading = actionLoading === record.id;
        const isClosed = record.status === 'CLOSED';
        const isSuspended = record.status === 'SUSPENDED';
        const isExpired = record.status === 'EXPIRED';
        const isActive = record.status === 'ACTIVE';
        const isTrial = record.status === 'TRIAL';

        // CLOSED 终态：只显示详情，不可做任何操作
        if (isClosed) {
          return (
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => { setDrawerTenant(record); setDrawerOpen(true); }}
            >
              详情
            </Button>
          );
        }

        const menuItems = [
          // 应用套餐
          {
            key: 'plan-group', type: 'group' as const, label: '应用套餐',
          },
          ...PLAN_OPTIONS.map(p => ({
            key: `plan-${p.value}`,
            label: p.label,
            onClick: () => handleApplyPlan(record, p.value),
          })),
          { key: 'div1', type: 'divider' as const },
          // 状态操作
          ...(isActive || isTrial ? [
            { key: 'suspend', label: <span style={{ color: '#f59e0b' }}>暂停</span>, onClick: () => handleSuspend(record) },
          ] : []),
          ...((isSuspended || isExpired) ? [
            { key: 'activate', label: <span style={{ color: '#10b981' }}>启用</span>, onClick: () => handleActivate(record) },
            { key: 'restore', label: '恢复', onClick: () => handleRestore(record) },
          ] : []),
          ...(!isActive && !isTrial && !isSuspended && !isExpired ? [
            { key: 'activate', label: <span style={{ color: '#10b981' }}>启用</span>, onClick: () => handleActivate(record) },
          ] : []),
          { key: 'div2', type: 'divider' as const },
          { key: 'close', label: <span style={{ color: '#ef4444' }}>永久关闭</span>, onClick: () => handleClose(record) },
          { key: 'div3', type: 'divider' as const },
          // 重置密码
          { key: 'reset-pwd', label: '重置密码', onClick: () => {
            resetAdminPassword(record.id).then(res => {
              Modal.info({
                title: '密码已重置',
                content: <div><p>新密码：<code>{res.newPassword}</code></p><p>请及时通知企业管理员修改密码。</p></div>,
              });
            });
          }},
        ];

        return (
          <Space size="small">
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => { setDrawerTenant(record); setDrawerOpen(true); }}
            >
              详情
            </Button>
            <Dropdown menu={{ items: menuItems }} disabled={isLoading} trigger={['click']}>
              <Button size="small" loading={isLoading} icon={<MoreOutlined />}>
                更多
              </Button>
            </Dropdown>
          </Space>
        );
      },
    },
  ];

  // ============ Drawer 字段行 ============

  const F = ({ label, children }: { label: string; children: React.ReactNode }) => (
    <div style={{ marginBottom: 12 }}>
      <Text style={{ color: '#94a3b8', fontSize: 12, display: 'block', marginBottom: 2 }}>{label}</Text>
      <div>{children}</div>
    </div>
  );

  const FT = ({ label, value, mono }: { label: string; value: string | null; mono?: boolean }) => (
    <F label={label}>
      <Text style={mono ? { fontFamily: 'monospace', fontSize: 13 } : { fontSize: 13 }}>
        {value || '-'}
      </Text>
    </F>
  );

  const renderDrawerContent = (t: PlatformTenant) => {
    const sm = STATUS_MAP[t.status];
    return (
      <div>
        {/* 基础信息 */}
        <Text strong style={{ fontSize: 14, color: '#0f172a' }}>基础信息</Text>
        <Divider style={{ margin: '8px 0 12px' }} />
        <Row gutter={[24, 0]}>
          <Col span={12}><FT label="企业名称" value={t.name} /></Col>
          <Col span={12}><FT label="租户编码" value={t.tenantCode} mono /></Col>
          <Col span={12}><FT label="联系人" value={t.contactName} /></Col>
          <Col span={12}><FT label="电话" value={t.contactPhone} /></Col>
          <Col span={24}><FT label="地址" value={t.address} /></Col>
        </Row>

        {/* 生命周期 */}
        <Text strong style={{ fontSize: 14, color: '#0f172a', display: 'block', marginTop: 16 }}>生命周期</Text>
        <Divider style={{ margin: '8px 0 12px' }} />
        <Row gutter={[24, 0]}>
          <Col span={12}>
            <F label="状态">{sm ? <Tag color={sm.color}>{sm.label}</Tag> : <Tag>{t.status}</Tag>}</F>
          </Col>
          <Col span={12}>
            <F label="套餐">{t.planName ? <Tag color="purple">{t.planName}</Tag> : t.planCode ? <Tag color="purple">{t.planCode}</Tag> : '-'}</F>
          </Col>
          <Col span={12}><FT label="试用到期" value={t.trialEndAt ? formatDate(t.trialEndAt) : null} /></Col>
          <Col span={12}><FT label="服务到期" value={t.expiredAt ? formatDate(t.expiredAt) : null} /></Col>
          <Col span={12}><FT label="创建时间" value={formatDate(t.createdAt)} /></Col>
          <Col span={12}><FT label="更新时间" value={formatDate(t.updatedAt)} /></Col>
        </Row>

        {/* 额度信息 */}
        <Text strong style={{ fontSize: 14, color: '#0f172a', display: 'block', marginTop: 16 }}>额度信息</Text>
        <Divider style={{ margin: '8px 0 12px' }} />
        <Row gutter={[24, 0]}>
          {([
            ['员工上限', t.maxUsers],
            ['师傅上限', t.maxTechnicians],
            ['知识库上限', t.maxKnowledgeBases],
            ['文档上限', t.maxDocuments],
            ['AI 日限额', t.maxAiDailyCalls],
            ['月工单限额', t.ticketMonthlyLimit],
          ] as [string, number | null][]).map(([label, val]) => (
            <Col span={12} key={label}>
              <F label={label}><Text style={{ fontSize: 13 }}>{formatNullable(val)}</Text></F>
            </Col>
          ))}
        </Row>

        {/* 门户信息 */}
        <Text strong style={{ fontSize: 14, color: '#0f172a', display: 'block', marginTop: 16 }}>门户信息</Text>
        <Divider style={{ margin: '8px 0 12px' }} />
        <Row gutter={[24, 0]}>
          <Col span={12}>
            <F label="门户开关">
              <Tag color={t.portalEnabled ? 'blue' : 'default'}>
                {t.portalEnabled ? '配置已启用' : '配置已关闭'}
              </Tag>
            </F>
          </Col>
          <Col span={12}>
            <F label="门户访问">
              {(() => {
                const st = t.status;
                if (st === 'CLOSED') return <Tag color="default">不可访问（租户已关闭）</Tag>;
                if (st === 'SUSPENDED') return <Tag color="orange">不可访问（租户已暂停）</Tag>;
                if (st === 'EXPIRED') return <Tag color="red">不可访问（租户已到期）</Tag>;
                if (!t.portalEnabled) return <Tag color="default">不可访问（门户开关已关闭）</Tag>;
                return <Tag color="green">可访问</Tag>;
              })()}
            </F>
          </Col>
          <Col span={12}><FT label="门户标题" value={t.portalTitle} /></Col>
          <Col span={24}><FT label="门户描述" value={t.portalDescription} /></Col>
        </Row>
      </div>
    );
  };

  // ============ 页面渲染 ============

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600, color: '#0f172a' }}>平台租户管理</h2>
          <p style={{ margin: '4px 0 0', color: '#64748b', fontSize: 14 }}>管理所有企业租户</p>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => fetchData(page)}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setCreateResult(null); setCreatePlan('TRIAL'); setCreateOpen(true); }}>
            创建租户
          </Button>
        </Space>
      </div>

      {/* 筛选区 */}
      <div style={{
        marginBottom: 16, padding: '12px 16px',
        background: '#f8fafc', borderRadius: 8, border: '1px solid #e2e8f0',
      }}>
        <Row gutter={[12, 8]} align="middle">
          <Col xs={24} sm={8}>
            <Input
              placeholder="搜索企业名称/编码/联系人/手机号"
              prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
              value={keyword}
              onChange={e => setKeyword(e.target.value)}
              allowClear
            />
          </Col>
          <Col xs={12} sm={5}>
            <Select
              placeholder="状态筛选"
              value={filterStatus || undefined}
              onChange={v => setFilterStatus(v || '')}
              allowClear
              style={{ width: '100%' }}
              options={[
                { value: 'TRIAL', label: '试用中' },
                { value: 'ACTIVE', label: '正式' },
                { value: 'EXPIRED', label: '已到期' },
                { value: 'SUSPENDED', label: '已暂停' },
                { value: 'CLOSED', label: '已关闭' },
              ]}
            />
          </Col>
          <Col xs={12} sm={5}>
            <Select
              placeholder="套餐筛选"
              value={filterPlan || undefined}
              onChange={v => setFilterPlan(v || '')}
              allowClear
              style={{ width: '100%' }}
              options={PLAN_OPTIONS}
            />
          </Col>
          <Col xs={24} sm={6}>
            <Button icon={<ClearOutlined />} onClick={handleResetFilters} disabled={!keyword && !filterStatus && !filterPlan}>
              重置
            </Button>
            <Text style={{ marginLeft: 8, fontSize: 12, color: '#94a3b8' }}>
              {(keyword || filterStatus || filterPlan) ? `筛选结果：${filteredData.length} 条` : ''}
            </Text>
          </Col>
        </Row>
      </div>

      <Table
        dataSource={filteredData}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{ current: page, total, pageSize: 20, onChange: fetchData }}
        size="middle"
        scroll={{ x: 1000 }}
      />

      {/* 详情 Drawer */}
      <Drawer
        title={drawerTenant ? `${drawerTenant.name || drawerTenant.tenantCode} — 租户详情` : '租户详情'}
        open={drawerOpen}
        onClose={() => { setDrawerOpen(false); setDrawerTenant(null); }}
        width={560}
        destroyOnHidden
      >
        {drawerTenant && renderDrawerContent(drawerTenant)}
      </Drawer>

      {/* 创建租户 Modal */}
      <Modal
        title={createResult ? '租户创建成功' : '创建租户'}
        open={createOpen}
        onCancel={handleCreateCancel}
        footer={createResult ? [
          <Button key="close" onClick={handleCreateCancel}>关闭</Button>,
        ] : [
          <Button key="cancel" onClick={handleCreateCancel}>取消</Button>,
          <Button key="ok" type="primary" onClick={handleCreate}>创建</Button>,
        ]}
        destroyOnHidden
      >
        {createResult ? (
          <div>
            <Alert
              type="success"
              message="租户创建成功"
              showIcon
              style={{ marginBottom: 16 }}
            />
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="租户编码">
                <code style={{ fontSize: 14, fontWeight: 600 }}>{createResult.tenantCode}</code>
              </Descriptions.Item>
              <Descriptions.Item label="管理员用户名">
                <code>{createResult.adminUsername}</code>
              </Descriptions.Item>
              <Descriptions.Item label="管理员密码">
                <code>{createResult.adminPassword}</code>
              </Descriptions.Item>
            </Descriptions>
            <p style={{ marginTop: 12, color: '#64748b', fontSize: 13 }}>
              请妥善保管密码，建议通知管理员首次登录后修改。
            </p>
          </div>
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
            <Form.Item label="套餐选择">
              <Select
                value={createPlan}
                onChange={v => setCreatePlan(v)}
                options={PLAN_OPTIONS}
                style={{ width: '100%' }}
              />
            </Form.Item>
          </Form>
        )}
      </Modal>
    </div>
  );
};

export default PlatformTenantsPage;
