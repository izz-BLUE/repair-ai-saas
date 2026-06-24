import React, { useEffect, useState } from 'react';
import { Card, Typography, Row, Col, Statistic, Alert, Progress, Tag, Space } from 'antd';
import {
  BookOutlined,
  FileTextOutlined,
  UploadOutlined,
  RobotOutlined,
  ToolOutlined,
  ArrowRightOutlined,
  CrownOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { listKnowledgeBases, listKnowledgeItems } from '../api/knowledge';
import { listDocuments } from '../api/documents';
import { listConversations } from '../api/ai';
import { getDashboardStats, type DashboardStats } from '../api/tickets';
import { getUsage, type UsageData } from '../api/settings';

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

const STATUS_TAG_MAP: Record<string, { color: string; label: string }> = {
  TRIAL: { color: 'blue', label: '试用中' },
  ACTIVE: { color: 'green', label: '正式' },
  EXPIRED: { color: 'red', label: '已到期' },
  SUSPENDED: { color: 'orange', label: '已暂停' },
  CLOSED: { color: 'default', label: '已关闭' },
};

function calcProgress(current: number, max: number | null): { percent: number; color: string } {
  if (max == null || max === 0) {
    return { percent: 0, color: '#2563eb' };
  }
  const pct = Math.round((current / max) * 100);
  if (pct >= 100) return { percent: 100, color: '#ef4444' };
  if (pct >= 80) return { percent: pct, color: '#f59e0b' };
  return { percent: pct, color: '#2563eb' };
}

function formatMax(max: number | null): string {
  if (max == null) return '不限';
  return String(max);
}

function formatDate(d: string | null): string {
  if (!d) return '';
  return d.slice(0, 10);
}

interface UsageRowProps {
  label: string;
  current: number;
  max: number | null;
}

const UsageRow: React.FC<UsageRowProps> = ({ label, current, max }) => {
  const { percent, color } = calcProgress(current, max);
  return (
    <div style={{ marginBottom: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
        <Text style={{ fontSize: 13, color: '#475569' }}>{label}</Text>
        <Text style={{ fontSize: 13, color: '#1e293b', fontWeight: 500 }}>
          {current} / {formatMax(max)}
        </Text>
      </div>
      {max != null && max > 0 ? (
        <Progress percent={percent} strokeColor={color} showInfo={false} size="small" />
      ) : (
        <Progress percent={0} strokeColor="#e2e8f0" showInfo={false} size="small" />
      )}
    </div>
  );
};

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState<StatState>({ kbCount: 0, itemCount: 0, docCount: 0, convCount: 0, ticketCount: 0 });
  const [dashStats, setDashStats] = useState<DashboardStats | null>(null);
  const [usageData, setUsageData] = useState<UsageData | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        const [kb, items, docs, convs, dash, usage] = await Promise.allSettled([
          listKnowledgeBases({ page: 1, size: 1 }),
          listKnowledgeItems({ page: 1, size: 1 }),
          listDocuments({ page: 1, size: 1 }),
          listConversations({ page: 1, size: 1 }),
          getDashboardStats(),
          getUsage(),
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
        if (usage.status === 'fulfilled') {
          setUsageData(usage.value);
        }
      } catch {
        // ignore
      }
    };
    load();
  }, []);

  const renderStatusAlert = () => {
    if (!usageData) return null;
    const { status, daysUntilTrialEnd } = usageData;

    if (status === 'EXPIRED') {
      return <Alert type="error" message="服务已到期" description="您的服务已到期，请联系平台续费恢复使用。" showIcon style={{ marginBottom: 20 }} />;
    }
    if (status === 'SUSPENDED') {
      return <Alert type="error" message="服务已被暂停" description="您的服务已被平台暂停，请联系平台处理。" showIcon style={{ marginBottom: 20 }} />;
    }
    if (status === 'CLOSED') {
      return <Alert type="error" message="服务已关闭" description="该服务已停止。" showIcon style={{ marginBottom: 20, backgroundColor: '#f9fafb', borderColor: '#d1d5db' }} />;
    }
    if (status === 'TRIAL' && daysUntilTrialEnd != null && daysUntilTrialEnd <= 3) {
      return <Alert type="warning" message={`试用即将到期，剩余 ${daysUntilTrialEnd} 天`} description="请尽快联系平台升级套餐，以免服务中断。" showIcon style={{ marginBottom: 20 }} />;
    }
    return null;
  };

  const statusTag = usageData ? STATUS_TAG_MAP[usageData.status] : null;

  return (
    <div>
      <div style={{ marginBottom: 28 }}>
        <Title level={4} style={{ margin: 0, color: '#0f172a' }}>仪表盘</Title>
        <Text style={{ color: '#64748b' }}>维修团队管理概览</Text>
      </div>

      {/* 状态提醒 */}
      {renderStatusAlert()}

      {/* 套餐用量卡片 */}
      {usageData && (
        <Card
          style={{ borderRadius: 10, border: '1px solid #e2e8f0', marginBottom: 20 }}
          styles={{ body: { padding: '20px 24px' } }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16 }}>
            <CrownOutlined style={{ color: '#f59e0b', fontSize: 18 }} />
            <Text style={{ fontSize: 15, fontWeight: 600, color: '#0f172a' }}>套餐用量</Text>
            {statusTag && <Tag color={statusTag.color}>{statusTag.label}</Tag>}
            {usageData.planName && (
              <Tag color="purple">{usageData.planName}</Tag>
            )}
          </div>

          {/* 到期时间 */}
          <Space size="large" style={{ marginBottom: 16 }}>
            {usageData.trialEndAt && (
              <Text style={{ fontSize: 13, color: '#64748b' }}>
                试用到期：{formatDate(usageData.trialEndAt)}
                {usageData.daysUntilTrialEnd != null && usageData.daysUntilTrialEnd >= 0 && (
                  <span style={{ color: usageData.daysUntilTrialEnd <= 3 ? '#ef4444' : '#f59e0b', marginLeft: 4 }}>
                    （剩余 {usageData.daysUntilTrialEnd} 天）
                  </span>
                )}
              </Text>
            )}
            {usageData.expiredAt && (
              <Text style={{ fontSize: 13, color: '#64748b' }}>
                服务到期：{formatDate(usageData.expiredAt)}
                {usageData.daysUntilExpiry != null && usageData.daysUntilExpiry >= 0 && (
                  <span style={{ color: usageData.daysUntilExpiry <= 7 ? '#ef4444' : '#64748b', marginLeft: 4 }}>
                    （剩余 {usageData.daysUntilExpiry} 天）
                  </span>
                )}
              </Text>
            )}
          </Space>

          <Row gutter={[24, 0]}>
            <Col xs={24} sm={12}>
              <UsageRow label="员工数" current={usageData.usage.currentUsers} max={usageData.limits.maxUsers} />
              <UsageRow label="师傅数" current={usageData.usage.currentTechnicians} max={usageData.limits.maxTechnicians} />
              <UsageRow label="知识库数" current={usageData.usage.currentKnowledgeBases} max={usageData.limits.maxKnowledgeBases} />
            </Col>
            <Col xs={24} sm={12}>
              <UsageRow label="文档数" current={usageData.usage.currentDocuments} max={usageData.limits.maxDocuments} />
              <UsageRow label="今日 AI 调用" current={usageData.usage.todayAiCalls} max={usageData.limits.maxAiDailyCalls} />
              <UsageRow label="本月工单" current={usageData.usage.monthlyTickets} max={usageData.limits.ticketMonthlyLimit} />
            </Col>
          </Row>
        </Card>
      )}

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
