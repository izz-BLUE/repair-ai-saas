"""
向量检索服务：Qdrant 客户端 + Embedding + CRUD + 搜索。
支持 mock 模式（无 API key 时基于 SHA-256 hash 生成确定性向量）。
"""
from __future__ import annotations

import hashlib
import logging
import math

from qdrant_client import QdrantClient
from qdrant_client.models import (
    Distance,
    FieldCondition,
    Filter,
    MatchValue,
    PointStruct,
    VectorParams,
)

from app.core.config import settings

logger = logging.getLogger(__name__)

# ---------- Qdrant 客户端（懒加载单例） ----------

_client: QdrantClient | None = None


def get_client() -> QdrantClient:
    global _client
    if _client is None:
        _client = QdrantClient(url=settings.qdrant_url)
    return _client


def init_collection():
    """启动时创建 collection（幂等）。"""
    client = get_client()
    names = [c.name for c in client.get_collections().collections]
    if settings.qdrant_collection not in names:
        client.create_collection(
            collection_name=settings.qdrant_collection,
            vectors_config=VectorParams(
                size=settings.embedding_dimension,
                distance=Distance.COSINE,
            ),
        )
        logger.info(
            "Created Qdrant collection: %s (dim=%d)",
            settings.qdrant_collection,
            settings.embedding_dimension,
        )
    else:
        logger.info("Qdrant collection already exists: %s", settings.qdrant_collection)


# ---------- Embedding ----------

def _embed_mock(text: str) -> list[float]:
    """确定性 mock embedding：SHA-256 hash → 归一化到指定维度。"""
    h = hashlib.sha256(text.encode("utf-8")).hexdigest()
    raw: list[float] = []
    seed = h
    while len(raw) < settings.embedding_dimension:
        for i in range(0, len(seed), 2):
            raw.append(int(seed[i : i + 2], 16) / 127.5 - 1.0)
        seed = hashlib.sha256(seed.encode("utf-8")).hexdigest()
    raw = raw[: settings.embedding_dimension]
    # L2 归一化
    norm = math.sqrt(sum(x * x for x in raw))
    return [x / norm for x in raw] if norm > 0 else raw


def _embed_live(text: str) -> list[float]:
    """调用 OpenAI 兼容 embedding API。"""
    from openai import OpenAI

    client = OpenAI(
        api_key=settings.embedding_api_key,
        base_url=settings.embedding_base_url,
    )
    resp = client.embeddings.create(
        model=settings.embedding_model,
        input=text,
    )
    return resp.data[0].embedding


def embed(text: str) -> list[float]:
    if settings.embedding_mock_mode:
        return _embed_mock(text)
    return _embed_live(text)


# ---------- 同步（upsert） ----------

def sync_item(
    tenant_id: int,
    item_id: int,
    knowledge_base_id: int,
    title: str,
    question: str,
    answer: str,
    product_type: str | None,
    fault_type: str | None,
    status: str,
) -> bool:
    """Embed 并 upsert 一条知识条目到 Qdrant。成功返回 True。"""
    try:
        embed_text = f"{title}\n{question}"
        vector = embed(embed_text)

        point = PointStruct(
            id=item_id,
            vector=vector,
            payload={
                "tenant_id": tenant_id,
                "knowledge_item_id": item_id,
                "knowledge_base_id": knowledge_base_id,
                "title": title,
                "question": question,
                "answer": answer,
                "product_type": product_type or "",
                "fault_type": fault_type or "",
                "status": status,
            },
        )
        client = get_client()
        client.upsert(
            collection_name=settings.qdrant_collection,
            points=[point],
        )
        logger.info("Synced item %d (tenant=%d) to Qdrant", item_id, tenant_id)
        return True
    except Exception as e:
        logger.error("Failed to sync item %d to Qdrant: %s", item_id, e)
        return False


# ---------- 删除 ----------

def delete_item(tenant_id: int, item_id: int) -> bool:
    """从 Qdrant 删除一条知识条目。成功返回 True。"""
    try:
        client = get_client()
        client.delete(
            collection_name=settings.qdrant_collection,
            points_selector=[item_id],
        )
        logger.info("Deleted item %d (tenant=%d) from Qdrant", item_id, tenant_id)
        return True
    except Exception as e:
        logger.error("Failed to delete item %d from Qdrant: %s", item_id, e)
        return False


# ---------- 搜索 ----------

def search(
    tenant_id: int,
    question: str,
    product_type: str | None = None,
    fault_type: str | None = None,
    top_k: int = 5,
) -> list[dict]:
    """向量搜索当前租户的 ACTIVE FAQ。返回结果列表。"""
    try:
        vector = embed(question)
        client = get_client()

        # 必选过滤：tenant_id + status=ACTIVE
        must_conditions = [
            FieldCondition(key="tenant_id", match=MatchValue(value=tenant_id)),
            FieldCondition(key="status", match=MatchValue(value="ACTIVE")),
        ]
        if product_type:
            must_conditions.append(
                FieldCondition(key="product_type", match=MatchValue(value=product_type))
            )
        if fault_type:
            must_conditions.append(
                FieldCondition(key="fault_type", match=MatchValue(value=fault_type))
            )

        search_filter = Filter(must=must_conditions)

        results = client.query_points(
            collection_name=settings.qdrant_collection,
            query=vector,
            query_filter=search_filter,
            limit=top_k,
            with_payload=True,
        )

        items = []
        for point in results.points:
            payload = point.payload
            items.append(
                {
                    "itemId": payload.get("knowledge_item_id"),
                    "score": point.score,
                    "title": payload.get("title"),
                    "question": payload.get("question"),
                    "answer": payload.get("answer"),
                    "productType": payload.get("product_type"),
                    "faultType": payload.get("fault_type"),
                }
            )
        return items
    except Exception as e:
        logger.error("Qdrant search failed for tenant %d: %s", tenant_id, e)
        return []
