import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Typography, Tag, Button, Spin, Empty, App, Modal, Form, Input, Timeline,
} from 'antd';
import {
  EnvironmentOutlined,
  PhoneOutlined,
  UserOutlined,
  CheckCircleOutlined,
  PlayCircleOutlined,
  CopyOutlined,
} from '@ant-design/icons';
import { getMyTicket, startProcess, completeTicket } from '../../api/technician';
import type { Ticket, TicketStatusLog } from '../../api/tickets';

const { Text, Title } = Typography;
const { TextArea } = Input;

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待处理', color: '#d97706' },
  ASSIGNED: { label: '已派单', color: '#2563eb' },
  IN_PROGRESS: { label: '处理中', color: '#7c3aed' },
  COMPLETED: { label: '已完成', color: '#059669' },
  FOLLOWED_UP: { label: '已回访', color: '#0891b2' },
  CLOSED: { label: '已关闭', color: '#6b7280' },
  CANCELLED: { label: '已取消', color: '#dc2626' },
};

const STATUS_TIMELINE_MAP: Record<string, string> = {
  PENDING: '工单创建',
  ASSIGNED: '已派单',
  IN_PROGRESS: '开始处理',
  COMPLETED: '维修完成',
  FOLLOWED_UP: '已回访',
  CLOSED: '已关闭',
  CANCELLED: '已取消',
};

const TechnicianTicketDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { message } = App.useApp();
  const [form] = Form.useForm();

  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [statusLogs, setStatusLogs] = useState<TicketStatusLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [completeOpen, setCompleteOpen] = useState(false);

  const fetchDetail = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const result = await getMyTicket(Number(id));
      setTicket(result.ticket);
      setStatusLogs(result.statusLogs || []);
    } catch {
      message.error('加载工单详情失败');
    } finally {
      setLoading(false);
    }
  }, [id, message]);

  useEffect(() => {
    fetchDetail();
  }, [fetchDetail]);

  const handleStart = async () => {
    if (!id) return;
    setActionLoading(true);
    try {
      await startProcess(Number(id));
      message.success('已开始处理，工单状态更新为"处理中"');
      fetchDetail();
    } catch {
      // 错误已由拦截器显示
    } finally {
      setActionLoading(false);
    }
  };

  const handleComplete = async () => {
    if (!id) return;
    try {
      const values = await form.validateFields();
      setActionLoading(true);
      await completeTicket(Number(id), {
        repairResult: values.repairResult,
        costNote: values.costNote || undefined,
        partsNote: values.partsNote || undefined,
        remark: values.remark || undefined,
      });
      setCompleteOpen(false);
      form.resetFields();
      message.success('工单已完成，维修结果已记录');
      fetchDetail();
    } catch {
      // validateFields 失败不 toast，complete 错误由拦截器显示
    } finally {
      setActionLoading(false);
    }
  };

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text).then(() => {
      message.success('已复制');
    }).catch(() => {
      message.error('复制失败');
    });
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 120 }}>
        <Spin size="large" />
        <div style={{ marginTop: 12, color: '#94a3b8', fontSize: 13 }}>加载工单详情...</div>
      </div>
    );
  }

  if (!ticket) {
    return (
      <div style={{ textAlign: 'center', padding: 80 }}>
        <Empty description="工单不存在" />
        <Button onClick={() => navigate('/technician/tickets')} style={{ marginTop: 16 }}>
          返回列表
        </Button>
      </div>
    );
  }

  const statusInfo = STATUS_MAP[ticket.status] || { label: ticket.status, color: '#6b7280' };
  const isReadonly = ['COMPLETED', 'FOLLOWED_UP', 'CLOSED', 'CANCELLED'].includes(ticket.status);

  return (
    <div style={{ maxWidth: 480, margin: '0 auto', paddingBottom: 24 }}>
      {/* 状态横幅 */}
      <div style={{
        background: '#fff',
        borderRadius: 12,
        padding: '20px 16px',
        marginBottom: 12,
        border: '1px solid #e2e8f0',
        textAlign: 'center',
      }}>
        <div style={{
          display: 'inline-block',
          background: `${statusInfo.color}14`,
          borderRadius: 20,
          padding: '4px 16px',
          marginBottom: 12,
        }}>
          <Text style={{ fontSize: 13, color: statusInfo.color, fontWeight: 600 }}>
            {statusInfo.label}
          </Text>
        </div>
        <Title level={4} style={{ margin: '0 0 4px', fontFamily: 'monospace', color: '#0f172a' }}>
          {ticket.ticketNo}
        </Title>
        <Text type="secondary" style={{ fontSize: 12 }}>
          {ticket.createdAt ? new Date(ticket.createdAt).toLocaleString('zh-CN', {
            year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
          }) : '-'}
        </Text>
      </div>

      {/* 客户信息卡片 */}
      <div style={{
        background: '#fff',
        borderRadius: 12,
        padding: 16,
        marginBottom: 12,
        border: '1px solid #e2e8f0',
      }}>
        <Text type="secondary" style={{ fontSize: 11, textTransform: 'uppercase', letterSpacing: 1, marginBottom: 8, display: 'block' }}>
          客户信息
        </Text>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <UserOutlined style={{ color: '#94a3b8', fontSize: 14 }} />
            <Text style={{ fontSize: 15, color: '#0f172a' }}>{ticket.customerName}</Text>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <PhoneOutlined style={{ color: '#94a3b8', fontSize: 14 }} />
            <a
              href={`tel:${ticket.customerPhone}`}
              style={{ fontSize: 15, color: '#2563eb', textDecoration: 'none' }}
            >
              {ticket.customerPhone}
            </a>
          </div>
          {ticket.serviceAddress && (
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
              <EnvironmentOutlined style={{ color: '#94a3b8', fontSize: 14, marginTop: 2 }} />
              <div style={{ flex: 1 }}>
                <Text style={{ fontSize: 14, color: '#0f172a', wordBreak: 'break-all' }}>
                  {ticket.serviceAddress}
                </Text>
                <Button
                  type="link"
                  size="small"
                  icon={<CopyOutlined />}
                  onClick={() => handleCopy(ticket.serviceAddress!)}
                  style={{ padding: '0 4px', fontSize: 11, height: 20 }}
                >
                  复制
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 工单信息卡片 */}
      <div style={{
        background: '#fff',
        borderRadius: 12,
        padding: 16,
        marginBottom: 12,
        border: '1px solid #e2e8f0',
      }}>
        <Text type="secondary" style={{ fontSize: 11, textTransform: 'uppercase', letterSpacing: 1, marginBottom: 8, display: 'block' }}>
          工单信息
        </Text>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px 16px' }}>
          {ticket.productType && (
            <div>
              <Text type="secondary" style={{ fontSize: 11 }}>产品类型</Text>
              <div><Text style={{ fontSize: 14, color: '#0f172a' }}>{ticket.productType}</Text></div>
            </div>
          )}
          {ticket.faultType && (
            <div>
              <Text type="secondary" style={{ fontSize: 11 }}>故障类型</Text>
              <div><Text style={{ fontSize: 14, color: '#0f172a' }}>{ticket.faultType}</Text></div>
            </div>
          )}
          {ticket.priority && (
            <div>
              <Text type="secondary" style={{ fontSize: 11 }}>优先级</Text>
              <div>
                <Tag color={ticket.priority === 'URGENT' ? 'red' : ticket.priority === 'HIGH' ? 'orange' : 'blue'} style={{ margin: 0, borderRadius: 4 }}>
                  {ticket.priority === 'URGENT' ? '紧急' : ticket.priority === 'HIGH' ? '高' : ticket.priority === 'NORMAL' ? '普通' : ticket.priority === 'LOW' ? '低' : ticket.priority}
                </Tag>
              </div>
            </div>
          )}
          {ticket.scheduledTime && (
            <div>
              <Text type="secondary" style={{ fontSize: 11 }}>预约时间</Text>
              <div><Text style={{ fontSize: 14, color: '#0f172a' }}>{new Date(ticket.scheduledTime).toLocaleString('zh-CN')}</Text></div>
            </div>
          )}
        </div>
        {ticket.faultDescription && (
          <div style={{ marginTop: 12 }}>
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 4 }}>故障描述</Text>
            <div style={{
              background: '#f8fafc',
              borderRadius: 8,
              padding: '10px 12px',
              fontSize: 13,
              color: '#334155',
              lineHeight: '20px',
            }}>
              {ticket.faultDescription}
            </div>
          </div>
        )}
      </div>

      {/* 维修结果卡片（已完成/已关闭时显示） */}
      {ticket.repairResult && (
        <div style={{
          background: '#fff',
          borderRadius: 12,
          padding: 16,
          marginBottom: 12,
          border: '1px solid #e2e8f0',
          borderLeft: '3px solid #059669',
        }}>
          <Text type="secondary" style={{ fontSize: 11, textTransform: 'uppercase', letterSpacing: 1, marginBottom: 8, display: 'block' }}>
            维修结果
          </Text>
          <Text style={{ fontSize: 14, color: '#0f172a', lineHeight: '22px', whiteSpace: 'pre-wrap' }}>
            {ticket.repairResult}
          </Text>
          {ticket.partsNote && (
            <div style={{ marginTop: 12 }}>
              <Text type="secondary" style={{ fontSize: 11 }}>配件说明</Text>
              <div style={{
                background: '#f8fafc', borderRadius: 6, padding: '8px 10px', marginTop: 4,
                fontSize: 13, color: '#475569',
              }}>
                {ticket.partsNote}
              </div>
            </div>
          )}
          {ticket.costNote && (
            <div style={{ marginTop: 12 }}>
              <Text type="secondary" style={{ fontSize: 11 }}>费用说明</Text>
              <div style={{
                background: '#f8fafc', borderRadius: 6, padding: '8px 10px', marginTop: 4,
                fontSize: 13, color: '#475569',
              }}>
                {ticket.costNote}
              </div>
            </div>
          )}
        </div>
      )}

      {/* 状态时间线 */}
      {statusLogs.length > 0 && (
        <div style={{
          background: '#fff',
          borderRadius: 12,
          padding: 16,
          marginBottom: 12,
          border: '1px solid #e2e8f0',
        }}>
          <Text type="secondary" style={{ fontSize: 11, textTransform: 'uppercase', letterSpacing: 1, marginBottom: 12, display: 'block' }}>
            处理记录
          </Text>
          <Timeline
            items={[...statusLogs].reverse().map((log) => {
              const targetStatusInfo = STATUS_MAP[log.toStatus];
              return {
                color: targetStatusInfo?.color || '#94a3b8',
                children: (
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 2 }}>
                      <Text style={{ fontSize: 13, fontWeight: 500, color: '#0f172a' }}>
                        {STATUS_TIMELINE_MAP[log.toStatus] || log.toStatus}
                      </Text>
                      {targetStatusInfo && (
                        <Tag color={targetStatusInfo.color} style={{ margin: 0, fontSize: 10, lineHeight: '16px', padding: '0 6px', borderRadius: 4 }}>
                          {targetStatusInfo.label}
                        </Tag>
                      )}
                    </div>
                    {log.remark && (
                      <Text style={{ fontSize: 12, color: '#64748b' }}>{log.remark}</Text>
                    )}
                    <Text type="secondary" style={{ fontSize: 11, display: 'block' }}>
                      {new Date(log.createdAt).toLocaleString('zh-CN', {
                        month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
                      })}
                    </Text>
                  </div>
                ),
              };
            })}
          />
        </div>
      )}

      {/* 操作按钮区 */}
      {!isReadonly && (
        <div style={{
          background: '#fff',
          borderRadius: 12,
          padding: 16,
          border: '1px solid #e2e8f0',
          position: 'sticky',
          bottom: 0,
        }}>
          {ticket.status === 'ASSIGNED' && (
            <Button
              type="primary"
              block
              size="large"
              icon={<PlayCircleOutlined />}
              loading={actionLoading}
              onClick={handleStart}
              style={{
                height: 48,
                borderRadius: 8,
                fontWeight: 600,
                fontSize: 15,
              }}
            >
              开始处理
            </Button>
          )}
          {ticket.status === 'IN_PROGRESS' && (
            <Button
              type="primary"
              block
              size="large"
              icon={<CheckCircleOutlined />}
              loading={actionLoading}
              onClick={() => setCompleteOpen(true)}
              style={{
                height: 48,
                borderRadius: 8,
                fontWeight: 600,
                fontSize: 15,
                background: '#059669',
                borderColor: '#059669',
              }}
            >
              完成工单
            </Button>
          )}
        </div>
      )}
      {isReadonly && (
        <div style={{
          textAlign: 'center',
          padding: 12,
          color: '#94a3b8',
          fontSize: 12,
        }}>
          <CheckCircleOutlined style={{ marginRight: 4 }} />
          此工单已完结，无需操作
        </div>
      )}

      {/* 完成工单弹窗 */}
      <Modal
        title="完成维修工单"
        open={completeOpen}
        onOk={handleComplete}
        onCancel={() => {
          setCompleteOpen(false);
          form.resetFields();
        }}
        confirmLoading={actionLoading}
        okText="提交"
        cancelText="取消"
        destroyOnHidden
        styles={{ body: { padding: '16px 24px' } }}
      >
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item
            name="repairResult"
            label="维修结果"
            rules={[{ required: true, message: '请填写维修结果' }]}
          >
            <TextArea
              rows={4}
              placeholder="描述故障原因、维修措施和结果..."
              style={{ borderRadius: 8 }}
            />
          </Form.Item>
          <Form.Item
            name="partsNote"
            label="配件说明"
          >
            <TextArea
              rows={2}
              placeholder="更换的配件型号、数量等（选填）"
              style={{ borderRadius: 8 }}
            />
          </Form.Item>
          <Form.Item
            name="costNote"
            label="费用说明"
          >
            <TextArea
              rows={2}
              placeholder="维修费用明细（选填）"
              style={{ borderRadius: 8 }}
            />
          </Form.Item>
          <Form.Item
            name="remark"
            label="备注"
          >
            <TextArea
              rows={2}
              placeholder="其他需要说明的情况（选填）"
              style={{ borderRadius: 8 }}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default TechnicianTicketDetailPage;
