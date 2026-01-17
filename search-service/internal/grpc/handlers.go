/*
gRPC Handlers - Search Service
================================================================================

完整实现 search_service.proto 定义的所有 RPC 方法
*/

package grpc

import (
	"context"
	"fmt"
	"log"
	"sort"
	"strings"
	"sync"
	"time"

	pb "github.com/cvibe/search-service/internal/grpc/proto"
	"github.com/cvibe/search-service/internal/matching"
	"github.com/cvibe/search-service/internal/search"
)

// SearchServiceServer gRPC 服务实现
type SearchServiceServer struct {
	pb.UnimplementedSearchServiceServer
	engine  *search.Engine
	matcher *matching.Matcher
	// 模拟数据存储
	jobs       []pb.Job
	jobsMu     sync.RWMutex
	trending   []string
	trendingMu sync.RWMutex
}

// NewSearchServiceServer 创建服务实例
func NewSearchServiceServer(engine *search.Engine, matcher *matching.Matcher) *SearchServiceServer {
	s := &SearchServiceServer{
		engine:   engine,
		matcher:  matcher,
		trending: []string{"软件工程师", "后端开发", "前端开发", "全栈工程师", "数据分析"},
	}
	// 初始化模拟数据
	s.initMockData()
	return s
}

// initMockData 初始化模拟职位数据
func (s *SearchServiceServer) initMockData() {
	s.jobs = []pb.Job{
		{
			Id:             "job-001",
			Title:          "Senior Software Engineer",
			Company:        "Google",
			CompanyLogo:    "https://logo.clearbit.com/google.com",
			Location:       "Beijing, China",
			SalaryRange:    "$150,000 - $200,000",
			Experience:     "5+ years",
			EmploymentType: "full-time",
			Description:    "We are looking for a Senior Software Engineer to join our team...",
			Requirements:   []string{"5+ years experience", "Java or Python", "System Design"},
			Benefits:       []string{"Competitive salary", "Health insurance", "Stock options"},
			PostedAt:       time.Now().Add(-24 * time.Hour).Format(time.RFC3339),
			Source:         "direct",
			SourceUrl:      "https://careers.google.com/jobs/123",
		},
		{
			Id:             "job-002",
			Title:          "Backend Developer",
			Company:        "ByteDance",
			CompanyLogo:    "https://logo.clearbit.com/bytedance.com",
			Location:       "Shanghai, China",
			SalaryRange:    "$100,000 - $150,000",
			Experience:     "3-5 years",
			EmploymentType: "full-time",
			Description:    "Join our backend team to build scalable systems...",
			Requirements:   []string{"Go or Java", "Microservices", "Kubernetes"},
			Benefits:       []string{"Competitive salary", "Free meals", "Flexible hours"},
			PostedAt:       time.Now().Add(-48 * time.Hour).Format(time.RFC3339),
			Source:         "boss",
			SourceUrl:      "https://www.zhipin.com/job/123",
		},
		{
			Id:             "job-003",
			Title:          "Full Stack Developer",
			Company:        "Alibaba",
			CompanyLogo:    "https://logo.clearbit.com/alibaba.com",
			Location:       "Hangzhou, China",
			SalaryRange:    "$80,000 - $120,000",
			Experience:     "2-4 years",
			EmploymentType: "full-time",
			Description:    "Build next-generation e-commerce platform...",
			Requirements:   []string{"React/Vue", "Node.js or Java", "MySQL"},
			Benefits:       []string{"Stock options", "Annual bonus", "Education fund"},
			PostedAt:       time.Now().Add(-72 * time.Hour).Format(time.RFC3339),
			Source:         "lagou",
			SourceUrl:      "https://www.lagou.com/job/123",
		},
		{
			Id:             "job-004",
			Title:          "Frontend Engineer",
			Company:        "Tencent",
			CompanyLogo:    "https://logo.clearbit.com/tencent.com",
			Location:       "Shenzhen, China",
			SalaryRange:    "$70,000 - $100,000",
			Experience:     "2-3 years",
			EmploymentType: "full-time",
			Description:    "Create beautiful and performant web applications...",
			Requirements:   []string{"React", "TypeScript", "CSS"},
			Benefits:       []string{"Game discounts", "Health insurance", "Gym membership"},
			PostedAt:       time.Now().Add(-96 * time.Hour).Format(time.RFC3339),
			Source:         "direct",
			SourceUrl:      "https://careers.tencent.com/job/123",
		},
		{
			Id:             "job-005",
			Title:          "Data Scientist",
			Company:        "Huawei",
			CompanyLogo:    "https://logo.clearbit.com/huawei.com",
			Location:       "Beijing, China",
			SalaryRange:    "$120,000 - $180,000",
			Experience:     "3+ years",
			EmploymentType: "full-time",
			Description:    "Apply machine learning to solve complex problems...",
			Requirements:   []string{"Python", "TensorFlow/PyTorch", "Statistics"},
			Benefits:       []string{"High salary", "Research funding", "Patent bonus"},
			PostedAt:       time.Now().Add(-120 * time.Hour).Format(time.RFC3339),
			Source:         "linkedin",
			SourceUrl:      "https://www.linkedin.com/jobs/123",
		},
	}
}

// ==================== Job Search ====================

func (s *SearchServiceServer) SearchJobs(ctx context.Context, req *pb.SearchJobsRequest) (*pb.SearchJobsResponse, error) {
	log.Printf("SearchJobs: query=%s, page=%d", req.Query, req.Page)

	s.jobsMu.RLock()
	defer s.jobsMu.RUnlock()

	// 过滤职位
	var filtered []*pb.Job
	query := strings.ToLower(req.Query)

	for i := range s.jobs {
		job := &s.jobs[i]

		// 关键词匹配
		if query != "" {
			titleMatch := strings.Contains(strings.ToLower(job.Title), query)
			companyMatch := strings.Contains(strings.ToLower(job.Company), query)
			descMatch := strings.Contains(strings.ToLower(job.Description), query)
			if !titleMatch && !companyMatch && !descMatch {
				continue
			}
		}

		// 地点过滤
		if len(req.Locations) > 0 {
			locationMatch := false
			for _, loc := range req.Locations {
				if strings.Contains(strings.ToLower(job.Location), strings.ToLower(loc)) {
					locationMatch = true
					break
				}
			}
			if !locationMatch {
				continue
			}
		}

		// 经验级别过滤
		if req.ExperienceLevel != "" {
			// 简化匹配逻辑
			expMatch := strings.Contains(strings.ToLower(job.Experience), strings.ToLower(req.ExperienceLevel))
			if !expMatch {
				// 对于 entry/mid/senior 做特殊处理
				switch req.ExperienceLevel {
				case "entry":
					if !strings.Contains(job.Experience, "1") && !strings.Contains(job.Experience, "2") {
						continue
					}
				case "senior":
					if !strings.Contains(job.Experience, "5") && !strings.Contains(job.Experience, "+") {
						continue
					}
				}
			}
		}

		filtered = append(filtered, job)
	}

	// 排序
	switch req.SortBy {
	case "date":
		sort.Slice(filtered, func(i, j int) bool {
			return filtered[i].PostedAt > filtered[j].PostedAt
		})
	case "salary":
		// 简化：按公司名排序
		sort.Slice(filtered, func(i, j int) bool {
			return filtered[i].Company < filtered[j].Company
		})
	default: // relevance
		// 保持原顺序
	}

	// 分页
	page := int(req.Page)
	if page < 1 {
		page = 1
	}
	pageSize := int(req.PageSize)
	if pageSize < 1 || pageSize > 50 {
		pageSize = 20
	}

	total := len(filtered)
	totalPages := (total + pageSize - 1) / pageSize
	start := (page - 1) * pageSize
	end := start + pageSize
	if start > total {
		start = total
	}
	if end > total {
		end = total
	}

	paged := filtered[start:end]

	// 构建 facets
	facets := s.buildFacets(filtered)

	return &pb.SearchJobsResponse{
		Jobs:       paged,
		Total:      int32(total),
		Page:       int32(page),
		TotalPages: int32(totalPages),
		Facets:     facets,
	}, nil
}

func (s *SearchServiceServer) buildFacets(jobs []*pb.Job) []*pb.Facet {
	locationCounts := make(map[string]int)
	sourceCounts := make(map[string]int)

	for _, job := range jobs {
		locationCounts[job.Location]++
		sourceCounts[job.Source]++
	}

	var facets []*pb.Facet

	// Location facet
	var locationItems []*pb.FacetItem
	for loc, count := range locationCounts {
		locationItems = append(locationItems, &pb.FacetItem{Value: loc, Count: int32(count)})
	}
	facets = append(facets, &pb.Facet{Name: "location", Items: locationItems})

	// Source facet
	var sourceItems []*pb.FacetItem
	for src, count := range sourceCounts {
		sourceItems = append(sourceItems, &pb.FacetItem{Value: src, Count: int32(count)})
	}
	facets = append(facets, &pb.Facet{Name: "source", Items: sourceItems})

	return facets
}

// ==================== Resume-Job Matching ====================

func (s *SearchServiceServer) MatchResumeToJob(ctx context.Context, req *pb.MatchRequest) (*pb.MatchResponse, error) {
	log.Printf("MatchResumeToJob: user=%s", req.Resume.UserId)

	// 计算匹配分数
	skillMatch := s.calculateSkillMatch(req.Resume.Skills, req.Job.Requirements)
	expMatch := s.calculateExpMatch(int(req.Resume.YearsExperience), req.Job.Experience)
	titleMatch := s.calculateTitleMatch(req.Resume.Title, req.Job.Title)
	locationMatch := s.calculateLocationMatch(req.Resume.PreferredLocations, req.Job.Location)

	overall := skillMatch*0.5 + expMatch*0.2 + titleMatch*0.2 + locationMatch*0.1

	var matchReasons, gapReasons []string
	if skillMatch > 70 {
		matchReasons = append(matchReasons, "Strong skill match")
	}
	if expMatch > 70 {
		matchReasons = append(matchReasons, "Experience level fits")
	}
	if skillMatch < 50 {
		gapReasons = append(gapReasons, "Some required skills are missing")
	}

	return &pb.MatchResponse{
		OverallScore: overall,
		Details: &pb.MatchDetails{
			SkillMatch:      skillMatch,
			ExperienceMatch: expMatch,
			LocationMatch:   locationMatch,
			TitleMatch:      titleMatch,
		},
		MatchReasons: matchReasons,
		GapReasons:   gapReasons,
	}, nil
}

func (s *SearchServiceServer) calculateSkillMatch(userSkills []string, requirements []string) float64 {
	if len(requirements) == 0 {
		return 50.0
	}

	skillSet := make(map[string]bool)
	for _, skill := range userSkills {
		skillSet[strings.ToLower(skill)] = true
	}

	matched := 0
	for _, req := range requirements {
		for keyword := range skillSet {
			if strings.Contains(strings.ToLower(req), keyword) {
				matched++
				break
			}
		}
	}

	return float64(matched) / float64(len(requirements)) * 100
}

func (s *SearchServiceServer) calculateExpMatch(years int, expReq string) float64 {
	// 简化逻辑
	if years >= 5 && strings.Contains(expReq, "5+") {
		return 100.0
	}
	if years >= 3 && (strings.Contains(expReq, "3") || strings.Contains(expReq, "2-4")) {
		return 90.0
	}
	if years >= 1 {
		return 70.0
	}
	return 50.0
}

func (s *SearchServiceServer) calculateTitleMatch(userTitle, jobTitle string) float64 {
	userLower := strings.ToLower(userTitle)
	jobLower := strings.ToLower(jobTitle)

	if strings.Contains(jobLower, userLower) || strings.Contains(userLower, jobLower) {
		return 100.0
	}

	// 关键词匹配
	keywords := []string{"engineer", "developer", "architect", "lead", "senior", "junior"}
	matches := 0
	for _, kw := range keywords {
		if strings.Contains(userLower, kw) && strings.Contains(jobLower, kw) {
			matches++
		}
	}
	return float64(matches) * 20.0
}

func (s *SearchServiceServer) calculateLocationMatch(preferred []string, jobLoc string) float64 {
	jobLower := strings.ToLower(jobLoc)
	for _, loc := range preferred {
		if strings.Contains(jobLower, strings.ToLower(loc)) {
			return 100.0
		}
	}
	return 30.0 // 远程可能
}

// ==================== Batch Matching ====================

func (s *SearchServiceServer) BatchMatch(ctx context.Context, req *pb.BatchMatchRequest) (*pb.BatchMatchResponse, error) {
	log.Printf("BatchMatch: user=%s, jobs=%d", req.Resume.UserId, len(req.JobIds))

	var matches []*pb.JobMatch
	var wg sync.WaitGroup
	var mu sync.Mutex
	processed := 0
	failed := 0

	for _, jobId := range req.JobIds {
		wg.Add(1)
		go func(id string) {
			defer wg.Done()

			job := s.findJobById(id)
			if job == nil {
				mu.Lock()
				failed++
				mu.Unlock()
				return
			}

			resp, err := s.MatchResumeToJob(ctx, &pb.MatchRequest{
				Resume: req.Resume,
				Job:    job,
			})

			mu.Lock()
			if err != nil {
				failed++
			} else {
				matches = append(matches, &pb.JobMatch{
					JobId:   id,
					Score:   resp.OverallScore,
					Details: resp.Details,
				})
				processed++
			}
			mu.Unlock()
		}(jobId)
	}

	wg.Wait()

	// 按分数排序
	sort.Slice(matches, func(i, j int) bool {
		return matches[i].Score > matches[j].Score
	})

	return &pb.BatchMatchResponse{
		Matches:   matches,
		Processed: int32(processed),
		Failed:    int32(failed),
	}, nil
}

func (s *SearchServiceServer) findJobById(id string) *pb.Job {
	s.jobsMu.RLock()
	defer s.jobsMu.RUnlock()

	for i := range s.jobs {
		if s.jobs[i].Id == id {
			return &s.jobs[i]
		}
	}
	return nil
}

// ==================== Recommendations ====================

func (s *SearchServiceServer) GetJobRecommendations(ctx context.Context, req *pb.RecommendRequest) (*pb.RecommendResponse, error) {
	log.Printf("GetJobRecommendations: user=%s, limit=%d", req.UserId, req.Limit)

	s.jobsMu.RLock()
	defer s.jobsMu.RUnlock()

	// 排除已查看的职位
	excludeSet := make(map[string]bool)
	for _, id := range req.ExcludeJobIds {
		excludeSet[id] = true
	}

	var candidates []*pb.Job
	for i := range s.jobs {
		if !excludeSet[s.jobs[i].Id] {
			candidates = append(candidates, &s.jobs[i])
		}
	}

	// 计算每个候选职位的匹配分数
	type scored struct {
		job   *pb.Job
		score float64
	}

	var scoredJobs []scored
	for _, job := range candidates {
		resp, _ := s.MatchResumeToJob(ctx, &pb.MatchRequest{
			Resume: req.Resume,
			Job:    job,
		})
		scoredJobs = append(scoredJobs, scored{job: job, score: resp.OverallScore})
	}

	// 排序
	sort.Slice(scoredJobs, func(i, j int) bool {
		return scoredJobs[i].score > scoredJobs[j].score
	})

	// 取 top N
	limit := int(req.Limit)
	if limit <= 0 || limit > len(scoredJobs) {
		limit = len(scoredJobs)
	}
	if limit > 10 {
		limit = 10
	}

	var recommendations []*pb.RecommendedJob
	for i := 0; i < limit; i++ {
		sj := scoredJobs[i]
		recommendations = append(recommendations, &pb.RecommendedJob{
			Job:             sj.job,
			MatchScore:      sj.score,
			RecommendReason: fmt.Sprintf("Based on your skills and experience, this job has a %.0f%% match", sj.score),
		})
	}

	return &pb.RecommendResponse{
		Recommendations: recommendations,
	}, nil
}

// ==================== Search Suggestions ====================

func (s *SearchServiceServer) GetSearchSuggestions(ctx context.Context, req *pb.SuggestionRequest) (*pb.SuggestionResponse, error) {
	log.Printf("GetSearchSuggestions: prefix=%s", req.Prefix)

	prefix := strings.ToLower(req.Prefix)
	limit := int(req.Limit)
	if limit <= 0 {
		limit = 5
	}

	s.jobsMu.RLock()
	defer s.jobsMu.RUnlock()

	// 收集匹配的标题和公司
	suggestions := make(map[string]*pb.Suggestion)

	for _, job := range s.jobs {
		titleLower := strings.ToLower(job.Title)
		companyLower := strings.ToLower(job.Company)

		if strings.Contains(titleLower, prefix) {
			key := "title:" + job.Title
			if _, exists := suggestions[key]; !exists {
				suggestions[key] = &pb.Suggestion{
					Text:  job.Title,
					Type:  "job_title",
					Count: 1,
				}
			} else {
				suggestions[key].Count++
			}
		}

		if strings.Contains(companyLower, prefix) {
			key := "company:" + job.Company
			if _, exists := suggestions[key]; !exists {
				suggestions[key] = &pb.Suggestion{
					Text:  job.Company,
					Type:  "company",
					Count: 1,
				}
			} else {
				suggestions[key].Count++
			}
		}
	}

	// 转换为切片
	var result []*pb.Suggestion
	for _, sugg := range suggestions {
		result = append(result, sugg)
	}

	// 按 count 排序
	sort.Slice(result, func(i, j int) bool {
		return result[i].Count > result[j].Count
	})

	if len(result) > limit {
		result = result[:limit]
	}

	return &pb.SuggestionResponse{
		Suggestions: result,
	}, nil
}

// ==================== Trending Searches ====================

func (s *SearchServiceServer) GetTrendingSearches(ctx context.Context, req *pb.TrendingRequest) (*pb.TrendingResponse, error) {
	log.Printf("GetTrendingSearches: location=%s", req.Location)

	s.trendingMu.RLock()
	defer s.trendingMu.RUnlock()

	limit := int(req.Limit)
	if limit <= 0 {
		limit = 10
	}

	var items []*pb.TrendingItem
	for i, keyword := range s.trending {
		if i >= limit {
			break
		}
		items = append(items, &pb.TrendingItem{
			Keyword:     keyword,
			SearchCount: int32(1000 - i*100),
			GrowthRate:  float64(10 - i),
		})
	}

	return &pb.TrendingResponse{
		Items: items,
	}, nil
}

// ==================== Crawler Control ====================

func (s *SearchServiceServer) TriggerCrawl(ctx context.Context, req *pb.CrawlRequest) (*pb.CrawlResponse, error) {
	log.Printf("TriggerCrawl: source=%s, keywords=%v", req.Source, req.Keywords)

	// 模拟触发爬虫
	taskId := fmt.Sprintf("crawl-%d", time.Now().Unix())

	return &pb.CrawlResponse{
		TaskId:        taskId,
		Status:        "queued",
		EstimatedJobs: 100,
	}, nil
}

// ==================== Analytics ====================

func (s *SearchServiceServer) GetAnalytics(ctx context.Context, req *pb.AnalyticsRequest) (*pb.AnalyticsResponse, error) {
	log.Printf("GetAnalytics: metric=%s, timeRange=%s", req.Metric, req.TimeRange)

	// 生成模拟数据
	var dataPoints []*pb.DataPoint
	now := time.Now()

	days := 7
	switch req.TimeRange {
	case "30d":
		days = 30
	case "90d":
		days = 90
	case "1y":
		days = 365
	}

	for i := 0; i < days && i < 30; i++ {
		date := now.AddDate(0, 0, -i)
		value := 100.0 + float64(i)*2 // 模拟数据

		dataPoints = append(dataPoints, &pb.DataPoint{
			Date:  date.Format("2006-01-02"),
			Value: value,
			Label: req.Metric,
		})
	}

	summary := map[string]float64{
		"average": 150.0,
		"max":     200.0,
		"min":     100.0,
		"growth":  5.5,
	}

	return &pb.AnalyticsResponse{
		DataPoints: dataPoints,
		Summary:    summary,
	}, nil
}
