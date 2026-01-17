# Search Service 设计 (Go)

> Go gRPC 服务，负责高并发搜索、并行计算、数据爬虫

---

## 1. 服务概述

### 1.1 定位

Search Service 是一个高性能的 Go 微服务，处理所有计算密集型和 I/O 密集型任务：
- 职位搜索（高并发）
- 简历-职位匹配（并行计算）
- 批量数据处理
- 外部数据爬取（Boss、拉勾等）
- 数据分析聚合

### 1.2 设计原则

1. **高并发** - 使用 goroutine + channel 实现高并发
2. **并行计算** - 匹配算法并行执行
3. **gRPC 通信** - 与 biz-service 通过 gRPC 通信
4. **无状态** - 服务无状态，可水平扩展

---

## 2. 项目结构

```
search-service/
├── go.mod
├── go.sum
├── cmd/
│   └── server/
│       └── main.go             # 入口
├── internal/
│   ├── config/                 # 配置
│   │   └── config.go
│   ├── grpc/                   # gRPC 服务
│   │   ├── server.go
│   │   └── handlers.go
│   ├── search/                 # 搜索引擎
│   │   ├── engine.go
│   │   ├── query.go
│   │   └── index.go
│   ├── matching/               # 匹配算法
│   │   ├── matcher.go
│   │   ├── parallel.go
│   │   └── scorer.go
│   ├── crawler/                # 数据爬虫
│   │   ├── crawler.go
│   │   ├── boss.go
│   │   ├── lagou.go
│   │   └── scheduler.go
│   ├── analytics/              # 数据分析
│   │   └── aggregator.go
│   └── storage/                # 数据存储
│       ├── cache.go
│       └── db.go
├── proto/                      # Proto 定义
│   └── search_service.proto
└── pkg/                        # 公共包
    └── utils/
```

---

## 3. gRPC 服务定义

### 3.1 Proto 文件

```protobuf
// proto/search_service.proto
syntax = "proto3";
package cvibe.search;

option go_package = "github.com/cvibe/search-service/proto";
option java_package = "com.cvibe.grpc.search";
option java_outer_classname = "SearchServiceProto";

// ==================== 服务定义 ====================

service SearchService {
  // 职位搜索
  rpc SearchJobs(SearchJobsRequest) returns (SearchJobsResponse);
  
  // 简历-职位匹配
  rpc MatchResumeToJob(MatchRequest) returns (MatchResponse);
  
  // 批量匹配（并行计算）
  rpc BatchMatch(BatchMatchRequest) returns (BatchMatchResponse);
  
  // 职位推荐
  rpc GetJobRecommendations(RecommendRequest) returns (RecommendResponse);
  
  // 搜索建议（自动补全）
  rpc GetSearchSuggestions(SuggestionRequest) returns (SuggestionResponse);
  
  // 热门搜索
  rpc GetTrendingSearches(TrendingRequest) returns (TrendingResponse);
  
  // 爬虫任务
  rpc TriggerCrawl(CrawlRequest) returns (CrawlResponse);
  
  // 数据聚合
  rpc GetAnalytics(AnalyticsRequest) returns (AnalyticsResponse);
}

// ==================== 职位搜索 ====================

message SearchJobsRequest {
  string query = 1;                 // 搜索关键词
  repeated string locations = 2;    // 地点筛选
  repeated string industries = 3;   // 行业筛选
  string experience_level = 4;      // 经验要求
  string salary_range = 5;          // 薪资范围
  string employment_type = 6;       // 全职/兼职
  int32 page = 7;
  int32 page_size = 8;
  string sort_by = 9;               // relevance / date / salary
}

message SearchJobsResponse {
  repeated Job jobs = 1;
  int32 total = 2;
  int32 page = 3;
  int32 total_pages = 4;
  repeated Facet facets = 5;        // 聚合筛选项
}

message Job {
  string id = 1;
  string title = 2;
  string company = 3;
  string company_logo = 4;
  string location = 5;
  string salary_range = 6;
  string experience = 7;
  string employment_type = 8;
  string description = 9;
  repeated string requirements = 10;
  repeated string benefits = 11;
  string posted_at = 12;
  string source = 13;               // boss / lagou / linkedin
  string source_url = 14;
  double match_score = 15;          // 匹配度（0-100）
}

message Facet {
  string name = 1;                  // location / industry / experience
  repeated FacetItem items = 2;
}

message FacetItem {
  string value = 1;
  int32 count = 2;
}

// ==================== 匹配计算 ====================

message MatchRequest {
  ResumeProfile resume = 1;
  Job job = 2;
}

message ResumeProfile {
  string user_id = 1;
  string title = 2;
  repeated string skills = 3;
  int32 years_experience = 4;
  repeated Experience experiences = 5;
  repeated string preferred_locations = 6;
  string expected_salary = 7;
}

message Experience {
  string title = 1;
  string company = 2;
  int32 duration_months = 3;
  repeated string skills_used = 4;
}

message MatchResponse {
  double overall_score = 1;         // 0-100
  MatchDetails details = 2;
  repeated string match_reasons = 3;
  repeated string gap_reasons = 4;
}

message MatchDetails {
  double skill_match = 1;           // 技能匹配度
  double experience_match = 2;      // 经验匹配度
  double location_match = 3;        // 地点匹配度
  double salary_match = 4;          // 薪资匹配度
  double title_match = 5;           // 职位匹配度
}

// ==================== 批量匹配 ====================

message BatchMatchRequest {
  ResumeProfile resume = 1;
  repeated string job_ids = 2;      // 最多 100 个
}

message BatchMatchResponse {
  repeated JobMatch matches = 1;
  int32 processed = 2;
  int32 failed = 3;
}

message JobMatch {
  string job_id = 1;
  double score = 2;
  MatchDetails details = 3;
}

// ==================== 推荐 ====================

message RecommendRequest {
  string user_id = 1;
  ResumeProfile resume = 2;
  int32 limit = 3;
  repeated string exclude_job_ids = 4;  // 排除已看过的
}

message RecommendResponse {
  repeated RecommendedJob recommendations = 1;
}

message RecommendedJob {
  Job job = 1;
  double match_score = 2;
  string recommend_reason = 3;
}

// ==================== 搜索建议 ====================

message SuggestionRequest {
  string prefix = 1;
  int32 limit = 2;
}

message SuggestionResponse {
  repeated Suggestion suggestions = 1;
}

message Suggestion {
  string text = 1;
  string type = 2;                  // job_title / company / skill
  int32 count = 3;
}

// ==================== 热门搜索 ====================

message TrendingRequest {
  string location = 1;
  int32 limit = 2;
}

message TrendingResponse {
  repeated TrendingItem items = 1;
}

message TrendingItem {
  string keyword = 1;
  int32 search_count = 2;
  double growth_rate = 3;           // 增长率
}

// ==================== 爬虫 ====================

message CrawlRequest {
  string source = 1;                // boss / lagou / all
  repeated string keywords = 2;
  repeated string locations = 3;
}

message CrawlResponse {
  string task_id = 1;
  string status = 2;                // queued / running / completed
  int32 estimated_jobs = 3;
}

// ==================== 分析 ====================

message AnalyticsRequest {
  string metric = 1;                // salary_trend / demand_trend / skill_trend
  string industry = 2;
  string location = 3;
  string time_range = 4;            // 7d / 30d / 90d
}

message AnalyticsResponse {
  repeated DataPoint data_points = 1;
  map<string, double> summary = 2;
}

message DataPoint {
  string date = 1;
  double value = 2;
  string label = 3;
}
```

---

## 4. 核心实现

### 4.1 主入口

```go
// cmd/server/main.go
package main

import (
	"log"
	"net"
	"os"
	"os/signal"
	"syscall"

	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"

	"github.com/cvibe/search-service/internal/config"
	grpcserver "github.com/cvibe/search-service/internal/grpc"
	"github.com/cvibe/search-service/internal/matching"
	"github.com/cvibe/search-service/internal/search"
	pb "github.com/cvibe/search-service/proto"
)

func main() {
	// 加载配置
	cfg := config.Load()

	// 初始化组件
	searchEngine := search.NewEngine(cfg)
	matcher := matching.NewMatcher(cfg)

	// 创建 gRPC 服务
	server := grpc.NewServer(
		grpc.MaxRecvMsgSize(50*1024*1024), // 50MB
		grpc.MaxSendMsgSize(50*1024*1024),
	)

	// 注册服务
	handler := grpcserver.NewHandler(searchEngine, matcher)
	pb.RegisterSearchServiceServer(server, handler)

	// 反射（调试用）
	reflection.Register(server)

	// 启动监听
	lis, err := net.Listen("tcp", ":"+cfg.GRPCPort)
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}

	log.Printf("Search Service gRPC server starting on port %s", cfg.GRPCPort)

	// 优雅关闭
	go func() {
		sigCh := make(chan os.Signal, 1)
		signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
		<-sigCh
		log.Println("Shutting down...")
		server.GracefulStop()
	}()

	if err := server.Serve(lis); err != nil {
		log.Fatalf("Failed to serve: %v", err)
	}
}
```

### 4.2 配置

```go
// internal/config/config.go
package config

import (
	"os"
)

type Config struct {
	GRPCPort        string
	DatabaseURL     string
	RedisURL        string
	ElasticsearchURL string
	MaxWorkers      int
}

func Load() *Config {
	return &Config{
		GRPCPort:         getEnv("GRPC_PORT", "50052"),
		DatabaseURL:      getEnv("DATABASE_URL", "postgres://localhost/cvibe"),
		RedisURL:         getEnv("REDIS_URL", "redis://localhost:6379"),
		ElasticsearchURL: getEnv("ELASTICSEARCH_URL", "http://localhost:9200"),
		MaxWorkers:       getEnvInt("MAX_WORKERS", 100),
	}
}

func getEnv(key, defaultValue string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return defaultValue
}

func getEnvInt(key string, defaultValue int) int {
	// 省略实现
	return defaultValue
}
```

### 4.3 gRPC Handler

```go
// internal/grpc/handlers.go
package grpc

import (
	"context"

	"github.com/cvibe/search-service/internal/matching"
	"github.com/cvibe/search-service/internal/search"
	pb "github.com/cvibe/search-service/proto"
)

type Handler struct {
	pb.UnimplementedSearchServiceServer
	engine  *search.Engine
	matcher *matching.Matcher
}

func NewHandler(engine *search.Engine, matcher *matching.Matcher) *Handler {
	return &Handler{
		engine:  engine,
		matcher: matcher,
	}
}

// SearchJobs 职位搜索
func (h *Handler) SearchJobs(ctx context.Context, req *pb.SearchJobsRequest) (*pb.SearchJobsResponse, error) {
	query := search.Query{
		Keyword:         req.Query,
		Locations:       req.Locations,
		Industries:      req.Industries,
		ExperienceLevel: req.ExperienceLevel,
		SalaryRange:     req.SalaryRange,
		EmploymentType:  req.EmploymentType,
		Page:            int(req.Page),
		PageSize:        int(req.PageSize),
		SortBy:          req.SortBy,
	}

	result, err := h.engine.Search(ctx, query)
	if err != nil {
		return nil, err
	}

	jobs := make([]*pb.Job, len(result.Jobs))
	for i, j := range result.Jobs {
		jobs[i] = jobToProto(j)
	}

	return &pb.SearchJobsResponse{
		Jobs:       jobs,
		Total:      int32(result.Total),
		Page:       int32(result.Page),
		TotalPages: int32(result.TotalPages),
		Facets:     facetsToProto(result.Facets),
	}, nil
}

// MatchResumeToJob 单个匹配
func (h *Handler) MatchResumeToJob(ctx context.Context, req *pb.MatchRequest) (*pb.MatchResponse, error) {
	resume := protoToResume(req.Resume)
	job := protoToJob(req.Job)

	result := h.matcher.Match(resume, job)

	return &pb.MatchResponse{
		OverallScore: result.OverallScore,
		Details: &pb.MatchDetails{
			SkillMatch:      result.SkillMatch,
			ExperienceMatch: result.ExperienceMatch,
			LocationMatch:   result.LocationMatch,
			SalaryMatch:     result.SalaryMatch,
			TitleMatch:      result.TitleMatch,
		},
		MatchReasons: result.MatchReasons,
		GapReasons:   result.GapReasons,
	}, nil
}

// BatchMatch 批量匹配（并行）
func (h *Handler) BatchMatch(ctx context.Context, req *pb.BatchMatchRequest) (*pb.BatchMatchResponse, error) {
	resume := protoToResume(req.Resume)

	// 并行匹配
	results := h.matcher.BatchMatchParallel(ctx, resume, req.JobIds)

	matches := make([]*pb.JobMatch, len(results))
	for i, r := range results {
		matches[i] = &pb.JobMatch{
			JobId: r.JobID,
			Score: r.Score,
			Details: &pb.MatchDetails{
				SkillMatch:      r.Details.SkillMatch,
				ExperienceMatch: r.Details.ExperienceMatch,
				LocationMatch:   r.Details.LocationMatch,
				SalaryMatch:     r.Details.SalaryMatch,
				TitleMatch:      r.Details.TitleMatch,
			},
		}
	}

	return &pb.BatchMatchResponse{
		Matches:   matches,
		Processed: int32(len(results)),
		Failed:    0,
	}, nil
}

// GetJobRecommendations 职位推荐
func (h *Handler) GetJobRecommendations(ctx context.Context, req *pb.RecommendRequest) (*pb.RecommendResponse, error) {
	resume := protoToResume(req.Resume)

	recommendations := h.engine.Recommend(ctx, resume, int(req.Limit), req.ExcludeJobIds)

	result := make([]*pb.RecommendedJob, len(recommendations))
	for i, r := range recommendations {
		result[i] = &pb.RecommendedJob{
			Job:             jobToProto(r.Job),
			MatchScore:      r.MatchScore,
			RecommendReason: r.Reason,
		}
	}

	return &pb.RecommendResponse{
		Recommendations: result,
	}, nil
}

// 辅助转换函数
func jobToProto(j *search.Job) *pb.Job {
	return &pb.Job{
		Id:             j.ID,
		Title:          j.Title,
		Company:        j.Company,
		CompanyLogo:    j.CompanyLogo,
		Location:       j.Location,
		SalaryRange:    j.SalaryRange,
		Experience:     j.Experience,
		EmploymentType: j.EmploymentType,
		Description:    j.Description,
		Requirements:   j.Requirements,
		Benefits:       j.Benefits,
		PostedAt:       j.PostedAt,
		Source:         j.Source,
		SourceUrl:      j.SourceURL,
	}
}

func protoToResume(p *pb.ResumeProfile) *matching.Resume {
	return &matching.Resume{
		UserID:             p.UserId,
		Title:              p.Title,
		Skills:             p.Skills,
		YearsExperience:    int(p.YearsExperience),
		PreferredLocations: p.PreferredLocations,
		ExpectedSalary:     p.ExpectedSalary,
	}
}

func protoToJob(p *pb.Job) *search.Job {
	return &search.Job{
		ID:             p.Id,
		Title:          p.Title,
		Company:        p.Company,
		Location:       p.Location,
		SalaryRange:    p.SalaryRange,
		Experience:     p.Experience,
		EmploymentType: p.EmploymentType,
		Description:    p.Description,
		Requirements:   p.Requirements,
	}
}

func facetsToProto(facets []search.Facet) []*pb.Facet {
	result := make([]*pb.Facet, len(facets))
	for i, f := range facets {
		items := make([]*pb.FacetItem, len(f.Items))
		for j, item := range f.Items {
			items[j] = &pb.FacetItem{
				Value: item.Value,
				Count: int32(item.Count),
			}
		}
		result[i] = &pb.Facet{
			Name:  f.Name,
			Items: items,
		}
	}
	return result
}
```

---

## 5. 搜索引擎

### 5.1 搜索实现

```go
// internal/search/engine.go
package search

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/elastic/go-elasticsearch/v8"
)

type Engine struct {
	es *elasticsearch.Client
}

func NewEngine(cfg *config.Config) *Engine {
	es, _ := elasticsearch.NewClient(elasticsearch.Config{
		Addresses: []string{cfg.ElasticsearchURL},
	})
	return &Engine{es: es}
}

type Job struct {
	ID             string   `json:"id"`
	Title          string   `json:"title"`
	Company        string   `json:"company"`
	CompanyLogo    string   `json:"company_logo"`
	Location       string   `json:"location"`
	SalaryRange    string   `json:"salary_range"`
	Experience     string   `json:"experience"`
	EmploymentType string   `json:"employment_type"`
	Description    string   `json:"description"`
	Requirements   []string `json:"requirements"`
	Benefits       []string `json:"benefits"`
	PostedAt       string   `json:"posted_at"`
	Source         string   `json:"source"`
	SourceURL      string   `json:"source_url"`
}

type SearchResult struct {
	Jobs       []*Job
	Total      int
	Page       int
	TotalPages int
	Facets     []Facet
}

type Facet struct {
	Name  string
	Items []FacetItem
}

type FacetItem struct {
	Value string
	Count int
}

func (e *Engine) Search(ctx context.Context, query Query) (*SearchResult, error) {
	// 构建 Elasticsearch 查询
	esQuery := e.buildQuery(query)

	res, err := e.es.Search(
		e.es.Search.WithContext(ctx),
		e.es.Search.WithIndex("jobs"),
		e.es.Search.WithBody(strings.NewReader(esQuery)),
		e.es.Search.WithFrom((query.Page-1)*query.PageSize),
		e.es.Search.WithSize(query.PageSize),
	)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	// 解析结果
	var result map[string]interface{}
	json.NewDecoder(res.Body).Decode(&result)

	return e.parseResult(result, query), nil
}

func (e *Engine) buildQuery(q Query) string {
	// 构建 bool 查询
	must := []map[string]interface{}{}

	if q.Keyword != "" {
		must = append(must, map[string]interface{}{
			"multi_match": map[string]interface{}{
				"query":  q.Keyword,
				"fields": []string{"title^3", "company^2", "description", "requirements"},
			},
		})
	}

	filter := []map[string]interface{}{}

	if len(q.Locations) > 0 {
		filter = append(filter, map[string]interface{}{
			"terms": map[string]interface{}{"location": q.Locations},
		})
	}

	if q.ExperienceLevel != "" {
		filter = append(filter, map[string]interface{}{
			"term": map[string]interface{}{"experience": q.ExperienceLevel},
		})
	}

	query := map[string]interface{}{
		"query": map[string]interface{}{
			"bool": map[string]interface{}{
				"must":   must,
				"filter": filter,
			},
		},
		"aggs": map[string]interface{}{
			"locations": map[string]interface{}{
				"terms": map[string]interface{}{"field": "location", "size": 20},
			},
			"industries": map[string]interface{}{
				"terms": map[string]interface{}{"field": "industry", "size": 20},
			},
		},
	}

	// 排序
	switch q.SortBy {
	case "date":
		query["sort"] = []map[string]interface{}{
			{"posted_at": "desc"},
		}
	case "salary":
		query["sort"] = []map[string]interface{}{
			{"salary_max": "desc"},
		}
	default:
		query["sort"] = []map[string]interface{}{
			{"_score": "desc"},
		}
	}

	bytes, _ := json.Marshal(query)
	return string(bytes)
}

func (e *Engine) parseResult(result map[string]interface{}, query Query) *SearchResult {
	hits := result["hits"].(map[string]interface{})
	total := int(hits["total"].(map[string]interface{})["value"].(float64))

	jobHits := hits["hits"].([]interface{})
	jobs := make([]*Job, len(jobHits))

	for i, hit := range jobHits {
		source := hit.(map[string]interface{})["_source"].(map[string]interface{})
		jobs[i] = &Job{
			ID:          source["id"].(string),
			Title:       source["title"].(string),
			Company:     source["company"].(string),
			Location:    source["location"].(string),
			SalaryRange: source["salary_range"].(string),
			Description: source["description"].(string),
		}
	}

	return &SearchResult{
		Jobs:       jobs,
		Total:      total,
		Page:       query.Page,
		TotalPages: (total + query.PageSize - 1) / query.PageSize,
	}
}

// Recommend 基于用户画像推荐职位
func (e *Engine) Recommend(ctx context.Context, resume *matching.Resume, limit int, excludeIDs []string) []*RecommendedJob {
	// 基于技能和经验构建推荐查询
	query := map[string]interface{}{
		"query": map[string]interface{}{
			"bool": map[string]interface{}{
				"should": []map[string]interface{}{
					{
						"terms": map[string]interface{}{
							"requirements": resume.Skills,
							"boost":        2.0,
						},
					},
					{
						"match": map[string]interface{}{
							"title": resume.Title,
						},
					},
				},
				"must_not": []map[string]interface{}{
					{"ids": map[string]interface{}{"values": excludeIDs}},
				},
			},
		},
		"size": limit,
	}

	// 执行搜索...
	return nil // 省略实现
}
```

---

## 6. 并行匹配

### 6.1 匹配器

```go
// internal/matching/matcher.go
package matching

import (
	"math"
	"strings"
)

type Resume struct {
	UserID             string
	Title              string
	Skills             []string
	YearsExperience    int
	PreferredLocations []string
	ExpectedSalary     string
}

type MatchResult struct {
	JobID        string
	Score        float64
	Details      MatchDetails
	MatchReasons []string
	GapReasons   []string
}

type MatchDetails struct {
	SkillMatch      float64
	ExperienceMatch float64
	LocationMatch   float64
	SalaryMatch     float64
	TitleMatch      float64
}

type Matcher struct {
	skillWeights map[string]float64
}

func NewMatcher(cfg *config.Config) *Matcher {
	return &Matcher{
		skillWeights: loadSkillWeights(),
	}
}

func (m *Matcher) Match(resume *Resume, job *search.Job) *MatchResult {
	details := MatchDetails{}
	var matchReasons, gapReasons []string

	// 技能匹配 (权重 40%)
	details.SkillMatch, matchReasons, gapReasons = m.matchSkills(resume.Skills, job.Requirements)

	// 经验匹配 (权重 25%)
	details.ExperienceMatch = m.matchExperience(resume.YearsExperience, job.Experience)

	// 地点匹配 (权重 15%)
	details.LocationMatch = m.matchLocation(resume.PreferredLocations, job.Location)

	// 薪资匹配 (权重 10%)
	details.SalaryMatch = m.matchSalary(resume.ExpectedSalary, job.SalaryRange)

	// 职位匹配 (权重 10%)
	details.TitleMatch = m.matchTitle(resume.Title, job.Title)

	// 计算总分
	overallScore := details.SkillMatch*0.4 +
		details.ExperienceMatch*0.25 +
		details.LocationMatch*0.15 +
		details.SalaryMatch*0.1 +
		details.TitleMatch*0.1

	return &MatchResult{
		JobID:        job.ID,
		Score:        overallScore * 100,
		Details:      details,
		MatchReasons: matchReasons,
		GapReasons:   gapReasons,
	}
}

func (m *Matcher) matchSkills(resumeSkills, jobRequirements []string) (float64, []string, []string) {
	if len(jobRequirements) == 0 {
		return 1.0, nil, nil
	}

	resumeSkillSet := make(map[string]bool)
	for _, s := range resumeSkills {
		resumeSkillSet[strings.ToLower(s)] = true
	}

	matched := 0
	var matchReasons, gapReasons []string

	for _, req := range jobRequirements {
		reqLower := strings.ToLower(req)
		if resumeSkillSet[reqLower] {
			matched++
			matchReasons = append(matchReasons, "技能匹配: "+req)
		} else {
			gapReasons = append(gapReasons, "缺少技能: "+req)
		}
	}

	return float64(matched) / float64(len(jobRequirements)), matchReasons, gapReasons
}

func (m *Matcher) matchExperience(resumeYears int, jobExp string) float64 {
	// 解析职位要求的经验年限
	requiredYears := parseExperienceYears(jobExp)

	if resumeYears >= requiredYears {
		return 1.0
	}

	// 差距越大，分数越低
	diff := requiredYears - resumeYears
	return math.Max(0, 1.0-float64(diff)*0.2)
}

func (m *Matcher) matchLocation(preferred []string, jobLocation string) float64 {
	if len(preferred) == 0 {
		return 1.0 // 不限地点
	}

	jobLocLower := strings.ToLower(jobLocation)
	for _, loc := range preferred {
		if strings.Contains(jobLocLower, strings.ToLower(loc)) {
			return 1.0
		}
	}

	return 0.5 // 地点不匹配但不是硬性条件
}

func (m *Matcher) matchSalary(expected, offered string) float64 {
	// 解析薪资范围...
	return 0.8 // 简化实现
}

func (m *Matcher) matchTitle(resumeTitle, jobTitle string) float64 {
	// 计算职位相似度
	resumeLower := strings.ToLower(resumeTitle)
	jobLower := strings.ToLower(jobTitle)

	if resumeLower == jobLower {
		return 1.0
	}

	// 部分匹配
	if strings.Contains(jobLower, resumeLower) || strings.Contains(resumeLower, jobLower) {
		return 0.8
	}

	return 0.5
}

func parseExperienceYears(exp string) int {
	// 解析 "3-5年" -> 3
	// 简化实现
	return 3
}

func loadSkillWeights() map[string]float64 {
	return map[string]float64{
		"java":       1.0,
		"python":     1.0,
		"go":         1.0,
		"javascript": 0.9,
		"react":      0.8,
	}
}
```

### 6.2 并行批量匹配

```go
// internal/matching/parallel.go
package matching

import (
	"context"
	"sync"

	"github.com/cvibe/search-service/internal/search"
)

// BatchMatchParallel 并行批量匹配
func (m *Matcher) BatchMatchParallel(ctx context.Context, resume *Resume, jobIDs []string) []*MatchResult {
	results := make([]*MatchResult, len(jobIDs))
	var wg sync.WaitGroup
	
	// 限制并发数
	semaphore := make(chan struct{}, 50)

	for i, jobID := range jobIDs {
		wg.Add(1)
		go func(idx int, id string) {
			defer wg.Done()
			
			// 获取信号量
			semaphore <- struct{}{}
			defer func() { <-semaphore }()

			// 检查上下文是否取消
			select {
			case <-ctx.Done():
				return
			default:
			}

			// 获取职位详情
			job, err := m.getJob(id)
			if err != nil {
				return
			}

			// 执行匹配
			results[idx] = m.Match(resume, job)
		}(i, jobID)
	}

	wg.Wait()

	// 过滤空结果
	filtered := make([]*MatchResult, 0, len(results))
	for _, r := range results {
		if r != nil {
			filtered = append(filtered, r)
		}
	}

	return filtered
}

// BatchMatchWithWorkerPool 使用工作池的批量匹配
func (m *Matcher) BatchMatchWithWorkerPool(ctx context.Context, resume *Resume, jobIDs []string, workerCount int) []*MatchResult {
	jobs := make(chan string, len(jobIDs))
	results := make(chan *MatchResult, len(jobIDs))

	// 启动工作协程
	var wg sync.WaitGroup
	for i := 0; i < workerCount; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for jobID := range jobs {
				select {
				case <-ctx.Done():
					return
				default:
				}

				job, err := m.getJob(jobID)
				if err != nil {
					continue
				}

				result := m.Match(resume, job)
				results <- result
			}
		}()
	}

	// 发送任务
	go func() {
		for _, id := range jobIDs {
			jobs <- id
		}
		close(jobs)
	}()

	// 等待完成并关闭结果通道
	go func() {
		wg.Wait()
		close(results)
	}()

	// 收集结果
	var allResults []*MatchResult
	for r := range results {
		allResults = append(allResults, r)
	}

	return allResults
}

func (m *Matcher) getJob(id string) (*search.Job, error) {
	// 从数据库或缓存获取职位
	return nil, nil // 省略实现
}
```

---

## 7. 爬虫调度

### 7.1 爬虫接口

```go
// internal/crawler/crawler.go
package crawler

import (
	"context"
)

type Crawler interface {
	Name() string
	Crawl(ctx context.Context, keywords []string, locations []string) ([]*Job, error)
}

type Job struct {
	Title       string
	Company     string
	Location    string
	SalaryRange string
	Description string
	URL         string
	Source      string
}

// 注册所有爬虫
var crawlers = map[string]Crawler{
	"boss":  &BossCrawler{},
	"lagou": &LagouCrawler{},
}

func GetCrawler(name string) Crawler {
	return crawlers[name]
}

func GetAllCrawlers() []Crawler {
	result := make([]Crawler, 0, len(crawlers))
	for _, c := range crawlers {
		result = append(result, c)
	}
	return result
}
```

### 7.2 Boss 直聘爬虫

```go
// internal/crawler/boss.go
package crawler

import (
	"context"

	"github.com/gocolly/colly/v2"
)

type BossCrawler struct {
	collector *colly.Collector
}

func (c *BossCrawler) Name() string {
	return "boss"
}

func (c *BossCrawler) Crawl(ctx context.Context, keywords []string, locations []string) ([]*Job, error) {
	collector := colly.NewCollector(
		colly.AllowedDomains("www.zhipin.com"),
		colly.UserAgent("Mozilla/5.0 ..."),
	)

	var jobs []*Job

	collector.OnHTML(".job-card-wrapper", func(e *colly.HTMLElement) {
		job := &Job{
			Title:       e.ChildText(".job-name"),
			Company:     e.ChildText(".company-name"),
			Location:    e.ChildText(".job-area"),
			SalaryRange: e.ChildText(".salary"),
			URL:         e.ChildAttr("a", "href"),
			Source:      "boss",
		}
		jobs = append(jobs, job)
	})

	for _, kw := range keywords {
		for _, loc := range locations {
			url := buildBossURL(kw, loc)
			collector.Visit(url)
		}
	}

	return jobs, nil
}

func buildBossURL(keyword, location string) string {
	return "https://www.zhipin.com/web/geek/job?query=" + keyword + "&city=" + location
}
```

### 7.3 调度器

```go
// internal/crawler/scheduler.go
package crawler

import (
	"context"
	"sync"
	"time"
)

type Scheduler struct {
	crawlers []Crawler
	interval time.Duration
}

func NewScheduler(interval time.Duration) *Scheduler {
	return &Scheduler{
		crawlers: GetAllCrawlers(),
		interval: interval,
	}
}

// RunAll 并行运行所有爬虫
func (s *Scheduler) RunAll(ctx context.Context, keywords []string, locations []string) []*Job {
	var allJobs []*Job
	var mu sync.Mutex
	var wg sync.WaitGroup

	for _, c := range s.crawlers {
		wg.Add(1)
		go func(crawler Crawler) {
			defer wg.Done()

			jobs, err := crawler.Crawl(ctx, keywords, locations)
			if err != nil {
				return
			}

			mu.Lock()
			allJobs = append(allJobs, jobs...)
			mu.Unlock()
		}(c)
	}

	wg.Wait()
	return allJobs
}

// StartScheduledCrawl 定时爬取
func (s *Scheduler) StartScheduledCrawl(ctx context.Context) {
	ticker := time.NewTicker(s.interval)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			// 执行爬取任务
			s.RunAll(ctx, []string{"java", "golang", "python"}, []string{"北京", "上海", "深圳"})
		}
	}
}
```

---

## 8. 配置与启动

### 8.1 环境变量

```bash
# .env
GRPC_PORT=50052
DATABASE_URL=postgres://user:pass@localhost:5432/cvibe
REDIS_URL=redis://localhost:6379
ELASTICSEARCH_URL=http://localhost:9200
MAX_WORKERS=100
```

### 8.2 启动命令

```bash
# 生成 Proto
protoc --go_out=. --go-grpc_out=. proto/search_service.proto

# 运行
cd search-service
go run cmd/server/main.go
```

### 8.3 依赖

```go
// go.mod
module github.com/cvibe/search-service

go 1.21

require (
    google.golang.org/grpc v1.60.0
    google.golang.org/protobuf v1.32.0
    github.com/elastic/go-elasticsearch/v8 v8.11.0
    github.com/gocolly/colly/v2 v2.1.0
    github.com/redis/go-redis/v9 v9.3.0
)
```
