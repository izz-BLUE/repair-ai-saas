import React, { useEffect, useState } from 'react';
import { Card, Form, Input, Switch, Button, Space, ColorPicker, App } from 'antd';
import { SaveOutlined, LinkOutlined } from '@ant-design/icons';
import { getSettings, updateSettings, type TenantSettings } from '../api/settings';

const THEME_PRESETS = [
  { label: '青色（默认）', value: '#0d9488' },
  { label: '蓝色', value: '#2563eb' },
  { label: '靛蓝', value: '#4f46e5' },
  { label: '紫色', value: '#7c3aed' },
  { label: '玫瑰', value: '#e11d48' },
  { label: '橙色', value: '#ea580c' },
];

const TenantSettingsPage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [tenantCode, setTenantCode] = useState('');
  const [tenantName, setTenantName] = useState('');
  const { message } = App.useApp();

  useEffect(() => {
    setLoading(true);
    getSettings()
      .then((data: TenantSettings) => {
        setTenantCode(data.tenantCode);
        setTenantName(data.name);
        form.setFieldsValue({
          portalTitle: data.portalTitle,
          portalDescription: data.portalDescription,
          contactPhone: data.contactPhone,
          logoUrl: data.logoUrl,
          themeColor: data.themeColor || '#0d9488',
          portalEnabled: data.portalEnabled,
        });
      })
      .finally(() => setLoading(false));
  }, [form]);

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      const themeColor = typeof values.themeColor === 'string'
        ? values.themeColor
        : values.themeColor?.toHexString?.() || '#0d9488';
      await updateSettings({
        ...values,
        themeColor,
      });
      message.success('保存成功');
    } catch {
      // validation error
    } finally {
      setSaving(false);
    }
  };

  const portalUrl = tenantCode ? `${window.location.origin}/portal/${tenantCode}` : '';

  return (
    <div style={{ maxWidth: 720 }}>
      <div style={{ marginBottom: 24 }}>
        <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600, color: '#0f172a' }}>
          企业设置
        </h2>
        <p style={{ margin: '4px 0 0', color: '#64748b', fontSize: 14 }}>
          配置企业门户的品牌信息和功能开关
        </p>
      </div>

      <Card loading={loading} style={{ marginBottom: 16 }}>
        <div style={{ marginBottom: 16, padding: '12px 16px', background: '#f8fafc', borderRadius: 8, border: '1px solid #e2e8f0' }}>
          <div style={{ fontSize: 14, color: '#475569' }}>
            <span style={{ color: '#94a3b8' }}>企业名称：</span>
            {tenantName || '-'}
          </div>
          <div style={{ fontSize: 14, color: '#475569', marginTop: 4 }}>
            <span style={{ color: '#94a3b8' }}>租户编码：</span>
            {tenantCode || '-'}
          </div>
          {portalUrl && (
            <div style={{ fontSize: 14, color: '#475569', marginTop: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
              <span style={{ color: '#94a3b8' }}>门户地址：</span>
              <a href={portalUrl} target="_blank" rel="noopener noreferrer" style={{ color: '#2563eb' }}>
                {portalUrl}
              </a>
              <LinkOutlined style={{ color: '#94a3b8', fontSize: 12 }} />
            </div>
          )}
        </div>

        <Form
          form={form}
          layout="vertical"
          initialValues={{
            portalEnabled: true,
            themeColor: '#0d9488',
          }}
        >
          <Form.Item label="门户标题" name="portalTitle">
            <Input placeholder="例如：XX 维修售后服务" maxLength={100} showCount />
          </Form.Item>

          <Form.Item label="门户描述" name="portalDescription">
            <Input.TextArea
              placeholder="简短描述您的服务，会显示在门户首页"
              maxLength={500}
              showCount
              rows={3}
            />
          </Form.Item>

          <Form.Item label="联系电话" name="contactPhone">
            <Input placeholder="例如：400-888-8888" />
          </Form.Item>

          <Form.Item label="Logo URL" name="logoUrl" extra="填写 Logo 图片链接，留空使用默认图标">
            <Input placeholder="https://example.com/logo.png" />
          </Form.Item>

          <Form.Item label="门户主题色" name="themeColor">
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
              <ColorPicker
                format="hex"
                presets={[
                  {
                    label: '推荐色',
                    colors: THEME_PRESETS.map(p => p.value),
                  },
                ]}
              />
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                {THEME_PRESETS.map(p => (
                  <div
                    key={p.value}
                    title={p.label}
                    style={{
                      width: 24,
                      height: 24,
                      borderRadius: 6,
                      background: p.value,
                      cursor: 'pointer',
                      border: '2px solid transparent',
                      transition: 'border-color 0.2s',
                    }}
                    onClick={() => form.setFieldValue('themeColor', p.value)}
                  />
                ))}
              </div>
            </div>
          </Form.Item>

          <Form.Item
            label="启用门户"
            name="portalEnabled"
            valuePropName="checked"
            extra="关闭后客户无法访问门户，AI 客服和报修功能暂停"
          >
            <Switch />
          </Form.Item>
        </Form>
      </Card>

      <div style={{ textAlign: 'right' }}>
        <Space>
          <Button onClick={() => form.resetFields()}>重置</Button>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            loading={saving}
            onClick={handleSave}
          >
            保存设置
          </Button>
        </Space>
      </div>
    </div>
  );
};

export default TenantSettingsPage;
