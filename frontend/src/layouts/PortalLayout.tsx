import React, { useEffect, useState } from 'react';
import { Outlet, useParams, useNavigate, useLocation } from 'react-router-dom';
import { HomeOutlined, RobotOutlined, FormOutlined, SearchOutlined, LockOutlined, ExclamationCircleOutlined, FieldTimeOutlined } from '@ant-design/icons';
import { getPortalSettings, type PortalConfig } from '../api/portal';
import { PortalConfigProvider } from '../contexts/PortalConfigContext';

const navItems = [
  { path: '', label: '首页', icon: <HomeOutlined /> },
  { path: '/chat', label: 'AI 客服', icon: <RobotOutlined /> },
  { path: '/repair', label: '提交报修', icon: <FormOutlined /> },
  { path: '/query', label: '查询进度', icon: <SearchOutlined /> },
];

const PortalLayout: React.FC = () => {
  const { tenantCode } = useParams<{ tenantCode: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const basePath = `/portal/${tenantCode}`;

  const [config, setConfig] = useState<PortalConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<'not_found' | 'disabled' | 'expired' | null>(null);

  useEffect(() => {
    if (!tenantCode) return;
    setLoading(true);
    setError(null);
    getPortalSettings(tenantCode)
      .then((data) => {
        setConfig(data);
        if (data.expired) {
          setError('expired');
        } else if (!data.portalEnabled) {
          setError('disabled');
        }
      })
      .catch((err) => {
        if (err?.message?.includes('企业不存在') || err?.message?.includes('NOT_FOUND')) {
          setError('not_found');
        } else {
          setError('not_found');
        }
      })
      .finally(() => setLoading(false));
  }, [tenantCode]);

  const isActive = (path: string) => {
    const fullPath = basePath + path;
    return location.pathname === fullPath || (path === '' && location.pathname === basePath);
  };

  const themeColor = config?.themeColor || '#0d9488';
  const title = config?.portalTitle || config?.name || '智能服务平台';

  // 加载中
  if (loading) {
    return (
      <div style={{ minHeight: '100vh', background: '#f8fafc', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ width: 32, height: 32, border: '3px solid #e2e8f0', borderTopColor: '#0d9488', borderRadius: '50%', animation: 'spin 0.8s linear infinite', margin: '0 auto 16px' }} />
          <p style={{ color: '#94a3b8', fontSize: 14 }}>加载中...</p>
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </div>
      </div>
    );
  }

  // 企业不存在
  if (error === 'not_found') {
    return (
      <div style={{ minHeight: '100vh', background: '#f8fafc', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}>
        <div style={{ textAlign: 'center', maxWidth: 400 }}>
          <ExclamationCircleOutlined style={{ fontSize: 56, color: '#ef4444', marginBottom: 20 }} />
          <h2 style={{ fontSize: 22, fontWeight: 700, color: '#0f172a', margin: '0 0 8px' }}>企业不存在</h2>
          <p style={{ color: '#64748b', fontSize: 15, margin: '0 0 24px', lineHeight: 1.6 }}>
            请检查链接是否正确，或联系企业客服获取正确的访问地址。
          </p>
          <button
            onClick={() => window.location.href = '/'}
            style={{ padding: '10px 24px', borderRadius: 8, border: '1px solid #e2e8f0', background: '#fff', color: '#475569', fontSize: 14, cursor: 'pointer' }}
          >
            返回首页
          </button>
        </div>
      </div>
    );
  }

  // 门户已停用
  if (error === 'disabled') {
    return (
      <div style={{ minHeight: '100vh', background: '#f8fafc', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}>
        <div style={{ textAlign: 'center', maxWidth: 400 }}>
          <LockOutlined style={{ fontSize: 56, color: '#d97706', marginBottom: 20 }} />
          <h2 style={{ fontSize: 22, fontWeight: 700, color: '#0f172a', margin: '0 0 8px' }}>该企业服务门户暂未启用</h2>
          <p style={{ color: '#64748b', fontSize: 15, margin: '0 0 8px', lineHeight: 1.6 }}>
            {config?.name ? `${config.name} 的` : ''}在线服务暂时不可用。
          </p>
          {config?.contactPhone && (
            <p style={{ color: '#475569', fontSize: 15, margin: '0 0 24px' }}>
              联系电话：<strong>{config.contactPhone}</strong>
            </p>
          )}
          {!config?.contactPhone && <div style={{ height: 24 }} />}
          <button
            onClick={() => window.location.href = '/'}
            style={{ padding: '10px 24px', borderRadius: 8, border: '1px solid #e2e8f0', background: '#fff', color: '#475569', fontSize: 14, cursor: 'pointer' }}
          >
            返回首页
          </button>
        </div>
      </div>
    );
  }

  // 服务已到期
  if (error === 'expired') {
    return (
      <div style={{ minHeight: '100vh', background: '#f8fafc', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}>
        <div style={{ textAlign: 'center', maxWidth: 400 }}>
          <FieldTimeOutlined style={{ fontSize: 56, color: '#dc2626', marginBottom: 20 }} />
          <h2 style={{ fontSize: 22, fontWeight: 700, color: '#0f172a', margin: '0 0 8px' }}>服务已到期</h2>
          <p style={{ color: '#64748b', fontSize: 15, margin: '0 0 8px', lineHeight: 1.6 }}>
            {config?.name ? `${config.name} 的` : ''}服务订阅已到期，在线服务暂停使用。
          </p>
          {config?.contactPhone && (
            <p style={{ color: '#475569', fontSize: 15, margin: '0 0 24px' }}>
              联系电话：<strong>{config.contactPhone}</strong>
            </p>
          )}
          {!config?.contactPhone && <div style={{ height: 24 }} />}
          <button
            onClick={() => window.location.href = '/'}
            style={{ padding: '10px 24px', borderRadius: 8, border: '1px solid #e2e8f0', background: '#fff', color: '#475569', fontSize: 14, cursor: 'pointer' }}
          >
            返回首页
          </button>
        </div>
      </div>
    );
  }

  const portalConfig: PortalConfig = config!;

  return (
    <PortalConfigProvider config={portalConfig}>
      <div style={{ minHeight: '100vh', background: '#f8fafc', display: 'flex', flexDirection: 'column' }}>
        {/* 顶栏 */}
        <header style={{
          background: '#fff',
          borderBottom: '1px solid #e2e8f0',
          padding: '0 20px',
          height: 56,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          position: 'sticky',
          top: 0,
          zIndex: 100,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              onClick={() => navigate(basePath)}
              style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}
            >
              {portalConfig.logoUrl ? (
                <img src={portalConfig.logoUrl} alt="Logo" style={{ width: 32, height: 32, borderRadius: 8, objectFit: 'cover' }} />
              ) : (
                <div style={{
                  width: 32, height: 32, borderRadius: 8,
                  background: `linear-gradient(135deg, ${themeColor}, ${themeColor}dd)`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                  <RobotOutlined style={{ color: '#fff', fontSize: 16 }} />
                </div>
              )}
              <span style={{ fontWeight: 700, fontSize: 16, color: '#0f172a', letterSpacing: -0.3 }}>
                {title}
              </span>
            </div>
          </div>

          {/* 桌面端导航 */}
          <nav style={{ display: 'flex', gap: 4 }}>
            {navItems.map((item) => (
              <button
                key={item.path}
                onClick={() => navigate(basePath + item.path)}
                style={{
                  display: 'flex', alignItems: 'center', gap: 5,
                  padding: '6px 14px', borderRadius: 8, border: 'none', cursor: 'pointer',
                  fontSize: 13, fontWeight: isActive(item.path) ? 600 : 400,
                  color: isActive(item.path) ? themeColor : '#64748b',
                  background: isActive(item.path) ? `${themeColor}10` : 'transparent',
                  transition: 'all 0.2s',
                }}
              >
                {item.icon}
                {item.label}
              </button>
            ))}
          </nav>
        </header>

        {/* 内容区 */}
        <main style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          <Outlet />
        </main>

        {/* 底部 */}
        <footer style={{
          textAlign: 'center', padding: '16px 20px',
          color: '#94a3b8', fontSize: 12, borderTop: '1px solid #e2e8f0',
          background: '#fff',
        }}>
          {portalConfig.portalTitle ? `Powered by ${portalConfig.portalTitle}` : 'Powered by Repair AI'} · 智能售后服务平台
          {portalConfig.contactPhone && ` · ${portalConfig.contactPhone}`}
        </footer>
      </div>
    </PortalConfigProvider>
  );
};

export default PortalLayout;
