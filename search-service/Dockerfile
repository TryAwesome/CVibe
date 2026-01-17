FROM golang:1.21-alpine AS builder

WORKDIR /app

# Install protoc
RUN apk add --no-cache protobuf protobuf-dev git

# Install Go protoc plugins
RUN go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
RUN go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest

# Copy go mod files
COPY go.mod go.sum ./
RUN go mod download

# Copy proto files and generate
COPY proto/ proto/
COPY generate_proto.sh .
RUN chmod +x generate_proto.sh && ./generate_proto.sh

# Copy source code
COPY . .

# Build
RUN CGO_ENABLED=0 GOOS=linux go build -o /search-service ./cmd/server

# Runtime image
FROM alpine:latest

RUN apk --no-cache add ca-certificates

WORKDIR /root/

COPY --from=builder /search-service .

ENV GRPC_PORT=50052

EXPOSE 50052

CMD ["./search-service"]
