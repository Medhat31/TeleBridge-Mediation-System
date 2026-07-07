# ==========================================
# TeleBridge CDR Generator - Dockerfile
# ==========================================
FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY cdr-generator.py .

CMD ["python", "-u", "cdr-generator.py"]
