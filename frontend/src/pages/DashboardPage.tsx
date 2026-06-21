import React, { useEffect, useState } from 'react';
import { Card, Typography, Row, Col, Statistic } from 'antd';
import {
  BookOutlined,
  FileTextOutlined,
  UploadOutlined,
  RobotOutlined,
  ToolOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { listKnowledgeBases, listKnowledgeItems } from '../api/knowledge';
import { listDocuments } from '../api/documents';
import { listConversations } from '../api/ai';
import { getDashboardStats, type DashboardStats } from '../api/tickets';

const { Title, Text, Paragraph } = Typography;

interface StatState {
  kbCount: number;
  itemCount: number;
  docCount: number;
  convCount: number;
  ticketCount: number;
}

const modules = [
  {
    title: '工单管理',
    desc: '管理客户报修工单，跟踪从派单到完成的全流程',
    icon: <ToolOutlined />,
    path: '/admin/tickets',
    color: '#0ea5e9',
    bg: '#f0f9ff',
    statKey: 'ticketCount' as keyof StatState,
    statLabel: '个工单',
  },
  {
    title: '知识库',
    desc: '管理 FAQ 知识库，按产品和故障分类组织知识内容',
    icon: <BookOutlined />,
    path: '/admin/knowledge-bases',
    color: '#2563eb',
    bg: '#eff6ff',
    statKey: 'kbCount' as keyof StatState,
    statLabel: '个知识库',
  },
  {
    title: '知识条目',
    desc: '维护 FAQ 条目，支持手动创建和文档自动解析',
    icon: <FileTextOutlined />,
    path: '/admin/knowledge-items',
    color: '#059669',
    bg: '#ecfdf5',
    statKey: 'itemCount' as keyof StatState,
    statLabel: '条知识',
  },
  {
    title: '文档管理',
    desc: '上传 txt/md 文档，自动解析生成知识条目并同步向量库',
    icon: <UploadOutlined />,
    path: '/admin/documents',
    color: '#d97706',
    bg: '#fffbeb',
    statKey: 'docCount' as keyof StatState,
    statLabel: '份文档',
  },
  {
    title: 'AI 对话',
    desc: '查看客户与 AI 的问答记录，分析常见问题',
    icon: <RobotOutlined />,
    path: '/admin/ai-conversations',
    color: '#7c3aed',
    bg: '#f5f3ff',
    statKey: 'convCount' as keyof StatState,
    statLabel: '次对话',
  },
];

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState<StatState>({ kbCount: 0, itemCount: 0, docCount: 0, convCount: 0, ticketCount: 0 });
  const [dashStats, setDashStats] = useState<DashboardStats | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        const [kb, items, docs, convs, dash] = await Promise.allSettled([
          listKnowledgeBases({ page: 1, size: 1 }),
          listKnowledgeItems({ page: 1, size: 1 }),
          listDocuments({ page: 1, size: 1 }),
          listConversations({ page: 1, size: 1 }),
          getDashboardStats(),
        ]);
        setStats({
          kbCount: kb.status === 'fulfilled' ? kb.value.total : 0,
          itemCount: items.status === 'fulfilled' ? items.value.total : 0,
          docCount: docs.status === 'fulfilled' ? docs.value.total : 0,
          convCount: convs.status === 'fulfilled' ? convs.value.total : 0,
          ticketCount: dash.status === 'fulfilled'
            ? (dash.value.todayNewTickets + dash.value.pendingTickets + dash.value.processingTickets + dash.value.completedTickets)
            : 0,
        });
        if (dash.status === 'fulfilled') {
          setDashStats(dash.value);
        }
      } catch {
        // ignore
      }
    };
    load();
  }, []);

  return (
    <div>
      <div style={{ marginBottom: 28 }}>
        <Title level={4} style={{ margin: 0, color: '#0f172a' }}>仪表盘</Title>
        <Text style={{ color: '#64748b' }}>维修团队管理概览</Text>
      </div>

      {/* 工单管道统计 */}
      {dashStats && (
        <Row gutter={16} style={{ marginBottom: 20 }}>
          {[
            { label: '今日新增', value: dashStats.todayNewTickets, color: '#2563eb', bg: '#eff6ff' },
            { label: '待处理', value: dashStats.pendingTickets, color: '#f59e0b', bg: '#fffbeb' },
            { label: '处理中', value: dashStats.processingTickets, color: '#8b5cf6', bg: '#f5f3ff' },
            { label: '已完成', value: dashStats.completedTickets, color: '#10b981', bg: '#ecfdf5' },
          ].map((item) => (
            <Col xs={12} sm={6} key={item.label}>
              <Card
                size="small"
                style={{ borderRadius: 10, border: '1px solid #e2e8f0' }}
                styles={{ body: { padding: '14px 18px' } }}
              >
                <Statistic
                  value={item.value}
                  suffix={<span style={{ fontSize: 12, color: '#94a3b8' }}>{item.label}</span>}
                  styles={{ content: { fontSize: 22, fontWeight: 700, lineHeight: 1, color: item.color } }}
                />
              </Card>
            </Col>
          ))}
        </Row>
      )}

      {/* 模块统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 28 }}>
        {modules.map((m) => (
          <Col xs={12} sm={m.statKey === 'ticketCount' ? 24 : 6} key={m.statKey}>
            <Card
              size="small"
              style={{ borderRadius: 10, border: '1px solid #e2e8f0' }}
              styles={{ body: { padding: '16px 20px' } }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{
                  width: 40, height: 40, borderRadius: 10, background: m.bg,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 18, color: m.color,
                }}>
                  {m.icon}
                </div>
                <div>
                  <Statistic
                    value={stats[m.statKey]}
                    suffix={<span style={{ fontSize: 13, color: '#94a3b8' }}>{m.statLabel}</span>}
                    styles={{ content: { fontSize: 24, fontWeight: 700, lineHeight: 1 } }}
                  />
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* 模块入口 */}
      <Row gutter={16}>
        {modules.map((m) => (
          <Col xs={24} sm={12} key={m.path}>
            <Card
              hoverable
              style={{
                borderRadius: 10, border: '1px solid #e2e8f0', marginBottom: 16,
                cursor: 'pointer',
              }}
              styles={{ body: { padding: '20px 24px' } }}
              onClick={() => navigate(m.path)}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                    <div style={{
                      width: 32, height: 32, borderRadius: 8, background: m.bg,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      color: m.color, fontSize: 16,
                    }}>
                      {m.icon}
                    </div>
                    <Text style={{ fontSize: 15, fontWeight: 600, color: '#0f172a' }}>{m.title}</Text>
                  </div>
                  <Paragraph style={{ color: '#64748b', fontSize: 13, margin: 0, lineHeight: 1.6 }}>
                    {m.desc}
                  </Paragraph>
                </div>
                <ArrowRightOutlined style={{ color: '#94a3b8', fontSize: 14, marginTop: 4 }} />
              </div>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
};

export default DashboardPage;
