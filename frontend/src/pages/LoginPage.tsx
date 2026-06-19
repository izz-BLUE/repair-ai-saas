import React, { useState } from 'react';
import { Form, Input, Button, Typography, App } from 'antd';
import { UserOutlined, LockOutlined, ShopOutlined, RobotOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { login, type LoginRequest } from '../api/auth';

const { Title, Text } = Typography;

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { message } = App.useApp();

  const onFinish = async (values: LoginRequest) => {
    setLoading(true);
    try {
      const result = await login(values);
      localStorage.setItem('token', result.token);
      localStorage.setItem('user', JSON.stringify(result));
      message.success(`欢迎回来，${result.realName || result.username}`);
      // SUPER_ADMIN 跳转平台管理，普通角色跳转管理后台
      if (result.role === 'SUPER_ADMIN') {
        navigate('/platform/tenants');
      } else {
        navigate('/admin/dashboard');
      }
    } catch {
      // http interceptor already shows error
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex' }}>
      {/* 左侧品牌面板 */}
      <div style={{
        flex: '0 0 45%',
        background: 'linear-gradient(135deg, #0f172a 0%, #1e3a5f 50%, #1e40af 100%)',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        padding: '60px 48px',
        position: 'relative',
        overflow: 'hidden',
      }}>
        {/* 几何装饰 */}
        <div style={{
          position: 'absolute', top: -80, right: -80, width: 300, height: 300,
          borderRadius: '50%', border: '1px solid rgba(96,165,250,0.15)',
        }} />
        <div style={{
          position: 'absolute', bottom: -60, left: -60, width: 200, height: 200,
          borderRadius: '50%', border: '1px solid rgba(96,165,250,0.1)',
        }} />
        <div style={{
          position: 'absolute', top: '50%', right: '15%',
          width: 80, height: 80, borderRadius: 12, transform: 'rotate(45deg)',
          border: '1px solid rgba(96,165,250,0.12)',
        }} />

        <RobotOutlined style={{ fontSize: 56, color: '#60a5fa', marginBottom: 24 }} />
        <Title level={2} style={{ color: '#fff', margin: 0, textAlign: 'center' }}>
          Repair AI SaaS
        </Title>
        <Text style={{ color: 'rgba(255,255,255,0.7)', fontSize: 16, marginTop: 12, textAlign: 'center', maxWidth: 320 }}>
          智能售后维修管理平台
        </Text>

        <div style={{ marginTop: 48, display: 'flex', gap: 32 }}>
          {[
            { label: '工单管理', desc: '状态机驱动' },
            { label: 'AI 问答', desc: 'RAG 知识库' },
            { label: '多租户', desc: '数据隔离' },
          ].map((f) => (
            <div key={f.label} style={{ textAlign: 'center' }}>
              <div style={{ color: '#60a5fa', fontSize: 13, fontWeight: 600 }}>{f.label}</div>
              <div style={{ color: 'rgba(255,255,255,0.5)', fontSize: 12, marginTop: 4 }}>{f.desc}</div>
            </div>
          ))}
        </div>
      </div>

      {/* 右侧登录表单 */}
      <div style={{
        flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center',
        background: '#f8fafc', padding: '48px',
      }}>
        <div style={{ width: '100%', maxWidth: 380 }}>
          <div style={{ marginBottom: 40 }}>
            <Title level={3} style={{ margin: 0, color: '#0f172a' }}>登录</Title>
            <Text style={{ color: '#64748b' }}>输入企业编码和账号密码进入后台</Text>
          </div>

          <Form
            name="login"
            onFinish={onFinish}
            size="large"
            layout="vertical"
            requiredMark={false}
          >
            <Form.Item
              name="tenantCode"
              rules={[{ required: true, message: '请输入企业编码' }]}
              label={<span style={{ color: '#334155', fontWeight: 500 }}>企业编码</span>}
            >
              <Input
                prefix={<ShopOutlined style={{ color: '#94a3b8' }} />}
                placeholder="例如：TL6L767"
                style={{ borderRadius: 8 }}
              />
            </Form.Item>
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
              label={<span style={{ color: '#334155', fontWeight: 500 }}>用户名</span>}
            >
              <Input
                prefix={<UserOutlined style={{ color: '#94a3b8' }} />}
                placeholder="请输入用户名"
                style={{ borderRadius: 8 }}
              />
            </Form.Item>
            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
              label={<span style={{ color: '#334155', fontWeight: 500 }}>密码</span>}
            >
              <Input.Password
                prefix={<LockOutlined style={{ color: '#94a3b8' }} />}
                placeholder="请输入密码"
                style={{ borderRadius: 8 }}
              />
            </Form.Item>
            <Form.Item style={{ marginTop: 32 }}>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                block
                style={{
                  height: 44,
                  borderRadius: 8,
                  fontWeight: 600,
                  fontSize: 15,
                  background: '#2563eb',
                  boxShadow: '0 2px 8px rgba(37,99,235,0.3)',
                }}
              >
                登录
              </Button>
            </Form.Item>
          </Form>

          <div style={{ textAlign: 'center', marginTop: 24 }}>
            <Text style={{ color: '#94a3b8', fontSize: 12 }}>
              企业维修管理 · 知识库 · AI 智能问答
            </Text>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
