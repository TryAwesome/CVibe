#!/bin/bash
# Generate Python gRPC code from proto files

PROTO_DIR="./proto"
OUT_DIR="./src/generated"

mkdir -p $OUT_DIR

python -m grpc_tools.protoc \
    -I$PROTO_DIR \
    --python_out=$OUT_DIR \
    --pyi_out=$OUT_DIR \
    --grpc_python_out=$OUT_DIR \
    $PROTO_DIR/ai_engine.proto

# Fix imports in generated files
sed -i '' 's/import ai_engine_pb2/from . import ai_engine_pb2/' $OUT_DIR/ai_engine_pb2_grpc.py 2>/dev/null || \
sed -i 's/import ai_engine_pb2/from . import ai_engine_pb2/' $OUT_DIR/ai_engine_pb2_grpc.py

echo "Proto files generated successfully!"
