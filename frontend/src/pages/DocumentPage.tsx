import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Select, Tag, Space, App, Popconfirm, Upload, Typography, Tooltip, Empty } from 'antd';
import { UploadOutlined, ReloadOutlined, DeleteOutlined, CheckCircleOutlined, ClockCircleOutlined, ExclamationCircleOutlined, CloudUploadOutlined, FileSearchOutlined, DatabaseOutlined, FileTextOutlined } from '@ant-design/icons';
import { listDocuments, uploadDocument, deleteDocument, reparseDocument, type KnowledgeDocument } from '../api/documents';
import { listKnowledgeBases, type KnowledgeBase } from '../api/knowledge';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';

const { Title, Text } = Typography;

const statusConfig: Record<string, { color: string; bg: string; border: string; icon: React.ReactNode; text: string }> = {
  PENDING: { color: '#2563eb', bg: '#eff6ff', border: '#bfdbfe', icon: <ClockCircleOutlined />, text: '解析中' },
  SUCCESS: { color: '#059669', bg: '#ecfdf5', border: '#a7f3d0', icon: <CheckCircleOutlined />, text: '成功' },
  FAILED: { color: '#dc2626', bg: '#fef2f2', border: '#fecaca', icon: <ExclamationCircleOutlined />, text: '失败' },
};

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1024 / 1024).toFixed(1) + ' MB';
};

const DocumentPage: React.FC = () => {
  const [data, setData] = useState<KnowledgeDocument[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size] = useState(20);
  const [kbIdFilter, setKbIdFilter] = useState<number | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [kbList, setKbList] = useState<KnowledgeBase[]>([]);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [uploadForm] = Form.useForm();
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [uploading, setUploading] = useState(false);
  const { message } = App.useApp();

  const fetchData = async (p = page, kbId = kbIdFilter, status = statusFilter) => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page: p, size };
      if (kbId) params.knowledgeBaseId = kbId;
      if (status) params.parseStatus = status;
      const res = await listDocuments(params as { page?: number; size?: number; knowledgeBaseId?: number; parseStatus?: string });
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

  const handleUpload = async () => {
    const values = await uploadForm.validateFields();
    if (fileList.length === 0) {
      message.warning('请选择文件');
      return;
    }
    setUploading(true);
    try {
      const result = await uploadDocument(values.knowledgeBaseId, fileList[0].originFileObj as File);
      if (result.parseStatus === 'SUCCESS') {
        message.success(`上传成功，解析生成 ${result.itemCount} 条知识`);
      } else if (result.parseStatus === 'FAILED') {
        message.warning(`上传成功但解析失败：${result.errorMessage}`);
      } else {
        message.success('上传成功');
      }
      setUploadOpen(false);
      setFileList([]);
      uploadForm.resetFields();
      fetchData(1);
    } catch {
      // handled
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteDocument(id);
      message.success('删除成功');
      fetchData();
    } catch {
      // handled
    }
  };

  const handleReparse = async (id: number) => {
    try {
      const result = await reparseDocument(id);
      if (result.parseStatus === 'SUCCESS') {
        message.success(`重解析完成，生成 ${result.itemCount} 条知识`);
      } else {
        message.warning(`重解析失败：${result.errorMessage}`);
      }
      fetchData();
    } catch {
      // handled
    }
  };

  const handleFilter = () => {
    setPage(1);
    fetchData(1);
  };

  const columns: ColumnsType<KnowledgeDocument> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    {
      title: '文件名', dataIndex: 'originalFilename', width: 200, ellipsis: true,
      render: (v: string) => <Text strong style={{ color: '#0f172a' }}>{v}</Text>,
    },
    {
      title: '大小', dataIndex: 'fileSize', width: 80,
      render: (v: number) => <Text style={{ color: '#64748b', fontSize: 13 }}>{formatFileSize(v)}</Text>,
    },
    {
      title: '解析状态', dataIndex: 'parseStatus', width: 100, align: 'center',
      render: (s: string) => {
        const cfg = statusConfig[s] || { color: '#64748b', bg: '#f1f5f9', border: '#e2e8f0', icon: null, text: s };
        return (
          <Tag
            icon={cfg.icon}
            style={{ color: cfg.color, background: cfg.bg, border: `1px solid ${cfg.border}`, borderRadius: 6, fontWeight: 500 }}
          >
            {cfg.text}
          </Tag>
        );
      },
    },
    {
      title: '条目数', dataIndex: 'itemCount', width: 80, align: 'center',
      render: (v: number) => <Text style={{ fontWeight: 600, color: v > 0 ? '#059669' : '#94a3b8' }}>{v}</Text>,
    },
    {
      title: '错误信息', dataIndex: 'errorMessage', width: 200, ellipsis: true,
      render: (v: string | null) => v ? <Tooltip title={v}><Text style={{ color: '#dc2626', fontSize: 13 }}>{v.substring(0, 40)}</Text></Tooltip> : <Text style={{ color: '#cbd5e1' }}>-</Text>,
    },
    { title: '上传时间', dataIndex: 'createdAt', width: 170, render: (v: string) => <Text style={{ color: '#94a3b8', fontSize: 13 }}>{v}</Text> },
    {
      title: '操作', width: 130,
      render: (_, record) => (
        <Space size={4}>
          <Tooltip title="重新解析">
            <Button type="text" size="small" icon={<ReloadOutlined />} onClick={() => handleReparse(record.id)} style={{ color: '#2563eb' }} />
          </Tooltip>
          <Popconfirm title="确认删除？关联条目将标记为停用" onConfirm={() => handleDelete(record.id)} okText="确认" cancelText="取消">
            <Tooltip title="删除">
              <Button type="text" size="small" icon={<DeleteOutlined />} style={{ color: '#dc2626' }} />
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
          <Title level={4} style={{ margin: 0, color: '#0f172a' }}>文档管理</Title>
          <Text style={{ color: '#64748b', fontSize: 13 }}>上传文档自动解析为知识条目，同步到向量检索库</Text>
        </div>
        <Button type="primary" icon={<CloudUploadOutlined />} onClick={() => { setUploadOpen(true); setFileList([]); uploadForm.resetFields(); }}
          style={{ borderRadius: 8, fontWeight: 500 }}>
          上传文档
        </Button>
      </div>

      {/* 流程说明 */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 0, marginBottom: 20, padding: '12px 20px',
        background: '#f8fafc', borderRadius: 8, border: '1px solid #e2e8f0',
      }}>
        {[
          { icon: <CloudUploadOutlined />, label: '上传文档' },
          { icon: <FileSearchOutlined />, label: '自动解析' },
          { icon: <FileTextOutlined />, label: '生成知识条目' },
          { icon: <DatabaseOutlined />, label: '同步向量库' },
        ].map((step, i, arr) => (
          <React.Fragment key={step.label}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ color: '#2563eb', fontSize: 14 }}>{step.icon}</span>
              <Text style={{ fontSize: 12, color: '#475569', fontWeight: 500 }}>{step.label}</Text>
            </div>
            {i < arr.length - 1 && (
              <div style={{ flex: 1, height: 1, background: '#cbd5e1', margin: '0 12px', minWidth: 20 }} />
            )}
          </React.Fragment>
        ))}
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
        <Select
          allowClear
          placeholder="解析状态"
          style={{ width: 130 }}
          value={statusFilter}
          onChange={(v) => setStatusFilter(v)}
          options={[
            { label: '解析中', value: 'PENDING' },
            { label: '成功', value: 'SUCCESS' },
            { label: '失败', value: 'FAILED' },
          ]}
        />
        <Button onClick={handleFilter} style={{ borderRadius: 6 }}>筛选</Button>
      </div>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        scroll={{ x: 1050 }}
        locale={{ emptyText: <Empty description="暂无文档，点击上方按钮上传" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
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
        title="上传文档"
        open={uploadOpen}
        onOk={handleUpload}
        onCancel={() => setUploadOpen(false)}
        confirmLoading={uploading}
        destroyOnHidden
        okText="上传"
        cancelText="取消"
      >
        <Form form={uploadForm} layout="vertical" preserve={false} style={{ marginTop: 16 }}>
          <Form.Item name="knowledgeBaseId" label="目标知识库" rules={[{ required: true, message: '请选择知识库' }]}>
            <Select
              placeholder="选择知识库"
              options={kbList.map((kb) => ({ label: kb.name, value: kb.id }))}
            />
          </Form.Item>
          <Form.Item label="选择文件" required>
            <Upload
              accept=".txt,.md"
              beforeUpload={() => false}
              fileList={fileList}
              onChange={({ fileList: fl }) => setFileList(fl.slice(-1))}
              maxCount={1}
            >
              <Button icon={<UploadOutlined />}>选择 .txt / .md 文件</Button>
            </Upload>
            <div style={{ color: '#94a3b8', fontSize: 12, marginTop: 6 }}>
              支持 .txt 和 .md 格式，最大 10MB。上传后自动按段落切分生成知识条目。
            </div>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default DocumentPage;
