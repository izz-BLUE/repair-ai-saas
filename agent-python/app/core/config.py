"""
AI 代理服务配置。
通过环境变量或 .env 文件加载。
"""
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # LLM 配置
    llm_api_key: str = ""
    llm_base_url: str = "https://api.deepseek.com"
    llm_model: str = "deepseek-chat"

    # 当 llm_api_key 为空时，使用 mock 模式
    @property
    def mock_mode(self) -> bool:
        return not self.llm_api_key

    # Qdrant 配置
    qdrant_url: str = "http://localhost:6333"
    qdrant_collection: str = "repair_faq_items"

    # Embedding 配置（OpenAI 兼容接口）
    embedding_api_key: str = ""
    embedding_base_url: str = "https://api.openai.com/v1"
    embedding_model: str = "text-embedding-3-small"
    embedding_dimension: int = 1536

    # 当 embedding_api_key 为空时，使用 mock embedding
    @property
    def embedding_mock_mode(self) -> bool:
        return not self.embedding_api_key

    # 服务端口
    agent_port: int = 8090

    model_config = {"env_prefix": "", "env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
