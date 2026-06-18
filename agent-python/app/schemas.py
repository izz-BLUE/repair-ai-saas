from pydantic import BaseModel


class FaqContext(BaseModel):
    title: str
    question: str
    answer: str


# ---------- Chat ----------

class ChatRequest(BaseModel):
    question: str
    contexts: list[FaqContext] = []  # 保留，向后兼容
    tenantId: int | None = None
    productType: str | None = None
    faultType: str | None = None
    topK: int = 5
    traceId: str | None = None


class ChatResponse(BaseModel):
    answer: str
    model: str
    shouldCreateTicket: bool = False
    matchedItemCount: int = 0
    traceId: str | None = None


# ---------- Knowledge Sync / Delete ----------

class SyncItemRequest(BaseModel):
    tenantId: int
    itemId: int
    knowledgeBaseId: int
    title: str
    question: str
    answer: str
    productType: str | None = None
    faultType: str | None = None
    status: str = "ACTIVE"


class DeleteItemRequest(BaseModel):
    tenantId: int
    itemId: int


# ---------- Search ----------

class SearchRequest(BaseModel):
    tenantId: int
    question: str
    productType: str | None = None
    faultType: str | None = None
    topK: int = 5
    traceId: str | None = None


class SearchResultItem(BaseModel):
    itemId: int
    score: float
    title: str
    question: str
    answer: str
    productType: str | None = None
    faultType: str | None = None


class SearchResponse(BaseModel):
    items: list[SearchResultItem]
    traceId: str | None = None
