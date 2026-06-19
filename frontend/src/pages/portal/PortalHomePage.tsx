import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { RobotOutlined, FormOutlined, SearchOutlined, ArrowRightOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { usePortalConfig, useThemeColor } from '../../contexts/PortalConfigContext';

const PortalHomePage: React.FC = () => {
  const { tenantCode } = useParams<{ tenantCode: string }>();
  const navigate = useNavigate();
  const [question, setQuestion] = useState('');
  const config = usePortalConfig();
  const themeColor = useThemeColor();

  const basePath = `/portal/${tenantCode}`;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (question.trim()) {
      navigate(`${basePath}/chat`, { state: { initialQuestion: question.trim() } });
    }
  };

  const handleChipClick = (text: string) => {
    navigate(`${basePath}/chat`, { state: { initialQuestion: text } });
  };

  const actions = [
    {
      icon: <RobotOutlined />,
      label: 'AI 智能客服',
      desc: '输入问题，AI 即时解答',
      color: themeColor,
      bg: `${themeColor}10`,
      path: '/chat',
    },
    {
      icon: <FormOutlined />,
      label: '提交报修',
      desc: '填写信息，快速安排维修',
      color: '#f97316',
      bg: '#fff7ed',
      path: '/repair',
    },
    {
      icon: <SearchOutlined />,
      label: '查询进度',
      desc: '输入工单号查看处理状态',
      color: '#2563eb',
      bg: '#eff6ff',
      path: '/query',
    },
  ];

  const suggestions = [
    '空调不制冷怎么办',
    '洗衣机漏水',
    '冰箱有异味如何处理',
    '热水器温度不稳定',
  ];

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '40px 20px' }}>
      {/* 主输入区 */}
      <div style={{ textAlign: 'center', marginBottom: 48, maxWidth: 560, width: '100%' }}>
        <div style={{
          width: 64, height: 64, borderRadius: 20, margin: '0 auto 20px',
          background: `linear-gradient(135deg, ${themeColor}, ${themeColor}dd)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: `0 8px 24px ${themeColor}40`,
        }}>
          <RobotOutlined style={{ fontSize: 28, color: '#fff' }} />
        </div>

        <h1 style={{
          fontSize: 28, fontWeight: 700, color: '#0f172a', margin: '0 0 8px',
          letterSpacing: -0.5, lineHeight: 1.3,
        }}>
          {config.portalTitle || '需要帮助？'}
        </h1>
        <p style={{ fontSize: 15, color: '#64748b', margin: '0 0 32px', lineHeight: 1.6 }}>
          {config.portalDescription || '描述您遇到的问题，AI 先尝试解答。无法解决时，我们帮您安排维修。'}
        </p>

        {/* 搜索/提问框 */}
        <form onSubmit={handleSubmit} style={{ position: 'relative', marginBottom: 20 }}>
          <input
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="描述您的问题，例如：空调不制冷..."
            style={{
              width: '100%', height: 56, borderRadius: 16,
              border: '2px solid #e2e8f0', padding: '0 56px 0 20px',
              fontSize: 15, outline: 'none', background: '#fff',
              color: '#0f172a', boxSizing: 'border-box',
              transition: 'border-color 0.2s, box-shadow 0.2s',
            }}
            onFocus={(e) => {
              e.target.style.borderColor = themeColor;
              e.target.style.boxShadow = `0 0 0 3px ${themeColor}1a`;
            }}
            onBlur={(e) => {
              e.target.style.borderColor = '#e2e8f0';
              e.target.style.boxShadow = 'none';
            }}
          />
          <button
            type="submit"
            disabled={!question.trim()}
            style={{
              position: 'absolute', right: 8, top: 8,
              width: 40, height: 40, borderRadius: 12,
              border: 'none', cursor: question.trim() ? 'pointer' : 'default',
              background: question.trim() ? themeColor : '#e2e8f0',
              color: '#fff', fontSize: 16,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              transition: 'all 0.2s',
            }}
          >
            <ArrowRightOutlined />
          </button>
        </form>

        {/* 快捷提问 */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, justifyContent: 'center' }}>
          {suggestions.map((s) => (
            <button
              key={s}
              onClick={() => handleChipClick(s)}
              style={{
                padding: '6px 14px', borderRadius: 20,
                border: '1px solid #e2e8f0', background: '#fff',
                color: '#64748b', fontSize: 13, cursor: 'pointer',
                transition: 'all 0.2s',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.borderColor = themeColor;
                e.currentTarget.style.color = themeColor;
                e.currentTarget.style.background = `${themeColor}10`;
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.borderColor = '#e2e8f0';
                e.currentTarget.style.color = '#64748b';
                e.currentTarget.style.background = '#fff';
              }}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      {/* 三个服务入口 */}
      <div style={{
        display: 'flex', gap: 16, maxWidth: 720, width: '100%',
        flexWrap: 'wrap', justifyContent: 'center',
      }}>
        {actions.map((a) => (
          <div
            key={a.label}
            onClick={() => navigate(basePath + a.path)}
            style={{
              flex: '1 1 200px', maxWidth: 220, padding: '20px',
              borderRadius: 16, background: '#fff',
              border: '1px solid #e2e8f0',
              cursor: 'pointer', transition: 'all 0.2s',
              textAlign: 'center',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.borderColor = a.color;
              e.currentTarget.style.boxShadow = `0 4px 16px ${a.color}18`;
              e.currentTarget.style.transform = 'translateY(-2px)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.borderColor = '#e2e8f0';
              e.currentTarget.style.boxShadow = 'none';
              e.currentTarget.style.transform = 'translateY(0)';
            }}
          >
            <div style={{
              width: 44, height: 44, borderRadius: 12, margin: '0 auto 12px',
              background: a.bg, display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 20, color: a.color,
            }}>
              {a.icon}
            </div>
            <div style={{ fontWeight: 600, fontSize: 15, color: '#0f172a', marginBottom: 4 }}>{a.label}</div>
            <div style={{ fontSize: 12, color: '#94a3b8' }}>{a.desc}</div>
          </div>
        ))}
      </div>

      {/* 流程说明 */}
      <div style={{
        marginTop: 48, display: 'flex', alignItems: 'center', gap: 12,
        padding: '12px 24px', borderRadius: 12, background: `${themeColor}10`,
        border: `1px solid ${themeColor}30`,
      }}>
        <ThunderboltOutlined style={{ color: themeColor, fontSize: 16 }} />
        <span style={{ fontSize: 13, color: `${themeColor}cc`, fontWeight: 500 }}>
          AI 优先解答常见问题，无法解决时自动引导您提交报修，工程师快速响应
        </span>
      </div>
    </div>
  );
};

export default PortalHomePage;
