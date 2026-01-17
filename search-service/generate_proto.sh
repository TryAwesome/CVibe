#!/bin/bash
# Generate Go gRPC code from proto files

PROTO_DIR="./proto"
OUT_DIR="./internal/grpc/proto"

mkdir -p $OUT_DIR

# Install protoc plugins if not exist
# go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
# go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest

protoc \
    --proto_path=$PROTO_DIR \
    --go_out=$OUT_DIR \
    --go_opt=paths=source_relative \
    --go-grpc_out=$OUT_DIR \
    --go-grpc_opt=paths=source_relative \
    $PROTO_DIR/search_service.proto

echo "Proto files generated successfully!"
