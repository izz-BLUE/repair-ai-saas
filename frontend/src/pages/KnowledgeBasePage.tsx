import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, Tag, Space, message, Popconfirm, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { listKnowledgeBases, createKnowledgeBase, updateKnowledgeBase, updateKnowledgeBaseStatus, type KnowledgeBase } from '../api/knowledge';
import type { ColumnsType } from 'antd/es/table';

const { TextArea } = Input;
const { Title } = Typography;

const KnowledgeBasePage: React.FC = () => {
  const [data, setData] = useState<KnowledgeBase[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size] = useState(20);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<KnowledgeBase | null>(null);
  const [form] = Form.useForm();

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
    { title: '名称', dataIndex: 'name', width: 200 },
    { title: '描述', dataIndex: 'description', ellipsis: true },
    {
      title: '状态', dataIndex: 'status', width: 100,
      render: (s: string) => <Tag color={s === 'ACTIVE' ? 'green' : 'default'}>{s === 'ACTIVE' ? '启用' : '禁用'}</Tag>,
    },
    { title: '创建时间', dataIndex: 'createdAt', width: 180 },
    {
      title: '操作', width: 220,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm
            title={`确认${record.status === 'ACTIVE' ? '禁用' : '启用'}此知识库？`}
            onConfirm={() => handleToggleStatus(record)}
          >
            <Button size="small" type={record.status === 'ACTIVE' ? 'default' : 'primary'} danger={record.status === 'ACTIVE'}>
              {record.status === 'ACTIVE' ? '禁用' : '启用'}
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>知识库管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增知识库</Button>
      </div>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => { setPage(p); fetchData(p); },
        }}
      />
      <Modal
        title={editing ? '编辑知识库' : '新增知识库'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="请输入知识库名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} placeholder="请输入描述（可选）" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default KnowledgeBasePage;
