import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { SearchOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { queryTicket, type TicketQueryResult } from '../../api/portal';
import { useThemeColor } from '../../contexts/PortalConfigContext';

const STATUS_COLORS: Record<string, string> = {
  PENDING: '#f59e0b',
  ASSIGNED: '#3b82f6',
  IN_PROGRESS: '#8b5cf6',
  COMPLETED: '#10b981',
  FOLLOWED_UP: '#06b6d4',
  CLOSED: '#6b7280',
  CANCELLED: '#ef4444',
};

const formatDate = (d: string | null | undefined): string => {
  if (!d) return '-';
  const date = new Date(d);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const PortalQueryPage: React.FC = () => {
  const { tenantCode } = useParams<{ tenantCode: string }>();
  const navigate = useNavigate();
  const themeColor = useThemeColor();
  const basePath = `/portal/${tenantCode}`;

  const [form, setForm] = useState({ ticketNo: '', phone: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [queryError, setQueryError] = useState<string | null>(null);
  const [result, setResult] = useState<TicketQueryResult | null>(null);

  const validate = (): boolean => {
    const errs: Record<string, string> = {};
    if (!form.ticketNo.trim()) errs.ticketNo = '请输入工单编号';
    if (!form.phone.trim()) errs.phone = '请输入手机号';
    else if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) errs.phone = '请输入正确的手机号';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    setLoading(true);
    setQueryError(null);
    try {
      const res = await queryTicket(tenantCode!, form.ticketNo.trim(), form.phone.trim());
      setResult(res);
    } catch {
      setQueryError('未找到匹配的工单，请检查工单编号和手机号');
    } finally {
      setLoading(false);
    }
  };

  const updateField = (field: string, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => { const next = { ...prev }; delete next[field]; return next; });
    }
    if (queryError) setQueryError(null);
  };

  const handleBackToQuery = () => {
    setResult(null);
    setQueryError(null);
    setForm({ ticketNo: '', phone: '' });
    setErrors({});
  };

  // ========== 结果视图 ==========
  if (result) {
    const statusColor = STATUS_COLORS[result.status] || '#6b7280';

    return (
      <div style={{ flex: 1, padding: '32px 16px', background: '#f8fafc' }}>
        <div style={{ maxWidth: 520, margin: '0 auto' }}>
          {/* 返回按钮 */}
          <button
            onClick={handleBackToQuery}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 6,
              padding: '6px 0', border: 'none', background: 'none',
              color: '#64748b', fontSize: 13, cursor: 'pointer', marginBottom: 24,
            }}
          >
            <ArrowLeftOutlined /> 返回查询
          </button>

          {/* 状态徽章 */}
          <div style={{
            display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24,
            padding: '16px 20px', background: '#fff', borderRadius: 16,
            border: '1px solid #e2e8f0',
          }}>
            <div style={{
              width: 12, height: 12, borderRadius: '50%', background: statusColor,
              boxShadow: `0 0 8px ${statusColor}40`,
            }} />
            <div>
              <div style={{ fontSize: 18, fontWeight: 700, color: '#0f172a' }}>{result.statusLabel}</div>
              <div style={{ fontSize: 12, color: '#94a3b8' }}>当前状态</div>
            </div>
            <div style={{ marginLeft: 'auto', textAlign: 'right' }}>
              <div style={{ fontSize: 13, color: '#64748b', fontFamily: 'monospace' }}>{result.ticketNo}</div>
              <div style={{ fontSize: 12, color: '#94a3b8' }}>工单编号</div>
            </div>
          </div>

          {/* 工单信息 */}
          <div style={{
            background: '#fff', borderRadius: 16, padding: 20, marginBottom: 16,
            border: '1px solid #e2e8f0',
          }}>
            <h3 style={{ fontSize: 14, fontWeight: 600, color: '#0f172a', margin: '0 0 14px' }}>工单信息</h3>
            <InfoRow label="产品类型" value={result.productType || '-'} />
            <InfoRow label="故障描述" value={result.faultDescription || '-'} />
            <InfoRow label="优先级" value={result.priority} />
            <InfoRow label="创建时间" value={formatDate(result.createdAt)} />
            {result.scheduledTime && <InfoRow label="预约时间" value={formatDate(result.scheduledTime)} />}
            {result.startTime && <InfoRow label="开始处理" value={formatDate(result.startTime)} />}
            {result.completionTime && <InfoRow label="完成时间" value={formatDate(result.completionTime)} />}
          </div>

          {/* 客户信息 */}
          <div style={{
            background: '#fff', borderRadius: 16, padding: 20, marginBottom: 16,
            border: '1px solid #e2e8f0',
          }}>
            <h3 style={{ fontSize: 14, fontWeight: 600, color: '#0f172a', margin: '0 0 14px' }}>联系信息</h3>
            <InfoRow label="联系人" value={result.customerName} />
            <InfoRow label="手机号" value={result.customerPhone} />
            <InfoRow label="地址" value={result.serviceAddress || '-'} />
          </div>

          {/* 师傅信息 */}
          {result.technicianName && (
            <div style={{
              background: '#fff', borderRadius: 16, padding: 20, marginBottom: 16,
              border: '1px solid #e2e8f0',
            }}>
              <h3 style={{ fontSize: 14, fontWeight: 600, color: '#0f172a', margin: '0 0 14px' }}>师傅信息</h3>
              <InfoRow label="师傅姓名" value={result.technicianName} />
              <InfoRow label="联系电话" value={result.technicianPhone || '-'} />
            </div>
          )}

          {/* 状态时间线 */}
          <div style={{
            background: '#fff', borderRadius: 16, padding: 20, marginBottom: 24,
            border: '1px solid #e2e8f0',
          }}>
            <h3 style={{ fontSize: 14, fontWeight: 600, color: '#0f172a', margin: '0 0 14px' }}>处理记录</h3>
            {result.statusLogs.length === 0 ? (
              <p style={{ color: '#94a3b8', fontSize: 13, margin: 0 }}>暂无记录</p>
            ) : (
              <div style={{ position: 'relative', paddingLeft: 20 }}>
                {result.statusLogs.map((log, idx) => {
                  const dotColor = STATUS_COLORS[log.toStatus] || '#6b7280';
                  return (
                    <div key={idx} style={{
                      position: 'relative',
                      paddingBottom: idx < result.statusLogs.length - 1 ? 16 : 0,
                      borderLeft: idx < result.statusLogs.length - 1
                        ? '2px solid #e2e8f0'
                        : '2px solid transparent',
                      marginLeft: -20, paddingLeft: 20,
                    }}>
                      <div style={{
                        position: 'absolute', left: -5, top: 2,
                        width: 8, height: 8, borderRadius: '50%',
                        background: dotColor, border: '2px solid #fff',
                        boxShadow: `0 0 0 2px ${dotColor}30`,
                      }} />
                      <div style={{ fontSize: 14, fontWeight: 600, color: '#0f172a', marginBottom: 2 }}>
                        {log.toStatusLabel}
                      </div>
                      {log.remark && (
                        <div style={{ fontSize: 13, color: '#64748b', marginBottom: 2 }}>{log.remark}</div>
                      )}
                      <div style={{ fontSize: 12, color: '#94a3b8' }}>{formatDate(log.createdAt)}</div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  // ========== 表单视图 ==========
  const inputStyle = (field: string): React.CSSProperties => ({
    width: '100%', height: 44, borderRadius: 10,
    border: `1.5px solid ${errors[field] ? '#ef4444' : '#e2e8f0'}`,
    padding: '0 14px', fontSize: 14, outline: 'none',
    color: '#0f172a', background: '#fff', boxSizing: 'border-box' as const,
    fontFamily: 'inherit',
  });

  const labelStyle: React.CSSProperties = {
    display: 'block', fontSize: 13, fontWeight: 600, color: '#334155', marginBottom: 6,
  };

  const errorStyle: React.CSSProperties = {
    fontSize: 12, color: '#ef4444', marginTop: 4,
  };

  return (
    <div style={{ flex: 1, padding: '32px 16px', background: '#f8fafc' }}>
      <div style={{ maxWidth: 520, margin: '0 auto' }}>
        {/* 头部 */}
        <div style={{ marginBottom: 32 }}>
          <button
            onClick={() => navigate(basePath)}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 6,
              padding: '6px 0', border: 'none', background: 'none',
              color: '#64748b', fontSize: 13, cursor: 'pointer', marginBottom: 16,
            }}
          >
            <ArrowLeftOutlined /> 返回首页
          </button>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{
              width: 44, height: 44, borderRadius: 12,
              background: '#eff6ff', border: '1px solid #bfdbfe',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <SearchOutlined style={{ fontSize: 20, color: '#2563eb' }} />
            </div>
            <div>
              <h1 style={{ fontSize: 20, fontWeight: 700, color: '#0f172a', margin: 0 }}>查询进度</h1>
              <p style={{ fontSize: 13, color: '#94a3b8', margin: 0 }}>输入工单编号和手机号查询维修进度</p>
            </div>
          </div>
        </div>

        {/* 表单 */}
        <form onSubmit={handleSubmit}>
          <div style={{
            background: '#fff', borderRadius: 16, padding: 24, marginBottom: 24,
            border: '1px solid #e2e8f0',
          }}>
            <div style={{ marginBottom: 16 }}>
              <label style={labelStyle}>工单编号 <span style={{ color: '#ef4444' }}>*</span></label>
              <input
                value={form.ticketNo}
                onChange={(e) => updateField('ticketNo', e.target.value)}
                placeholder="例如：TK202606210001"
                style={{ ...inputStyle('ticketNo'), fontFamily: 'monospace' }}
              />
              {errors.ticketNo && <div style={errorStyle}>{errors.ticketNo}</div>}
            </div>
            <div>
              <label style={labelStyle}>手机号 <span style={{ color: '#ef4444' }}>*</span></label>
              <input
                value={form.phone}
                onChange={(e) => updateField('phone', e.target.value)}
                placeholder="11 位手机号"
                maxLength={11}
                style={inputStyle('phone')}
              />
              {errors.phone && <div style={errorStyle}>{errors.phone}</div>}
            </div>
          </div>

          {queryError && (
            <div style={{
              padding: '12px 16px', borderRadius: 12, marginBottom: 16,
              background: '#fef2f2', border: '1px solid #fecaca',
              fontSize: 13, color: '#dc2626',
            }}>
              {queryError}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%', height: 48, borderRadius: 12,
              border: 'none', background: themeColor, color: '#fff',
              fontSize: 15, fontWeight: 600, cursor: loading ? 'default' : 'pointer',
              opacity: loading ? 0.7 : 1, fontFamily: 'inherit',
            }}
          >
            {loading ? '查询中...' : '查询'}
          </button>
        </form>
      </div>
    </div>
  );
};

/** 信息行组件（内联，不拆分文件） */
const InfoRow: React.FC<{ label: string; value: string }> = ({ label, value }) => (
  <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f1f5f9' }}>
    <span style={{ fontSize: 13, color: '#64748b', flexShrink: 0 }}>{label}</span>
    <span style={{ fontSize: 13, color: '#0f172a', textAlign: 'right', marginLeft: 16 }}>{value}</span>
  </div>
);

export default PortalQueryPage;
