import React, { useState, useEffect } from 'react';
import { Layout, Menu, Button, theme, Avatar, Dropdown, Typography } from 'antd';
import {
  ShopOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  LogoutOutlined,
  UserOutlined,
  KeyOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import ChangePasswordModal from '../components/ChangePasswordModal';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

const menuItems = [
  { key: '/platform/tenants', icon: <ShopOutlined />, label: '租户管理' },
];

const PlatformLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [changePwdOpen, setChangePwdOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { token: { colorBgContainer, borderRadiusLG } } = theme.useToken();

  const user = JSON.parse(localStorage.getItem('user') || '{}');

  useEffect(() => {
    const token = localStorage.getItem('token');
    const role = JSON.parse(localStorage.getItem('user') || '{}').role;
    if (!token || role !== 'SUPER_ADMIN') {
      navigate('/admin/login');
    }
  }, [navigate]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/admin/login');
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        width={224}
        style={{
          background: '#0f172a',
          boxShadow: '2px 0 8px rgba(0,0,0,0.08)',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          zIndex: 10,
          overflow: 'auto',
        }}
      >
        <div style={{
          height: 64,
          display: 'flex',
          alignItems: 'center',
          justifyContent: collapsed ? 'center' : 'flex-start',
          padding: collapsed ? 0 : '0 20px',
          borderBottom: '1px solid rgba(255,255,255,0.06)',
        }}>
          <ShopOutlined style={{ fontSize: collapsed ? 20 : 22, color: '#f59e0b' }} />
          {!collapsed && (
            <span style={{ color: '#fff', fontSize: 16, fontWeight: 700, marginLeft: 10, letterSpacing: -0.5 }}>
              平台管理
            </span>
          )}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ borderRight: 0, background: 'transparent', padding: '8px 0' }}
        />
      </Sider>
      <Layout style={{ marginLeft: collapsed ? 80 : 224, transition: 'margin-left 0.2s' }}>
        <Header style={{
          padding: '0 24px',
          background: colorBgContainer,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid #e2e8f0',
          height: 56,
          position: 'sticky',
          top: 0,
          zIndex: 5,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              style={{ color: '#64748b' }}
            />
            <Text style={{ fontSize: 15, fontWeight: 600, color: '#0f172a' }}>租户管理</Text>
          </div>
          <Dropdown
            menu={{
              items: [
                { key: 'changePwd', icon: <KeyOutlined />, label: '修改密码', onClick: () => setChangePwdOpen(true) },
                { type: 'divider' as const },
                { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: handleLogout },
              ],
            }}
            placement="bottomRight"
          >
            <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8, padding: '4px 8px', borderRadius: 8 }}>
              <Avatar size={32} icon={<UserOutlined />} style={{ background: '#f59e0b' }} />
              <div style={{ lineHeight: 1.2 }}>
                <div style={{ fontSize: 13, fontWeight: 500, color: '#0f172a' }}>{user.realName || user.username}</div>
                <div style={{ fontSize: 11, color: '#94a3b8' }}>SUPER_ADMIN</div>
              </div>
            </div>
          </Dropdown>
        </Header>
        <Content style={{
          margin: 20,
          padding: 24,
          background: colorBgContainer,
          borderRadius: borderRadiusLG,
          minHeight: 280,
          border: '1px solid #e2e8f0',
        }}>
          <Outlet />
        </Content>
      </Layout>
      <ChangePasswordModal open={changePwdOpen} onClose={() => setChangePwdOpen(false)} />
    </Layout>
  );
};

export default PlatformLayout;
