import React, { useState, useRef, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { RobotOutlined, SendOutlined, UserOutlined, FormOutlined, BookOutlined, LoadingOutlined } from '@ant-design/icons';
import { aiChat, type AiChatResponse } from '../../api/portal';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  meta?: AiChatResponse;
  timestamp: number;
}

const PortalChatPage: React.FC = () => {
  const { tenantCode } = useParams<{ tenantCode: string }>();
  const navigate = useNavigate();
  const location = useLocation();

  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const basePath = `/portal/${tenantCode}`;

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => { scrollToBottom(); }, [messages]);

  // 从首页带入初始问题
  useEffect(() => {
    const initialQuestion = (location.state as { initialQuestion?: string })?.initialQuestion;
    if (initialQuestion) {
      navigate(location.pathname, { replace: true, state: {} });
      sendMessage(initialQuestion);
    }
  }, []);

  const sendMessage = async (text: string) => {
    const question = text.trim();
    if (!question || loading) return;

    const userMsg: Message = { role: 'user', content: question, timestamp: Date.now() };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const res = await aiChat(tenantCode!, { question });
      const aiMsg: Message = {
        role: 'assistant',
        content: res.answer,
        meta: res,
        timestamp: Date.now(),
      };
      setMessages((prev) => [...prev, aiMsg]);
    } catch {
      const errMsg: Message = {
        role: 'assistant',
        content: '抱歉，服务暂时不可用，请稍后再试。如果问题紧急，您可以直接提交报修。',
        timestamp: Date.now(),
      };
      setMessages((prev) => [...prev, errMsg]);
    } finally {
      setLoading(false);
      inputRef.current?.focus();
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    sendMessage(input);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage(input);
    }
  };

  const quickQuestions = [
    '空调不制冷怎么办',
    '洗衣机漏水如何处理',
    '设备保修期多久',
  ];

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', height: 'calc(100vh - 56px)', background: '#f8fafc' }}>
      {/* 聊天消息区 */}
      <div style={{ flex: 1, overflow: 'auto', padding: '20px 16px' }}>
        <div style={{ maxWidth: 680, margin: '0 auto' }}>
          {/* 空状态：欢迎消息 */}
          {messages.length === 0 && (
            <div style={{ textAlign: 'center', padding: '60px 20px 40px' }}>
              <div style={{
                width: 72, height: 72, borderRadius: 24, margin: '0 auto 20px',
                background: 'linear-gradient(135deg, #0d9488, #0f766e)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                boxShadow: '0 8px 24px rgba(13,148,136,0.2)',
              }}>
                <RobotOutlined style={{ fontSize: 32, color: '#fff' }} />
              </div>
              <h2 style={{ fontSize: 20, fontWeight: 700, color: '#0f172a', margin: '0 0 8px' }}>
                你好，我是 AI 客服 👋
              </h2>
              <p style={{ fontSize: 14, color: '#64748b', margin: '0 0 32px', lineHeight: 1.6 }}>
                描述您遇到的问题，我会先尝试从知识库中为您解答。<br />
                如果无法解决，我会引导您提交报修。
              </p>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, justifyContent: 'center' }}>
                {quickQuestions.map((q) => (
                  <button
                    key={q}
                    onClick={() => sendMessage(q)}
                    style={{
                      padding: '8px 16px', borderRadius: 20,
                      border: '1px solid #e2e8f0', background: '#fff',
                      color: '#475569', fontSize: 13, cursor: 'pointer',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.borderColor = '#0d9488';
                      e.currentTarget.style.color = '#0d9488';
                      e.currentTarget.style.background = '#f0fdfa';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.borderColor = '#e2e8f0';
                      e.currentTarget.style.color = '#475569';
                      e.currentTarget.style.background = '#fff';
                    }}
                  >
                    {q}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* 消息列表 */}
          {messages.map((msg, i) => (
            <div key={i} style={{
              display: 'flex',
              flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
              gap: 10, marginBottom: 16, alignItems: 'flex-start',
            }}>
              {/* 头像 */}
              <div style={{
                width: 36, height: 36, borderRadius: 12, flexShrink: 0,
                background: msg.role === 'user' ? '#0d9488' : '#f0fdfa',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                border: msg.role === 'user' ? 'none' : '1px solid #ccfbf1',
              }}>
                {msg.role === 'user'
                  ? <UserOutlined style={{ color: '#fff', fontSize: 16 }} />
                  : <RobotOutlined style={{ color: '#0d9488', fontSize: 16 }} />
                }
              </div>

              {/* 消息气泡 */}
              <div style={{ maxWidth: '80%', minWidth: 0 }}>
                <div style={{
                  padding: '12px 16px', borderRadius: 16,
                  background: msg.role === 'user' ? '#0d9488' : '#fff',
                  color: msg.role === 'user' ? '#fff' : '#0f172a',
                  fontSize: 14, lineHeight: 1.7, whiteSpace: 'pre-wrap', wordBreak: 'break-word',
                  borderTopLeftRadius: msg.role === 'assistant' ? 4 : 16,
                  borderTopRightRadius: msg.role === 'user' ? 4 : 16,
                  border: msg.role === 'assistant' ? '1px solid #e2e8f0' : 'none',
                  boxShadow: msg.role === 'assistant' ? '0 1px 3px rgba(0,0,0,0.04)' : 'none',
                }}>
                  {msg.content}
                </div>

                {/* AI 回复的附加信息 */}
                {msg.role === 'assistant' && msg.meta && (
                  <div style={{ marginTop: 8 }}>
                    {/* 知识库匹配提示 */}
                    {msg.meta.matchedItemCount > 0 && (
                      <div style={{
                        display: 'inline-flex', alignItems: 'center', gap: 4,
                        padding: '3px 10px', borderRadius: 12,
                        background: '#f0fdfa', border: '1px solid #ccfbf1',
                        fontSize: 11, color: '#0f766e', fontWeight: 500,
                      }}>
                        <BookOutlined />
                        已参考 {msg.meta.matchedItemCount} 条知识库资料
                      </div>
                    )}

                    {/* 建议报修提示 */}
                    {msg.meta.shouldCreateTicket && (
                      <div style={{
                        marginTop: 8, padding: '12px 16px', borderRadius: 12,
                        background: '#fff7ed', border: '1px solid #fed7aa',
                      }}>
                        <div style={{ fontSize: 13, color: '#9a3412', fontWeight: 600, marginBottom: 8 }}>
                          未能在知识库中找到匹配的解决方案
                        </div>
                        <button
                          onClick={() => navigate(`${basePath}/repair`)}
                          style={{
                            display: 'inline-flex', alignItems: 'center', gap: 6,
                            padding: '8px 16px', borderRadius: 10,
                            border: 'none', background: '#f97316', color: '#fff',
                            fontSize: 13, fontWeight: 600, cursor: 'pointer',
                          }}
                        >
                          <FormOutlined /> 提交报修
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))}

          {/* 加载中 */}
          {loading && (
            <div style={{
              display: 'flex', gap: 10, marginBottom: 16, alignItems: 'flex-start',
            }}>
              <div style={{
                width: 36, height: 36, borderRadius: 12, flexShrink: 0,
                background: '#f0fdfa', border: '1px solid #ccfbf1',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <RobotOutlined style={{ color: '#0d9488', fontSize: 16 }} />
              </div>
              <div style={{
                padding: '14px 18px', borderRadius: 16, borderTopLeftRadius: 4,
                background: '#fff', border: '1px solid #e2e8f0',
              }}>
                <LoadingOutlined style={{ color: '#0d9488', fontSize: 16 }} />
                <span style={{ marginLeft: 8, color: '#94a3b8', fontSize: 13 }}>正在思考...</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>
      </div>

      {/* 输入区 */}
      <div style={{
        borderTop: '1px solid #e2e8f0', background: '#fff',
        padding: '12px 16px',
      }}>
        <form
          onSubmit={handleSubmit}
          style={{
            maxWidth: 680, margin: '0 auto',
            display: 'flex', gap: 10, alignItems: 'flex-end',
          }}
        >
          <textarea
            ref={inputRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="输入您的问题..."
            rows={1}
            style={{
              flex: 1, borderRadius: 12, border: '1.5px solid #e2e8f0',
              padding: '10px 14px', fontSize: 14, resize: 'none',
              outline: 'none', color: '#0f172a', lineHeight: 1.5,
              minHeight: 42, maxHeight: 120,
              fontFamily: 'inherit',
            }}
          />
          <button
            type="submit"
            disabled={!input.trim() || loading}
            style={{
              width: 42, height: 42, borderRadius: 12,
              border: 'none', flexShrink: 0,
              background: input.trim() && !loading ? '#0d9488' : '#e2e8f0',
              color: '#fff', fontSize: 16, cursor: input.trim() && !loading ? 'pointer' : 'default',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              transition: 'background 0.2s',
            }}
          >
            <SendOutlined />
          </button>
        </form>
      </div>
    </div>
  );
};

export default PortalChatPage;
