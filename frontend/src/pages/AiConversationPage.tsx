import React, { useEffect, useState } from 'react';
import { Table, Modal, Tag, Space, Typography, Empty, Tooltip } from 'antd';
import { RobotOutlined, UserOutlined, MessageOutlined, LinkOutlined } from '@ant-design/icons';
import { listConversations, getConversation, type AiConversation, type AiMessage } from '../api/ai';
import type { ColumnsType } from 'antd/es/table';

const { Title, Text, Paragraph } = Typography;

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
    {
      title: '客户', dataIndex: 'customerName', width: 100,
      render: (v: string | null) => v || <Text style={{ color: '#cbd5e1' }}>匿名</Text>,
    },
    { title: '电话', dataIndex: 'customerPhone', width: 120, render: (v: string | null) => v || <Text style={{ color: '#cbd5e1' }}>-</Text> },
    {
      title: '问题', dataIndex: 'question', width: 220, ellipsis: true,
      render: (v: string) => <Text style={{ color: '#0f172a' }}>{v}</Text>,
    },
    {
      title: '来源', dataIndex: 'source', width: 100,
      render: (v: string) => <Tag style={{ borderRadius: 4, fontSize: 12 }}>{v === 'PUBLIC_CHAT' ? 'AI 客服' : v}</Tag>,
    },
    {
      title: '匹配', dataIndex: 'matchedItemCount', width: 70, align: 'center',
      render: (v: number | null) => <Text style={{ fontWeight: 600, color: v ? '#059669' : '#94a3b8' }}>{v ?? 0}</Text>,
    },
    {
      title: '模型', dataIndex: 'model', width: 100,
      render: (v: string | null) => <Tag style={{ borderRadius: 4, fontSize: 11 }}>{v || '-'}</Tag>,
    },
    { title: '时间', dataIndex: 'createdAt', width: 170, render: (v: string) => <Text style={{ color: '#94a3b8', fontSize: 13 }}>{v}</Text> },
    {
      title: '操作', width: 80, align: 'center',
      render: (_, record) => (
        <Tooltip title="查看对话详情">
          <a onClick={() => handleViewDetail(record)} style={{ color: '#2563eb', fontSize: 13 }}>
            <MessageOutlined style={{ marginRight: 4 }} />详情
          </a>
        </Tooltip>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 20 }}>
        <Title level={4} style={{ margin: 0, color: '#0f172a' }}>AI 对话</Title>
        <Text style={{ color: '#64748b', fontSize: 13 }}>查看客户与 AI 助手的问答记录，分析常见问题</Text>
      </div>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        scroll={{ x: 1000 }}
        locale={{ emptyText: <Empty description="暂无 AI 对话记录" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
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
        title={
          <Space>
            <MessageOutlined style={{ color: '#2563eb' }} />
            <span>对话 #{currentConv?.id || ''}</span>
          </Space>
        }
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={680}
        destroyOnHidden
      >
        {detailLoading ? (
          <div style={{ textAlign: 'center', padding: 60, color: '#94a3b8' }}>加载中...</div>
        ) : (
          <>
            {currentConv && (
              <div style={{
                display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 20,
                padding: '12px 16px', background: '#f8fafc', borderRadius: 8, border: '1px solid #e2e8f0',
              }}>
                <Tag style={{ borderRadius: 4 }}>客户: {currentConv.customerName || '匿名'}</Tag>
                {currentConv.customerPhone && <Tag style={{ borderRadius: 4 }}>电话: {currentConv.customerPhone}</Tag>}
                <Tag style={{ borderRadius: 4 }}>来源: {currentConv.source === 'PUBLIC_CHAT' ? 'AI 客服' : currentConv.source}</Tag>
                <Tag style={{ borderRadius: 4 }}>匹配: {currentConv.matchedItemCount ?? 0} 条</Tag>
                <Tag style={{ borderRadius: 4 }}>模型: {currentConv.model || '-'}</Tag>
                {currentConv.traceId && (
                  <Tooltip title={currentConv.traceId}>
                    <Tag icon={<LinkOutlined />} style={{ borderRadius: 4, cursor: 'pointer' }}>traceId</Tag>
                  </Tooltip>
                )}
              </div>
            )}

            {/* 原始问题 */}
            {currentConv?.question && (
              <div style={{ marginBottom: 16 }}>
                <Text style={{ fontSize: 12, color: '#94a3b8', fontWeight: 500 }}>用户问题</Text>
                <div style={{ marginTop: 4, padding: '8px 12px', background: '#eff6ff', borderRadius: 8, border: '1px solid #bfdbfe' }}>
                  <Text style={{ color: '#1e40af' }}>{currentConv.question}</Text>
                </div>
              </div>
            )}

            {/* 消息列表 */}
            <div>
              {messages.map((msg) => {
                const isUser = msg.role?.toUpperCase() === 'USER';
                return (
                  <div key={msg.id} style={{ padding: '6px 0' }}>
                    <div style={{
                      width: '100%',
                      display: 'flex',
                      flexDirection: isUser ? 'row-reverse' : 'row',
                      gap: 10,
                      alignItems: 'flex-start',
                    }}>
                      <div style={{
                        width: 32, height: 32, borderRadius: '50%', flexShrink: 0,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        background: isUser ? '#2563eb' : '#059669',
                        color: '#fff', fontSize: 14,
                      }}>
                        {isUser ? <UserOutlined /> : <RobotOutlined />}
                      </div>
                      <div style={{
                        maxWidth: '80%',
                        padding: '10px 14px',
                        borderRadius: isUser ? '12px 12px 2px 12px' : '12px 12px 12px 2px',
                        background: isUser ? '#2563eb' : '#f1f5f9',
                        color: isUser ? '#fff' : '#0f172a',
                        border: isUser ? 'none' : '1px solid #e2e8f0',
                      }}>
                        <div style={{ marginBottom: 4 }}>
                          <Text style={{ fontSize: 11, color: isUser ? 'rgba(255,255,255,0.7)' : '#94a3b8', fontWeight: 500 }}>
                            {isUser ? '用户' : 'AI 助手'}
                          </Text>
                        </div>
                        <Paragraph style={{
                          margin: 0, whiteSpace: 'pre-wrap', fontSize: 13, lineHeight: 1.7,
                          color: isUser ? '#fff' : '#334155',
                        }}>
                          {msg.content}
                        </Paragraph>
                        <div style={{ fontSize: 11, color: isUser ? 'rgba(255,255,255,0.5)' : '#94a3b8', marginTop: 6, textAlign: 'right' }}>
                          {msg.createdAt}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )}
      </Modal>
    </div>
  );
};

export default AiConversationPage;
