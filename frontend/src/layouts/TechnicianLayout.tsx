import React, { useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Button, Typography, App } from 'antd';
import { LogoutOutlined, LeftOutlined, ToolOutlined } from '@ant-design/icons';

const { Text } = Typography;

const TechnicianLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { message } = App.useApp();

  const user = JSON.parse(localStorage.getItem('user') || '{}');

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/admin/login');
      return;
    }
    // 非师傅角色不允许进入师傅端
    if (user.role && user.role !== 'TECHNICIAN') {
      if (user.role === 'SUPER_ADMIN') {
        navigate('/platform/tenants');
      } else {
        navigate('/admin/dashboard');
      }
      message.warning('当前账号不是维修师傅，已自动跳转');
    }
  }, [navigate, user.role, message]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/admin/login');
  };

  const isDetailPage = location.pathname.includes('/technician/tickets/');

  return (
    <div style={{ minHeight: '100vh', background: '#f1f5f9', paddingBottom: 24 }}>
      {/* 顶栏 */}
      <div style={{
        position: 'sticky',
        top: 0,
        zIndex: 10,
        background: '#fff',
        borderBottom: '1px solid #e2e8f0',
        padding: '0 16px',
        height: 52,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {isDetailPage ? (
            <Button
              type="text"
              icon={<LeftOutlined />}
              onClick={() => navigate('/technician/tickets')}
              style={{ color: '#64748b', marginLeft: -8 }}
            >
              返回
            </Button>
          ) : (
            <>
              <ToolOutlined style={{ fontSize: 18, color: '#2563eb' }} />
              <Text strong style={{ fontSize: 15, color: '#0f172a' }}>师傅工作台</Text>
            </>
          )}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Text style={{ fontSize: 13, color: '#64748b' }}>{user.realName || user.username}</Text>
          <Button
            type="text"
            danger
            size="small"
            icon={<LogoutOutlined />}
            onClick={handleLogout}
          >
            退出
          </Button>
        </div>
      </div>

      {/* 内容区域 */}
      <div style={{ padding: isDetailPage ? 0 : '12px 16px' }}>
        <Outlet />
      </div>
    </div>
  );
};

export default TechnicianLayout;
