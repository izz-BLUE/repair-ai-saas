import React, { useEffect, useState } from 'react';
import { Table, Modal, Tag, Space, Typography, List, Card } from 'antd';
import { RobotOutlined, UserOutlined } from '@ant-design/icons';
import { listConversations, getConversation, type AiConversation, type AiMessage } from '../api/ai';
import type { ColumnsType } from 'antd/es/table';

const { Title, Paragraph } = Typography;

const AiConversationPage: React.FC = () => {
  const [data, setData] = useState<AiConversation[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size] = useState(20);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [currentConv, setCurrentConv] = useState<AiConversation | null>(null);
  const [messages, setMessages] = useState<AiMessage[]>([]);

  const fetchData = async (p = page) => {
    setLoading(true);
    try {
      const res = await listConversations({ page: p, size });
      setData(res.records || []);
      setTotal(res.total || 0);
    } catch {
      // handled
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(1); }, []);

  const handleViewDetail = async (record: AiConversation) => {
    setCurrentConv(record);
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const res = await getConversation(record.id);
      setCurrentConv(res.conversation);
      setMessages(res.messages || []);
    } catch {
      // handled
    } finally {
      setDetailLoading(false);
    }
  };

  const columns: ColumnsType<AiConversation> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '客户姓名', dataIndex: 'customerName', width: 120, render: (v: string | null) => v || '-' },
    { title: '电话', dataIndex: 'customerPhone', width: 130, render: (v: string | null) => v || '-' },
    { title: '产品类型', dataIndex: 'productType', width: 100, render: (v: string | null) => v || '-' },
    { title: '故障类型', dataIndex: 'faultType', width: 100, render: (v: string | null) => v || '-' },
    { title: '最后消息', dataIndex: 'lastMessage', width: 250, ellipsis: true },
    { title: '消息数', dataIndex: 'messageCount', width: 80 },
    { title: '创建时间', dataIndex: 'createdAt', width: 180 },
    {
      title: '操作', width: 100,
      render: (_, record) => (
        <a onClick={() => handleViewDetail(record)}>查看详情</a>
      ),
    },
  ];

  return (
    <div>
      <Title level={4}>AI 对话记录</Title>
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
        title={`对话详情 #${currentConv?.id || ''}`}
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={700}
        destroyOnClose
      >
        {detailLoading ? (
          <div style={{ textAlign: 'center', padding: 40 }}>加载中...</div>
        ) : (
          <>
            {currentConv && (
              <Space style={{ marginBottom: 16 }} wrap>
                <Tag>客户: {currentConv.customerName || '匿名'}</Tag>
                <Tag>产品: {currentConv.productType || '-'}</Tag>
                <Tag>故障: {currentConv.faultType || '-'}</Tag>
                <Tag>消息数: {currentConv.messageCount}</Tag>
              </Space>
            )}
            <List
              dataSource={messages}
              renderItem={(msg) => (
                <List.Item style={{ border: 'none', padding: '8px 0' }}>
                  <Card
                    size="small"
                    style={{
                      width: '100%',
                      background: msg.role === 'user' ? '#e6f7ff' : '#f6ffed',
                    }}
                  >
                    <Space align="start">
                      {msg.role === 'user' ? <UserOutlined style={{ color: '#1890ff' }} /> : <RobotOutlined style={{ color: '#52c41a' }} />}
                      <div>
                        <div style={{ marginBottom: 4 }}>
                          <Tag color={msg.role === 'user' ? 'blue' : 'green'}>
                            {msg.role === 'user' ? '用户' : 'AI'}
                          </Tag>
                          {msg.model && <Tag>{msg.model}</Tag>}
                          {msg.matchedItemCount !== null && <Tag>匹配条目: {msg.matchedItemCount}</Tag>}
                          {msg.shouldCreateTicket && <Tag color="orange">建议报修</Tag>}
                        </div>
                        <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{msg.content}</Paragraph>
                        <div style={{ color: '#999', fontSize: 12, marginTop: 4 }}>{msg.createdAt}</div>
                      </div>
                    </Space>
                  </Card>
                </List.Item>
              )}
            />
          </>
        )}
      </Modal>
    </div>
  );
};

export default AiConversationPage;
