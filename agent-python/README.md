# AI 售后维修问答代理服务

基于 FastAPI 的轻量 AI 问答代理，为 repair-ai-saas 后端提供 LLM 能力。

## 技术栈

- Python 3.10+
- FastAPI
- OpenAI-compatible SDK（支持 DeepSeek / OpenAI）
- Pydantic Settings

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `LLM_API_KEY` | (空) | LLM API Key，为空时自动进入 Mock 模式 |
| `LLM_BASE_URL` | `https://api.deepseek.com` | LLM API 地址 |
| `LLM_MODEL` | `deepseek-chat` | 模型名称 |
| `AGENT_PORT` | `8090` | 服务端口 |

## 快速启动

```bash
cd agent-python

# 安装依赖
pip install -e .

# Mock 模式（无需 API Key）
uvicorn app.main:app --host 0.0.0.0 --port 8090

# Live 模式（需要 API Key）
LLM_API_KEY=sk-xxx uvicorn app.main:app --host 0.0.0.0 --port 8090
```

## 接口

### POST /agent/chat

请求：
```json
{
  "question": "空调不制冷怎么办？",
  "contexts": [
    {
      "title": "空调不制冷常见原因",
      "question": "空调不制冷是什么原因？",
      "answer": "检查外机运转、制冷剂、过滤网。"
    }
  ],
  "tenantId": 1,
  "traceId": "abc123"
}
```

响应：
```json
{
  "answer": "根据知识库信息...",
  "model": "deepseek-chat",
  "shouldCreateTicket": false,
  "traceId": "abc123"
}
```

### GET /health

健康检查，返回服务状态和当前模式（mock/live）。
