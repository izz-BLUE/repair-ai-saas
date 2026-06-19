import React from 'react';
import { Outlet, useParams, useNavigate, useLocation } from 'react-router-dom';
import { HomeOutlined, RobotOutlined, FormOutlined, SearchOutlined } from '@ant-design/icons';

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

  const isActive = (path: string) => {
    const fullPath = basePath + path;
    return location.pathname === fullPath || (path === '' && location.pathname === basePath);
  };

  return (
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
            <div style={{
              width: 32, height: 32, borderRadius: 8,
              background: 'linear-gradient(135deg, #0d9488, #0f766e)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <RobotOutlined style={{ color: '#fff', fontSize: 16 }} />
            </div>
            <span style={{ fontWeight: 700, fontSize: 16, color: '#0f172a', letterSpacing: -0.3 }}>
              智能服务平台
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
                color: isActive(item.path) ? '#0d9488' : '#64748b',
                background: isActive(item.path) ? '#f0fdfa' : 'transparent',
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
        Powered by Repair AI · 智能售后服务平台
      </footer>
    </div>
  );
};

export default PortalLayout;
