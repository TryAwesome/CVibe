/*
gRPC Handlers - Search Service
*/

package grpc

import (
	"context"

	"github.com/cvibe/search-service/internal/matching"
	"github.com/cvibe/search-service/internal/search"
)

// SearchHandler 搜索服务处理器
type SearchHandler struct {
	engine *search.Engine
}

// NewSearchHandler 创建搜索处理器
func NewSearchHandler(engine *search.Engine) *SearchHandler {
	return &SearchHandler{engine: engine}
}

// Search 搜索职位
func (h *SearchHandler) Search(ctx context.Context, req *search.SearchRequest) (*search.SearchResponse, error) {
	return h.engine.Search(ctx, *req)
}

// MatchingHandler 匹配服务处理器
type MatchingHandler struct {
	matcher *matching.Matcher
}

// NewMatchingHandler 创建匹配处理器
func NewMatchingHandler(matcher *matching.Matcher) *MatchingHandler {
	return &MatchingHandler{matcher: matcher}
}

// TODO: 实现 gRPC 服务接口
