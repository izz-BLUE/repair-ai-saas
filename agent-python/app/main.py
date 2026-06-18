"""
AI 售后维修问答代理服务入口。
"""
import logging

from fastapi import FastAPI

from app.core.config import settings
from app.schemas import ChatRequest, ChatResponse
from app.services import llm_service

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)

logger = logging.getLogger(__name__)

app = FastAPI(
    title="Repair AI Agent",
    description="AI 售后维修问答代理服务",
    version="0.1.0",
)


@app.on_event("startup")
async def startup():
    mode = "MOCK" if settings.mock_mode else f"LIVE ({settings.llm_model})"
    logger.info("Agent started in %s mode", mode)


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "mode": "mock" if settings.mock_mode else "live",
        "model": settings.llm_model,
    }


@app.post("/agent/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    logger.info(
        "Chat request: question='%s', contexts=%d, tenantId=%s, traceId=%s",
        req.question[:50], len(req.contexts), req.tenantId, req.traceId,
    )

    response = llm_service.chat(
        question=req.question,
        contexts=req.contexts,
        trace_id=req.traceId,
    )

    logger.info(
        "Chat response: model=%s, shouldCreateTicket=%s, traceId=%s",
        response.model, response.shouldCreateTicket, response.traceId,
    )
    return response
