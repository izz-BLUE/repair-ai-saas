# ============================================
# Python Agent Dockerfile
# ============================================
FROM python:3.11-slim
WORKDIR /app

# 一次性复制所有文件，然后安装
COPY . .
RUN pip install --no-cache-dir .

EXPOSE 8090
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
