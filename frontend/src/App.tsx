import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, App as AntdApp } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { setMessageApi } from './api/http';
import AdminLayout from './layouts/AdminLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import KnowledgeBasePage from './pages/KnowledgeBasePage';
import KnowledgeItemPage from './pages/KnowledgeItemPage';
import DocumentPage from './pages/DocumentPage';
import AiConversationPage from './pages/AiConversationPage';
import TenantSettingsPage from './pages/TenantSettingsPage';
import TicketPage from './pages/TicketPage';
import TechnicianLayout from './layouts/TechnicianLayout';
import TechnicianTicketsPage from './pages/technician/TechnicianTicketsPage';
import TechnicianTicketDetailPage from './pages/technician/TechnicianTicketDetailPage';
import PlatformLayout from './layouts/PlatformLayout';
import PlatformTenantsPage from './pages/platform/PlatformTenantsPage';
import PortalLayout from './layouts/PortalLayout';
import PortalHomePage from './pages/portal/PortalHomePage';
import PortalChatPage from './pages/portal/PortalChatPage';
import PortalRepairPage from './pages/portal/PortalRepairPage';
import PortalQueryPage from './pages/portal/PortalQueryPage';

const theme = {
  token: {
    colorPrimary: '#2563eb',
    colorSuccess: '#059669',
    colorWarning: '#d97706',
    colorError: '#dc2626',
    colorBgLayout: '#f1f5f9',
    colorBgContainer: '#ffffff',
    borderRadius: 8,
    fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif",
    fontSize: 14,
    colorText: '#0f172a',
    colorTextSecondary: '#64748b',
    colorBorder: '#e2e8f0',
  },
  components: {
    Table: {
      headerBg: '#f8fafc',
      headerColor: '#475569',
      rowHoverBg: '#f1f5f9',
      borderColor: '#e2e8f0',
    },
    Menu: {
      darkItemSelectedBg: 'rgba(37, 99, 235, 0.25)',
      darkItemSelectedColor: '#60a5fa',
      darkItemColor: 'rgba(255,255,255,0.65)',
      darkSubMenuItemBg: '#0f172a',
    },
    Card: {
      headerBg: 'transparent',
    },
  },
};

/** 将 antd App 的 message 实例注入到 http 模块，消除静态 message 警告 */
const MessageBridge: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { message } = AntdApp.useApp();
  useEffect(() => { setMessageApi(message); }, [message]);
  return <>{children}</>;
};

const App: React.FC = () => {
  return (
    <ConfigProvider locale={zhCN} theme={theme}>
      <AntdApp>
        <MessageBridge>
        <BrowserRouter>
          <Routes>
            <Route path="/admin/login" element={<LoginPage />} />
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<Navigate to="/admin/dashboard" replace />} />
              <Route path="dashboard" element={<DashboardPage />} />
              <Route path="knowledge-bases" element={<KnowledgeBasePage />} />
              <Route path="knowledge-items" element={<KnowledgeItemPage />} />
              <Route path="documents" element={<DocumentPage />} />
              <Route path="ai-conversations" element={<AiConversationPage />} />
              <Route path="tickets" element={<TicketPage />} />
              <Route path="settings" element={<TenantSettingsPage />} />
            </Route>
            <Route path="/platform" element={<PlatformLayout />}>
              <Route index element={<Navigate to="/platform/tenants" replace />} />
              <Route path="tenants" element={<PlatformTenantsPage />} />
            </Route>
            <Route path="/portal/:tenantCode" element={<PortalLayout />}>
              <Route index element={<PortalHomePage />} />
              <Route path="chat" element={<PortalChatPage />} />
              <Route path="repair" element={<PortalRepairPage />} />
              <Route path="query" element={<PortalQueryPage />} />
            </Route>
            <Route path="/technician" element={<TechnicianLayout />}>
              <Route index element={<Navigate to="/technician/tickets" replace />} />
              <Route path="tickets" element={<TechnicianTicketsPage />} />
              <Route path="tickets/:id" element={<TechnicianTicketDetailPage />} />
            </Route>
            <Route path="*" element={<Navigate to="/admin/login" replace />} />
          </Routes>
        </BrowserRouter>
        </MessageBridge>
      </AntdApp>
    </ConfigProvider>
  );
};

export default App;
