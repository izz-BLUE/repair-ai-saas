"""
LLM 服务：调用 DeepSeek / OpenAI 兼容 API。
无 API key 时自动降级为 mock 模式。
"""
from __future__ import annotations

import logging

from openai import OpenAI

from app.core.config import settings
from app.schemas import ChatResponse, FaqContext

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """你是售后维修 AI 助手。
你的职责是根据提供的 FAQ 知识库内容，回答客户的维修相关问题。

规则：
1. 只能基于给定的 FAQ 上下文回答，不要编造知识库没有的信息。
2. 如果 FAQ 上下文不足以回答问题，明确告知客户无法判断，并建议提交报修。
3. 涉及以下危险操作时，必须提示联系专业师傅：拆机、电路维修、燃气设备、高空作业、制冷剂添加。
4. 回答要简洁、实用，适合普通客户理解。
5. 使用简体中文回答。"""


def _build_user_message(question: str, contexts: list[FaqContext]) -> str:
    """将 FAQ 上下文和用户问题拼接为 LLM 输入。"""
    parts = ["以下是知识库中与该问题可能相关的 FAQ 条目：\n"]
    for i, ctx in enumerate(contexts, 1):
        parts.append(f"【{i}】{ctx.title}")
        parts.append(f"   问题：{ctx.question}")
        parts.append(f"   解答：{ctx.answer}")
        parts.append("")
    parts.append(f"客户问题：{question}")
    parts.append("\n请根据以上 FAQ 内容回答客户问题。如果 FAQ 内容不足以回答，请明确说明并建议客户提交报修。")
    return "\n".join(parts)


def _mock_chat(question: str, contexts: list[FaqContext], trace_id: str | None) -> ChatResponse:
    """无 API key 时的 mock 响应。"""
    logger.info("MOCK MODE: returning fallback answer (no LLM API key configured)")
    if not contexts:
        return ChatResponse(
            answer="当前知识库中没有足够信息判断该问题。建议提交报修，由客服或维修师傅进一步确认。",
            model="MOCK",
            shouldCreateTicket=True,
            traceId=trace_id,
        )

    answer_lines = ["根据知识库信息，以下内容可能与您的问题相关：\n"]
    for i, ctx in enumerate(contexts, 1):
        answer_lines.append(f"{i}. {ctx.title}")
        answer_lines.append(f"   问题：{ctx.question}")
        answer_lines.append(f"   解答：{ctx.answer}")
        answer_lines.append("")
    answer_lines.append("以上为知识库自动匹配结果（Mock 模式），如需进一步帮助请联系客服或提交报修。")

    return ChatResponse(
        answer="\n".join(answer_lines),
        model="MOCK",
        shouldCreateTicket=False,
        traceId=trace_id,
    )


def chat(question: str, contexts: list[FaqContext], trace_id: str | None = None) -> ChatResponse:
    """调用 LLM 或 mock 模式回答问题。"""
    if settings.mock_mode:
        return _mock_chat(question, contexts, trace_id)

    try:
        client = OpenAI(
            api_key=settings.llm_api_key,
            base_url=settings.llm_base_url,
        )

        user_msg = _build_user_message(question, contexts)

        completion = client.chat.completions.create(
            model=settings.llm_model,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_msg},
            ],
            temperature=0.3,
            max_tokens=1024,
        )

        answer = completion.choices[0].message.content or ""
        model_used = completion.model or settings.llm_model

        # 判断是否建议报修（简单启发式）
        should_ticket = any(
            kw in answer
            for kw in ["建议报修", "提交报修", "联系客服", "联系师傅", "无法判断", "无法确定"]
        )

        return ChatResponse(
            answer=answer,
            model=model_used,
            shouldCreateTicket=should_ticket,
            traceId=trace_id,
        )
    except Exception as e:
        logger.error("LLM call failed: %s", e)
        # 调用失败时降级为 mock
        return _mock_chat(question, contexts, trace_id)
