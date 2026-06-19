import React from 'react';
import { Button, Card, Space, Typography } from 'antd';
import {
  BookOutlined,
  FileTextOutlined,
  UploadOutlined,
  RobotOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const { Title } = Typography;

const cards = [
  { title: '知识库管理', icon: <BookOutlined style={{ fontSize: 36 }} />, path: '/admin/knowledge-bases', desc: '管理 FAQ 知识库' },
  { title: '知识条目', icon: <FileTextOutlined style={{ fontSize: 36 }} />, path: '/admin/knowledge-items', desc: '管理 FAQ 条目内容' },
  { title: '文档上传', icon: <UploadOutlined style={{ fontSize: 36 }} />, path: '/admin/documents', desc: '上传文档自动解析' },
  { title: 'AI 对话', icon: <RobotOutlined style={{ fontSize: 36 }} />, path: '/admin/ai-conversations', desc: '查看 AI 问答记录' },
];

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div>
      <Title level={3}>仪表盘</Title>
      <p style={{ marginBottom: 24, color: '#666' }}>
        欢迎使用 Repair AI SaaS 管理后台
      </p>
      <Space size="large" wrap>
        {cards.map((c) => (
          <Card
            key={c.path}
            hoverable
            style={{ width: 220, textAlign: 'center' }}
            onClick={() => navigate(c.path)}
          >
            <Space direction="vertical" size="middle">
              {c.icon}
              <div>
                <div style={{ fontWeight: 600 }}>{c.title}</div>
                <div style={{ color: '#999', fontSize: 12 }}>{c.desc}</div>
              </div>
              <Button type="primary" size="small">进入</Button>
            </Space>
          </Card>
        ))}
      </Space>
    </div>
  );
};

export default DashboardPage;
