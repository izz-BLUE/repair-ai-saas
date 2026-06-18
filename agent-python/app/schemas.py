from pydantic import BaseModel


class FaqContext(BaseModel):
    title: str
    question: str
    answer: str


class ChatRequest(BaseModel):
    question: str
    contexts: list[FaqContext] = []
    tenantId: int | None = None
    traceId: str | None = None


class ChatResponse(BaseModel):
    answer: str
    model: str
    shouldCreateTicket: bool = False
    traceId: str | None = None
