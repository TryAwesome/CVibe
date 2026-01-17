#!/bin/bash
# CVibe 服务启动脚本
# 用法: ./start.sh [infra|app|all]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 启动基础设施
start_infra() {
    log_info "Starting infrastructure services..."
    cd "$SCRIPT_DIR/infra"
    docker-compose up -d postgres redis minio zookeeper kafka elasticsearch
    
    log_info "Waiting for services to be healthy..."
    sleep 10
    
    # 检查服务状态
    docker-compose ps
    
    log_info "Infrastructure services started!"
}

# 启动应用服务
start_app() {
    log_info "Starting application services..."
    cd "$SCRIPT_DIR/infra"
    docker-compose --profile app up -d
    
    log_info "Application services started!"
}

# 本地开发模式启动
start_dev() {
    log_info "Starting in development mode..."
    
    # 1. 启动基础设施
    start_infra
    
    # 2. 在新终端中启动各服务（提示用户手动执行）
    echo ""
    log_info "Infrastructure is ready. Start services manually:"
    echo ""
    echo "  # Terminal 1 - biz-service:"
    echo "  cd biz-service && mvn spring-boot:run"
    echo ""
    echo "  # Terminal 2 - ai-engine:"
    echo "  cd ai-engine && pip install -r requirements.txt && python main.py"
    echo ""
    echo "  # Terminal 3 - search-service:"
    echo "  cd search-service && go run ./cmd/server"
    echo ""
    echo "  # Terminal 4 - frontend:"
    echo "  cd frontend && npm run dev"
    echo ""
}

# 停止所有服务
stop_all() {
    log_info "Stopping all services..."
    cd "$SCRIPT_DIR/infra"
    docker-compose --profile app down
    docker-compose down
    log_info "All services stopped!"
}

# 健康检查
health_check() {
    log_info "Checking service health..."
    
    # PostgreSQL
    if nc -z localhost 5432 2>/dev/null; then
        echo -e "PostgreSQL:     ${GREEN}✓${NC} Running on :5432"
    else
        echo -e "PostgreSQL:     ${RED}✗${NC} Not running"
    fi
    
    # Redis
    if nc -z localhost 6379 2>/dev/null; then
        echo -e "Redis:          ${GREEN}✓${NC} Running on :6379"
    else
        echo -e "Redis:          ${RED}✗${NC} Not running"
    fi
    
    # MinIO
    if curl -s http://localhost:9000/minio/health/live >/dev/null 2>&1; then
        echo -e "MinIO:          ${GREEN}✓${NC} Running on :9000"
    else
        echo -e "MinIO:          ${RED}✗${NC} Not running"
    fi
    
    # Elasticsearch
    if curl -s http://localhost:9200/_cluster/health >/dev/null 2>&1; then
        echo -e "Elasticsearch:  ${GREEN}✓${NC} Running on :9200"
    else
        echo -e "Elasticsearch:  ${RED}✗${NC} Not running"
    fi
    
    # biz-service
    if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo -e "biz-service:    ${GREEN}✓${NC} Running on :8080"
    else
        echo -e "biz-service:    ${RED}✗${NC} Not running"
    fi
    
    # ai-engine
    if nc -z localhost 50051 2>/dev/null; then
        echo -e "ai-engine:      ${GREEN}✓${NC} Running on :50051"
    else
        echo -e "ai-engine:      ${RED}✗${NC} Not running"
    fi
    
    # search-service
    if nc -z localhost 50052 2>/dev/null; then
        echo -e "search-service: ${GREEN}✓${NC} Running on :50052"
    else
        echo -e "search-service: ${RED}✗${NC} Not running"
    fi
    
    # Frontend
    if curl -s http://localhost:3000 >/dev/null 2>&1; then
        echo -e "frontend:       ${GREEN}✓${NC} Running on :3000"
    else
        echo -e "frontend:       ${RED}✗${NC} Not running"
    fi
}

# 显示帮助
show_help() {
    echo "CVibe 服务管理脚本"
    echo ""
    echo "用法: ./start.sh <command>"
    echo ""
    echo "命令:"
    echo "  infra    启动基础设施 (PostgreSQL, Redis, MinIO, Kafka, ES)"
    echo "  app      启动所有应用服务 (需要先构建 Docker 镜像)"
    echo "  dev      开发模式 (启动基础设施，提示手动启动应用)"
    echo "  stop     停止所有服务"
    echo "  health   检查服务健康状态"
    echo "  help     显示此帮助信息"
    echo ""
}

# 主入口
case "${1:-help}" in
    infra)
        start_infra
        ;;
    app)
        start_app
        ;;
    dev)
        start_dev
        ;;
    stop)
        stop_all
        ;;
    health)
        health_check
        ;;
    help|*)
        show_help
        ;;
esac
