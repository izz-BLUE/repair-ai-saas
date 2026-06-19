import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, Select, Tag, Space, App, Popconfirm, Typography, Empty, Tooltip } from 'antd';
import { PlusOutlined, SyncOutlined, EditOutlined, StopOutlined, CheckCircleOutlined, FileTextOutlined, FormOutlined } from '@ant-design/icons';
import {
  listKnowledgeItems, createKnowledgeItem, updateKnowledgeItem,
  updateKnowledgeItemStatus, listKnowledgeBases, syncVectors,
  type KnowledgeItem, type KnowledgeBase,
} from '../api/knowledge';
import type { ColumnsType } from 'antd/es/table';

const { TextArea } = Input;
const { Title, Text } = Typography;

const KnowledgeItemPage: React.FC = () => {
  const [data, setData] = useState<KnowledgeItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [kbIdFilter, setKbIdFilter] = useState<number | undefined>(undefined);
  const [kbList, setKbList] = useState<KnowledgeBase[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<KnowledgeItem | null>(null);
  const [form] = Form.useForm();
  const [syncing, setSyncing] = useState(false);
  const { message } = App.useApp();

  const fetchData = async (p = page, kw = keyword, kbId = kbIdFilter) => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page: p, size };
      if (kw) params.keyword = kw;
      if (kbId) params.knowledgeBaseId = kbId;
      const res = await listKnowledgeItems(params as { page?: number; size?: number; keyword?: string; knowledgeBaseId?: number });
      setData(res.records || []);
      setTotal(res.total || 0);
    } catch {
      // handled
    } finally {
      setLoading(false);
    }
  };

  const fetchKbList = async () => {
    try {
      const res = await listKnowledgeBases({ page: 1, size: 100 });
      setKbList(res.records || []);
    } catch {
      // handled
    }
  };

  useEffect(() => { fetchData(1); fetchKbList(); }, []);

  const handleSearch = () => {
    setPage(1);
    fetchData(1);
  };

  const handleAdd = () => {
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: KnowledgeItem) => {
    setEditing(record);
    form.setFieldsValue(record);
    setModalOpen(true);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        await updateKnowledgeItem(editing.id, values);
        message.success('编辑成功');
      } else {
        await createKnowledgeItem(values);
        message.success('新增成功');
      }
      setModalOpen(false);
      fetchData();
    } catch {
      // handled
    }
  };

  const handleToggleStatus = async (record: KnowledgeItem) => {
    const newStatus = record.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await updateKnowledgeItemStatus(record.id, newStatus);
      message.success(`已${newStatus === 'ACTIVE' ? '启用' : '禁用'}`);
      fetchData();
    } catch {
      // handled
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      const res = await syncVectors();
      message.success(`同步完成：总数 ${res.total}，成功 ${res.synced}，失败 ${res.failed}`);
    } catch {
      // handled
    } finally {
      setSyncing(false);
    }
  };

  const columns: ColumnsType<KnowledgeItem> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    {
      title: '标题', dataIndex: 'title', width: 180, ellipsis: true,
      render: (v: string) => <Text strong style={{ color: '#0f172a' }}>{v}</Text>,
    },
    { title: '问题', dataIndex: 'question', width: 180, ellipsis: true, render: (v: string) => <Text style={{ color: '#64748b' }}>{v}</Text> },
    {
      title: '答案', dataIndex: 'answer', width: 200, ellipsis: true,
      render: (v: string) => <Text style={{ color: '#94a3b8', fontSize: 13 }}>{v?.substring(0, 60)}{v?.length > 60 ? '...' : ''}</Text>,
    },
    { title: '产品', dataIndex: 'productType', width: 80, render: (v: string | null) => v ? <Tag style={{ borderRadius: 4 }}>{v}</Tag> : <Text style={{ color: '#cbd5e1' }}>-</Text> },
    { title: '故障', dataIndex: 'faultType', width: 80, render: (v: string | null) => v ? <Tag style={{ borderRadius: 4 }}>{v}</Tag> : <Text style={{ color: '#cbd5e1' }}>-</Text> },
    {
      title: '状态', dataIndex: 'status', width: 90, align: 'center',
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
    {
      title: '来源', dataIndex: 'documentId', width: 80, align: 'center',
      render: (v: number | null) => v
        ? <Tag icon={<FileTextOutlined />} color="blue" style={{ borderRadius: 4 }}>文档</Tag>
        : <Tag icon={<FormOutlined />} style={{ borderRadius: 4, color: '#64748b', borderColor: '#e2e8f0' }}>手动</Tag>,
    },
    {
      title: '操作', width: 100,
      render: (_, record) => (
        <Space size={4}>
          <Tooltip title="编辑">
            <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} style={{ color: '#64748b' }} />
          </Tooltip>
          <Popconfirm
            title={`确认${record.status === 'ACTIVE' ? '禁用' : '启用'}此条目？`}
            onConfirm={() => handleToggleStatus(record)}
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
          <Title level={4} style={{ margin: 0, color: '#0f172a' }}>知识条目</Title>
          <Text style={{ color: '#64748b', fontSize: 13 }}>管理 FAQ 条目内容，支持手动创建和文档自动解析</Text>
        </div>
        <Space>
          <Button icon={<SyncOutlined />} loading={syncing} onClick={handleSync}
            style={{ borderRadius: 8 }}>
            同步向量
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}
            style={{ borderRadius: 8, fontWeight: 500 }}>
            新增条目
          </Button>
        </Space>
      </div>
      <div style={{
        display: 'flex', gap: 12, marginBottom: 16, padding: '12px 16px',
        background: '#f8fafc', borderRadius: 8, border: '1px solid #e2e8f0',
      }}>
        <Select
          allowClear
          placeholder="选择知识库"
          style={{ width: 200 }}
          value={kbIdFilter}
          onChange={(v) => setKbIdFilter(v)}
          options={kbList.map((kb) => ({ label: kb.name, value: kb.id }))}
        />
        <Input.Search
          placeholder="搜索标题、问题、答案"
          style={{ width: 280 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={handleSearch}
          allowClear
        />
      </div>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        scroll={{ x: 1100 }}
        locale={{ emptyText: <Empty description="暂无知识条目，点击上方按钮新增" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
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
        title={editing ? '编辑条目' : '新增条目'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        width={600}
        destroyOnHidden
        okText="保存"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" preserve={false} style={{ marginTop: 16 }}>
          <Form.Item name="knowledgeBaseId" label="所属知识库" rules={[{ required: true, message: '请选择知识库' }]}>
            <Select
              placeholder="选择知识库"
              options={kbList.map((kb) => ({ label: kb.name, value: kb.id }))}
            />
          </Form.Item>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="例如：空调不制冷" />
          </Form.Item>
          <Form.Item name="question" label="问题" rules={[{ required: true, message: '请输入问题' }]}>
            <Input placeholder="客户可能的提问方式" />
          </Form.Item>
          <Form.Item name="answer" label="答案" rules={[{ required: true, message: '请输入答案' }]}>
            <TextArea rows={4} placeholder="标准回复内容" />
          </Form.Item>
          <Space style={{ width: '100%' }}>
            <Form.Item name="productType" label="产品类型" style={{ width: 240 }}>
              <Input placeholder="如：空调" />
            </Form.Item>
            <Form.Item name="faultType" label="故障类型" style={{ width: 240 }}>
              <Input placeholder="如：不制冷" />
            </Form.Item>
          </Space>
          <Form.Item name="keywords" label="关键词">
            <Input placeholder="逗号分隔，如：空调,不制冷,制冷差" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default KnowledgeItemPage;
