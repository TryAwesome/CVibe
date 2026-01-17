/*
Search Service - Main Entry Point
================================================================================

CVibe 搜索服务：
- 职位全文搜索
- 职位匹配算法
- 推荐引擎
- 数据聚合统计
*/

package main

import (
	"fmt"
	"log"
	"net"
	"os"
	"os/signal"
	"syscall"

	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"

	"github.com/cvibe/search-service/internal/config"
	sgrpc "github.com/cvibe/search-service/internal/grpc"
	pb "github.com/cvibe/search-service/internal/grpc/proto"
	"github.com/cvibe/search-service/internal/matching"
	"github.com/cvibe/search-service/internal/search"
)

func main() {
	// 加载配置
	cfg := config.Load()

	// 创建服务组件
	searchEngine := search.NewEngine()
	matcher := matching.NewMatcher()

	// 创建 gRPC 服务器
	lis, err := net.Listen("tcp", fmt.Sprintf(":%s", cfg.GRPCPort))
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}

	server := grpc.NewServer()

	// 注册服务
	searchService := sgrpc.NewSearchServiceServer(searchEngine, matcher)
	pb.RegisterSearchServiceServer(server, searchService)

	// 启用反射（方便调试）
	reflection.Register(server)

	// 优雅关闭
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		log.Printf("Search Service gRPC server starting on port %s", cfg.GRPCPort)
		if err := server.Serve(lis); err != nil {
			log.Fatalf("Failed to serve: %v", err)
		}
	}()

	<-quit
	log.Println("Shutting down server...")
	server.GracefulStop()
	log.Println("Server stopped")
}
