import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Select, Tag, Space, message, Popconfirm, Upload, Typography, Tooltip } from 'antd';
import { UploadOutlined, ReloadOutlined, DeleteOutlined } from '@ant-design/icons';
import { listDocuments, uploadDocument, deleteDocument, reparseDocument, type KnowledgeDocument } from '../api/documents';
import { listKnowledgeBases, type KnowledgeBase } from '../api/knowledge';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';

const { Title, Text } = Typography;

const statusMap: Record<string, { color: string; text: string }> = {
  PENDING: { color: 'processing', text: '解析中' },
  SUCCESS: { color: 'success', text: '成功' },
  FAILED: { color: 'error', text: '失败' },
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
      await uploadDocument(values.knowledgeBaseId, fileList[0].originFileObj as File);
      message.success('上传成功');
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
      await reparseDocument(id);
      message.success('重解析完成');
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
    { title: '文件名', dataIndex: 'originalFilename', width: 200, ellipsis: true },
    {
      title: '文件大小', dataIndex: 'fileSize', width: 100,
      render: (v: number) => formatFileSize(v),
    },
    {
      title: '解析状态', dataIndex: 'parseStatus', width: 100,
      render: (s: string) => {
        const st = statusMap[s] || { color: 'default', text: s };
        return <Tag color={st.color}>{st.text}</Tag>;
      },
    },
    { title: '条目数', dataIndex: 'itemCount', width: 80 },
    {
      title: '错误信息', dataIndex: 'errorMessage', width: 200, ellipsis: true,
      render: (v: string | null) => v ? <Tooltip title={v}><Text type="danger">{v.substring(0, 50)}</Text></Tooltip> : '-',
    },
    { title: '上传时间', dataIndex: 'createdAt', width: 180 },
    {
      title: '操作', width: 200,
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<ReloadOutlined />} onClick={() => handleReparse(record.id)}>重解析</Button>
          <Popconfirm title="确认删除此文档？关联条目将标记为停用" onConfirm={() => handleDelete(record.id)}>
            <Button size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>文档管理</Title>
        <Button type="primary" icon={<UploadOutlined />} onClick={() => { setUploadOpen(true); setFileList([]); uploadForm.resetFields(); }}>上传文档</Button>
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
        <Button onClick={handleFilter}>筛选</Button>
      </Space>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        scroll={{ x: 1100 }}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => { setPage(p); fetchData(p); },
        }}
      />
      <Modal
        title="上传文档"
        open={uploadOpen}
        onOk={handleUpload}
        onCancel={() => setUploadOpen(false)}
        confirmLoading={uploading}
        destroyOnClose
      >
        <Form form={uploadForm} layout="vertical" preserve={false}>
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
            <div style={{ color: '#999', fontSize: 12, marginTop: 4 }}>支持 .txt 和 .md 格式，最大 10MB</div>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default DocumentPage;
