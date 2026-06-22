# ============================================
# Python Agent Dockerfile
# ============================================
FROM python:3.11-slim
WORKDIR /app

# 复制依赖文件先安装
COPY pyproject.toml ./
RUN pip install --no-cache-dir .

# 复制源码
COPY app ./app

EXPOSE 8090
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
