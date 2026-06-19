import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { CheckCircleOutlined, ArrowLeftOutlined, FileTextOutlined } from '@ant-design/icons';
import { submitRepair, type RepairResponse } from '../../api/portal';

const PortalRepairPage: React.FC = () => {
  const { tenantCode } = useParams<{ tenantCode: string }>();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: '', phone: '', address: '', productType: '', faultDescription: '',
  });
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<RepairResponse | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const basePath = `/portal/${tenantCode}`;

  const validate = (): boolean => {
    const errs: Record<string, string> = {};
    if (!form.name.trim()) errs.name = '请输入联系人姓名';
    if (!form.phone.trim()) errs.phone = '请输入手机号';
    else if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) errs.phone = '请输入正确的手机号';
    if (!form.faultDescription.trim()) errs.faultDescription = '请描述故障情况';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    setLoading(true);
    try {
      const res = await submitRepair(tenantCode!, {
        name: form.name.trim(),
        phone: form.phone.trim(),
        address: form.address.trim() || undefined,
        productType: form.productType.trim() || undefined,
        faultDescription: form.faultDescription.trim(),
      });
      setResult(res);
    } catch {
      // http interceptor handles error
    } finally {
      setLoading(false);
    }
  };

  const updateField = (field: string, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => { const next = { ...prev }; delete next[field]; return next; });
    }
  };

  // 提交成功页
  if (result) {
    return (
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 40 }}>
        <div style={{ textAlign: 'center', maxWidth: 400 }}>
          <div style={{
            width: 72, height: 72, borderRadius: 24, margin: '0 auto 20px',
            background: '#f0fdfa', border: '2px solid #0d9488',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <CheckCircleOutlined style={{ fontSize: 36, color: '#0d9488' }} />
          </div>
          <h2 style={{ fontSize: 22, fontWeight: 700, color: '#0f172a', margin: '0 0 8px' }}>
            报修已提交
          </h2>
          <p style={{ fontSize: 14, color: '#64748b', margin: '0 0 24px', lineHeight: 1.6 }}>
            {result.message}
          </p>
          <div style={{
            padding: '16px 24px', borderRadius: 12, background: '#f8fafc',
            border: '1px solid #e2e8f0', marginBottom: 32, textAlign: 'left',
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
              <span style={{ color: '#64748b', fontSize: 13 }}>工单编号</span>
              <span style={{ color: '#0f172a', fontSize: 15, fontWeight: 700, fontFamily: 'monospace' }}>{result.ticketNo}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: '#64748b', fontSize: 13 }}>当前状态</span>
              <span style={{ color: '#0d9488', fontSize: 13, fontWeight: 600 }}>待处理</span>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
            <button
              onClick={() => navigate(basePath)}
              style={{
                padding: '10px 24px', borderRadius: 10,
                border: '1px solid #e2e8f0', background: '#fff',
                color: '#475569', fontSize: 14, cursor: 'pointer', fontWeight: 500,
              }}
            >
              返回首页
            </button>
            <button
              onClick={() => navigate(`${basePath}/chat`)}
              style={{
                padding: '10px 24px', borderRadius: 10,
                border: 'none', background: '#0d9488',
                color: '#fff', fontSize: 14, cursor: 'pointer', fontWeight: 500,
              }}
            >
              继续咨询 AI
            </button>
          </div>
        </div>
      </div>
    );
  }

  // 报修表单
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
              background: '#fff7ed', border: '1px solid #fed7aa',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <FileTextOutlined style={{ fontSize: 20, color: '#f97316' }} />
            </div>
            <div>
              <h1 style={{ fontSize: 20, fontWeight: 700, color: '#0f172a', margin: 0 }}>提交报修</h1>
              <p style={{ fontSize: 13, color: '#94a3b8', margin: 0 }}>填写以下信息，我们会尽快安排处理</p>
            </div>
          </div>
        </div>

        {/* 表单卡片 */}
        <form onSubmit={handleSubmit}>
          {/* 联系信息 */}
          <div style={{
            background: '#fff', borderRadius: 16, padding: 24, marginBottom: 16,
            border: '1px solid #e2e8f0',
          }}>
            <h3 style={{ fontSize: 14, fontWeight: 600, color: '#0f172a', margin: '0 0 16px' }}>联系信息</h3>
            <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
              <div style={{ flex: 1 }}>
                <label style={labelStyle}>联系人 <span style={{ color: '#ef4444' }}>*</span></label>
                <input
                  value={form.name}
                  onChange={(e) => updateField('name', e.target.value)}
                  placeholder="您的姓名"
                  style={inputStyle('name')}
                />
                {errors.name && <div style={errorStyle}>{errors.name}</div>}
              </div>
              <div style={{ flex: 1 }}>
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
            <div>
              <label style={labelStyle}>地址</label>
              <input
                value={form.address}
                onChange={(e) => updateField('address', e.target.value)}
                placeholder="维修地址（选填）"
                style={inputStyle('address')}
              />
            </div>
          </div>

          {/* 故障信息 */}
          <div style={{
            background: '#fff', borderRadius: 16, padding: 24, marginBottom: 24,
            border: '1px solid #e2e8f0',
          }}>
            <h3 style={{ fontSize: 14, fontWeight: 600, color: '#0f172a', margin: '0 0 16px' }}>故障信息</h3>
            <div style={{ marginBottom: 16 }}>
              <label style={labelStyle}>产品类型</label>
              <input
                value={form.productType}
                onChange={(e) => updateField('productType', e.target.value)}
                placeholder="例如：空调、洗衣机、冰箱（选填）"
                style={inputStyle('productType')}
              />
            </div>
            <div>
              <label style={labelStyle}>故障描述 <span style={{ color: '#ef4444' }}>*</span></label>
              <textarea
                value={form.faultDescription}
                onChange={(e) => updateField('faultDescription', e.target.value)}
                placeholder="请详细描述故障现象，例如：开机后不制冷，有异响..."
                rows={4}
                style={{
                  ...inputStyle('faultDescription'),
                  height: 'auto', minHeight: 100, padding: '10px 14px', resize: 'vertical',
                  lineHeight: 1.6,
                }}
              />
              {errors.faultDescription && <div style={errorStyle}>{errors.faultDescription}</div>}
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%', height: 48, borderRadius: 12,
              border: 'none', background: '#0d9488', color: '#fff',
              fontSize: 15, fontWeight: 600, cursor: loading ? 'default' : 'pointer',
              opacity: loading ? 0.7 : 1, fontFamily: 'inherit',
            }}
          >
            {loading ? '提交中...' : '提交报修'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default PortalRepairPage;
