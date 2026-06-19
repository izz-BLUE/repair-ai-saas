import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { SearchOutlined, ArrowLeftOutlined, ClockCircleOutlined } from '@ant-design/icons';

const PortalQueryPage: React.FC = () => {
  const { tenantCode } = useParams<{ tenantCode: string }>();
  const navigate = useNavigate();
  const basePath = `/portal/${tenantCode}`;

  return (
    <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 40 }}>
      <div style={{ textAlign: 'center', maxWidth: 400 }}>
        <div style={{
          width: 72, height: 72, borderRadius: 24, margin: '0 auto 20px',
          background: '#eff6ff', border: '2px solid #93c5fd',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <SearchOutlined style={{ fontSize: 32, color: '#2563eb' }} />
        </div>
        <h2 style={{ fontSize: 20, fontWeight: 700, color: '#0f172a', margin: '0 0 8px' }}>
          查询进度
        </h2>
        <p style={{ fontSize: 14, color: '#64748b', margin: '0 0 24px', lineHeight: 1.6 }}>
          工单进度查询功能正在开发中，后续版本将支持通过手机号和工单号查询维修进度。
        </p>
        <div style={{
          padding: '12px 16px', borderRadius: 12,
          background: '#eff6ff', border: '1px solid #bfdbfe',
          display: 'inline-flex', alignItems: 'center', gap: 8,
          marginBottom: 32,
        }}>
          <ClockCircleOutlined style={{ color: '#2563eb' }} />
          <span style={{ fontSize: 13, color: '#1e40af' }}>预计下个版本开放</span>
        </div>
        <div>
          <button
            onClick={() => navigate(basePath)}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 6,
              padding: '10px 24px', borderRadius: 10,
              border: '1px solid #e2e8f0', background: '#fff',
              color: '#475569', fontSize: 14, cursor: 'pointer', fontWeight: 500,
            }}
          >
            <ArrowLeftOutlined /> 返回首页
          </button>
        </div>
      </div>
    </div>
  );
};

export default PortalQueryPage;
