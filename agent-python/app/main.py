"""
AI 售后维修问答代理服务入口。
"""
import logging

from fastapi import FastAPI

from app.core.config import settings
from app.schemas import (
    ChatRequest,
    ChatResponse,
    DeleteItemRequest,
    FaqContext,
    SearchRequest,
    SearchResponse,
    SearchResultItem,
    SyncItemRequest,
)
from app.services import llm_service, vector_service

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)

logger = logging.getLogger(__name__)

app = FastAPI(
    title="Repair AI Agent",
    description="AI 售后维修问答代理服务",
    version="0.2.0",
)


@app.on_event("startup")
async def startup():
    llm_mode = "MOCK" if settings.mock_mode else f"LIVE ({settings.llm_model})"
    emb_mode = "MOCK" if settings.embedding_mock_mode else f"LIVE ({settings.embedding_model})"
    logger.info("Agent started | LLM: %s | Embedding: %s", llm_mode, emb_mode)
    # 初始化 Qdrant collection
    try:
        vector_service.init_collection()
    except Exception as e:
        logger.error("Failed to init Qdrant collection: %s", e)


# ---------- Health ----------

@app.get("/health")
async def health():
    qdrant_ok = False
    try:
        client = vector_service.get_client()
        client.get_collections()
        qdrant_ok = True
    except Exception:
        pass

    return {
        "status": "ok" if qdrant_ok else "degraded",
        "mode": "mock" if settings.mock_mode else "live",
        "model": settings.llm_model,
        "embeddingMode": "mock" if settings.embedding_mock_mode else "live",
        "qdrant": "connected" if qdrant_ok else "disconnected",
    }


# ---------- Chat（改造：优先向量检索） ----------

@app.post("/agent/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    logger.info(
        "Chat request: questionLen=%d, tenantId=%s, productType=%s, faultType=%s, traceId=%s",
        len(req.question), req.tenantId, req.productType, req.faultType, req.traceId,
    )

    # 优先：如果传了 tenantId，做向量搜索
    if req.tenantId is not None:
        search_results = vector_service.search(
            tenant_id=req.tenantId,
            question=req.question,
            product_type=req.productType,
            fault_type=req.faultType,
            top_k=req.topK,
        )
        # 如果带 product_type/fault_type 没搜到，放宽条件再搜一次
        if not search_results and (req.productType or req.faultType):
            logger.info("Retrying search without product/fault filter")
            search_results = vector_service.search(
                tenant_id=req.tenantId,
                question=req.question,
                top_k=req.topK,
            )
        contexts = [
            FaqContext(title=r["title"], question=r["question"], answer=r["answer"])
            for r in search_results
        ]
    else:
        # 向后兼容：如果没有 tenantId，使用请求中携带的 contexts
        contexts = req.contexts

    response = llm_service.chat(
        question=req.question,
        contexts=contexts,
        trace_id=req.traceId,
    )
    response.matchedItemCount = len(contexts)

    logger.info(
        "Chat response: model=%s, shouldCreateTicket=%s, matched=%d, traceId=%s",
        response.model, response.shouldCreateTicket, response.matchedItemCount, response.traceId,
    )
    return response


# ---------- Knowledge Sync ----------

@app.post("/agent/knowledge/sync")
async def knowledge_sync(req: SyncItemRequest):
    logger.info(
        "Knowledge sync: tenantId=%d, itemId=%d, status=%s",
        req.tenantId, req.itemId, req.status,
    )
    ok = vector_service.sync_item(
        tenant_id=req.tenantId,
        item_id=req.itemId,
        knowledge_base_id=req.knowledgeBaseId,
        title=req.title,
        question=req.question,
        answer=req.answer,
        product_type=req.productType,
        fault_type=req.faultType,
        status=req.status,
    )
    return {"success": ok}


# ---------- Knowledge Delete ----------

@app.post("/agent/knowledge/delete")
async def knowledge_delete(req: DeleteItemRequest):
    logger.info("Knowledge delete: tenantId=%d, itemId=%d", req.tenantId, req.itemId)
    ok = vector_service.delete_item(
        tenant_id=req.tenantId,
        item_id=req.itemId,
    )
    return {"success": ok}


# ---------- Search ----------

@app.post("/agent/search", response_model=SearchResponse)
async def search(req: SearchRequest):
    logger.info(
        "Search request: tenantId=%d, questionLen=%d, productType=%s, faultType=%s, topK=%d",
        req.tenantId, len(req.question), req.productType, req.faultType, req.topK,
    )
    items = vector_service.search(
        tenant_id=req.tenantId,
        question=req.question,
        product_type=req.productType,
        fault_type=req.faultType,
        top_k=req.topK,
    )
    return SearchResponse(
        items=[SearchResultItem(**item) for item in items],
        traceId=req.traceId,
    )
