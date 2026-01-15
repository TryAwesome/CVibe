/*
Search Service - Main Entry Point
================================================================================

CVibe 搜索服务：
- 职位爬虫（每日抓取）
- 全文搜索
- 职位匹配算法
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

	// "google.golang.org/grpc"
	// pb "github.com/cvibe/search-service/internal/grpc/proto"
)

func main() {
	port := getEnv("GRPC_PORT", "50052")

	// 启动 gRPC 服务
	lis, err := net.Listen("tcp", fmt.Sprintf(":%s", port))
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}

	log.Printf("Search Service starting on port %s", port)

	// TODO: 实际启动 gRPC 服务
	// server := grpc.NewServer()
	// pb.RegisterSearchServiceServer(server, &handlers.SearchHandler{})
	// pb.RegisterMatchingServiceServer(server, &handlers.MatchingHandler{})
	// pb.RegisterCrawlerServiceServer(server, &handlers.CrawlerHandler{})

	// 优雅关闭
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		// server.Serve(lis)
		log.Printf("gRPC server listening on %s", lis.Addr().String())
	}()

	<-quit
	log.Println("Shutting down server...")
	// server.GracefulStop()
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
