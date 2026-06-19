import React, { useEffect, useState } from 'react';
import { Table, Button, Tag, Space, Modal, Form, Input, App, Popconfirm, Descriptions } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { listTenants, createTenant, enableTenant, disableTenant, resetAdminPassword, type PlatformTenant } from '../../api/platform';

const PlatformTenantsPage: React.FC = () => {
  const [data, setData] = useState<PlatformTenant[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [createResult, setCreateResult] = useState<{ tenantCode: string; adminUsername: string; adminPassword: string } | null>(null);
  const [form] = Form.useForm();
  const { message } = App.useApp();

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

  const handleToggle = async (record: PlatformTenant) => {
    if (record.status === 'ACTIVE') {
      await disableTenant(record.id);
      message.success('已禁用');
    } else {
      await enableTenant(record.id);
      message.success('已启用');
    }
    fetchData(page);
  };

  const handleResetPwd = async (id: number) => {
    const res = await resetAdminPassword(id);
    Modal.info({
      title: '密码已重置',
      content: <div><p>新密码：<code>{res.newPassword}</code></p><p>请及时通知企业管理员修改密码。</p></div>,
    });
  };

  const columns = [
    { title: '企业名称', dataIndex: 'name', key: 'name', render: (v: string) => v || '-' },
    { title: '租户编码', dataIndex: 'tenantCode', key: 'tenantCode', render: (v: string) => <code>{v}</code> },
    { title: '联系人', dataIndex: 'contactName', key: 'contactName', render: (v: string) => v || '-' },
    { title: '电话', dataIndex: 'contactPhone', key: 'contactPhone', render: (v: string) => v || '-' },
    {
      title: '状态', dataIndex: 'status', key: 'status',
      render: (v: string) => <Tag color={v === 'ACTIVE' ? 'green' : 'red'}>{v === 'ACTIVE' ? '正常' : '已禁用'}</Tag>,
    },
    {
      title: '门户', dataIndex: 'portalEnabled', key: 'portalEnabled',
      render: (v: boolean) => v ? <Tag color="blue">已启用</Tag> : <Tag>已关闭</Tag>,
    },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => v?.slice(0, 19)?.replace('T', ' ') || '-' },
    {
      title: '操作', key: 'action',
      render: (_: unknown, record: PlatformTenant) => (
        <Space size="small">
          <Button size="small" type={record.status === 'ACTIVE' ? 'default' : 'primary'} danger={record.status === 'ACTIVE'}
            onClick={() => handleToggle(record)}>
            {record.status === 'ACTIVE' ? '禁用' : '启用'}
          </Button>
          <Popconfirm title="确认重置该租户管理员密码？" onConfirm={() => handleResetPwd(record.id)}>
            <Button size="small">重置密码</Button>
          </Popconfirm>
        </Space>
      ),
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
