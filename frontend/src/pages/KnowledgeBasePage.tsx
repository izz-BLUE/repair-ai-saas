import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, Tag, Space, App, Popconfirm, Typography, Empty, Tooltip } from 'antd';
import { PlusOutlined, EditOutlined, StopOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { listKnowledgeBases, createKnowledgeBase, updateKnowledgeBase, updateKnowledgeBaseStatus, type KnowledgeBase } from '../api/knowledge';
import type { ColumnsType } from 'antd/es/table';

const { TextArea } = Input;
const { Title, Text } = Typography;

const KnowledgeBasePage: React.FC = () => {
  const [data, setData] = useState<KnowledgeBase[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size] = useState(20);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<KnowledgeBase | null>(null);
  const [form] = Form.useForm();
  const { message } = App.useApp();

  const fetchData = async (p = page) => {
    setLoading(true);
    try {
      const res = await listKnowledgeBases({ page: p, size });
      setData(res.records || []);
      setTotal(res.total || 0);
    } catch {
      // handled by http interceptor
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(1); }, []);

  const handleAdd = () => {
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: KnowledgeBase) => {
    setEditing(record);
    form.setFieldsValue({ name: record.name, description: record.description });
    setModalOpen(true);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        await updateKnowledgeBase(editing.id, values);
        message.success('编辑成功');
      } else {
        await createKnowledgeBase(values);
        message.success('新增成功');
      }
      setModalOpen(false);
      fetchData();
    } catch {
      // handled
    }
  };

  const handleToggleStatus = async (record: KnowledgeBase) => {
    const newStatus = record.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await updateKnowledgeBaseStatus(record.id, newStatus);
      message.success(`已${newStatus === 'ACTIVE' ? '启用' : '禁用'}`);
      fetchData();
    } catch {
      // handled
    }
  };

  const columns: ColumnsType<KnowledgeBase> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    {
      title: '名称', dataIndex: 'name', width: 200,
      render: (v: string) => <Text strong style={{ color: '#0f172a' }}>{v}</Text>,
    },
    { title: '描述', dataIndex: 'description', ellipsis: true, render: (v: string) => <Text style={{ color: '#64748b' }}>{v || '-'}</Text> },
    {
      title: '状态', dataIndex: 'status', width: 100, align: 'center',
      render: (s: string) => (
        <Tag
          color={s === 'ACTIVE' ? '#ecfdf5' : '#f1f5f9'}
          style={{
            color: s === 'ACTIVE' ? '#059669' : '#64748b',
            border: s === 'ACTIVE' ? '1px solid #a7f3d0' : '1px solid #e2e8f0',
            borderRadius: 6,
            fontWeight: 500,
          }}
          icon={s === 'ACTIVE' ? <CheckCircleOutlined /> : <StopOutlined />}
        >
          {s === 'ACTIVE' ? '启用' : '禁用'}
        </Tag>
      ),
    },
    { title: '创建时间', dataIndex: 'createdAt', width: 180, render: (v: string) => <Text style={{ color: '#94a3b8', fontSize: 13 }}>{v}</Text> },
    {
      title: '操作', width: 160,
      render: (_, record) => (
        <Space size={4}>
          <Tooltip title="编辑">
            <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} style={{ color: '#64748b' }} />
          </Tooltip>
          <Popconfirm
            title={`确认${record.status === 'ACTIVE' ? '禁用' : '启用'}此知识库？`}
            onConfirm={() => handleToggleStatus(record)}
            okText="确认"
            cancelText="取消"
          >
            <Tooltip title={record.status === 'ACTIVE' ? '禁用' : '启用'}>
              <Button
                type="text"
                size="small"
                icon={record.status === 'ACTIVE' ? <StopOutlined /> : <CheckCircleOutlined />}
                style={{ color: record.status === 'ACTIVE' ? '#d97706' : '#059669' }}
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          <Title level={4} style={{ margin: 0, color: '#0f172a' }}>知识库</Title>
          <Text style={{ color: '#64748b', fontSize: 13 }}>管理 FAQ 知识库，按产品线和故障类型组织知识</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}
          style={{ borderRadius: 8, fontWeight: 500 }}>
          新增知识库
        </Button>
      </div>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        locale={{ emptyText: <Empty description="暂无知识库，点击上方按钮新增" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => { setPage(p); fetchData(p); },
          size: 'small',
        }}
      />
      <Modal
        title={editing ? '编辑知识库' : '新增知识库'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
        okText="保存"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" preserve={false} style={{ marginTop: 16 }}>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入知识库名称' }]}>
            <Input placeholder="例如：空调维修 FAQ" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} placeholder="简要说明知识库的范围和用途（可选）" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default KnowledgeBasePage;
