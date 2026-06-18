"""
Mock Embedding 测试：确定性、维度、归一化。
不需要真实 Qdrant 或 API Key。
"""
import math

from app.core.config import settings
from app.services.vector_service import _embed_mock


def test_deterministic():
    """同一文本两次调用结果完全一致。"""
    text = "空调不制冷怎么办"
    v1 = _embed_mock(text)
    v2 = _embed_mock(text)
    assert v1 == v2


def test_dimension():
    """向量维度等于配置值。"""
    v = _embed_mock("测试文本")
    assert len(v) == settings.embedding_dimension


def test_l2_normalized():
    """向量 L2 范数接近 1。"""
    v = _embed_mock("热水器显示E2错误")
    norm = math.sqrt(sum(x * x for x in v))
    assert abs(norm - 1.0) < 1e-6, f"L2 norm = {norm}, expected ~1.0"


def test_different_texts_different_vectors():
    """不同文本生成不同向量。"""
    v1 = _embed_mock("空调不制冷")
    v2 = _embed_mock("热水器不出热水")
    assert v1 != v2


def test_default_dimension():
    """默认维度为 1536。"""
    assert settings.embedding_dimension == 1536
