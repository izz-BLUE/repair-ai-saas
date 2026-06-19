import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, Select, Tag, Space, message, Popconfirm, Typography } from 'antd';
import { PlusOutlined, SyncOutlined } from '@ant-design/icons';
import {
  listKnowledgeItems, createKnowledgeItem, updateKnowledgeItem,
  updateKnowledgeItemStatus, listKnowledgeBases, syncVectors,
  type KnowledgeItem, type KnowledgeBase,
} from '../api/knowledge';
import type { ColumnsType } from 'antd/es/table';

const { TextArea } = Input;
const { Title } = Typography;

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
    { title: '标题', dataIndex: 'title', width: 200, ellipsis: true },
    { title: '问题', dataIndex: 'question', width: 200, ellipsis: true },
    {
      title: '答案', dataIndex: 'answer', width: 250, ellipsis: true,
      render: (v: string) => <span style={{ color: '#666' }}>{v?.substring(0, 80)}{v?.length > 80 ? '...' : ''}</span>,
    },
    { title: '产品类型', dataIndex: 'productType', width: 100, render: (v: string | null) => v || '-' },
    { title: '故障类型', dataIndex: 'faultType', width: 100, render: (v: string | null) => v || '-' },
    {
      title: '状态', dataIndex: 'status', width: 80,
      render: (s: string) => <Tag color={s === 'ACTIVE' ? 'green' : 'default'}>{s === 'ACTIVE' ? '启用' : '禁用'}</Tag>,
    },
    {
      title: '来源', dataIndex: 'documentId', width: 80,
      render: (v: number | null) => v ? <Tag color="blue">文档</Tag> : <Tag>手动</Tag>,
    },
    {
      title: '操作', width: 160,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm
            title={`确认${record.status === 'ACTIVE' ? '禁用' : '启用'}此条目？`}
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
        <Title level={4} style={{ margin: 0 }}>知识条目管理</Title>
        <Space>
          <Button icon={<SyncOutlined />} loading={syncing} onClick={handleSync}>同步向量</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增条目</Button>
        </Space>
      </div>
      <Space style={{ marginBottom: 16 }} wrap>
        <Select
          allowClear
          placeholder="选择知识库"
          style={{ width: 200 }}
          value={kbIdFilter}
          onChange={(v) => setKbIdFilter(v)}
          options={kbList.map((kb) => ({ label: kb.name, value: kb.id }))}
        />
        <Input.Search
          placeholder="搜索关键词"
          style={{ width: 250 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={handleSearch}
          allowClear
        />
      </Space>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        scroll={{ x: 1200 }}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => { setPage(p); fetchData(p); },
        }}
      />
      <Modal
        title={editing ? '编辑条目' : '新增条目'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
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
