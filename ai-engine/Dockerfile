FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    poppler-utils \
    tesseract-ocr \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy proto file and generate
COPY proto/ proto/
COPY generate_proto.sh .
RUN chmod +x generate_proto.sh && ./generate_proto.sh

# Copy source code
COPY src/ src/
COPY main.py .

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV GRPC_PORT=50051

EXPOSE 50051

CMD ["python", "main.py"]
