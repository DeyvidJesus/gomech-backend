FROM python:3.11-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1
WORKDIR /app
RUN adduser --disabled-password --gecos '' appuser
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt \
    && pip cache purge
COPY app.py .
USER appuser
EXPOSE 5000
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:5000/health')"
CMD ["gunicorn", "--workers", "1", "--threads", "2", "--worker-tmp-dir", "/dev/shm", "--max-requests", "1000", "--max-requests-jitter", "100", "--bind", "0.0.0.0:5000", "app:app"]
