/*
爬虫调度器
================================================================================

每日定时执行爬取任务
*/

package crawler

import (
	"context"
	"log"
	"sync"
	"time"
)

// Scheduler 爬虫调度器
type Scheduler struct {
	crawlers []Crawler
	storage  Storage
	config   SchedulerConfig
	running  bool
	mu       sync.Mutex
}

// SchedulerConfig 调度器配置
type SchedulerConfig struct {
	// CronExpr cron 表达式，默认每天凌晨2点
	CronExpr string `json:"cron_expr"`
	// Keywords 搜索关键词
	Keywords []string `json:"keywords"`
	// Locations 目标城市
	Locations []string `json:"locations"`
	// MaxPagesPerPlatform 每个平台最大爬取页数
	MaxPagesPerPlatform int `json:"max_pages_per_platform"`
}

// Storage 存储接口
type Storage interface {
	SaveJobs(ctx context.Context, jobs []JobPosting) error
	GetJobsByDate(ctx context.Context, date time.Time) ([]JobPosting, error)
}

// NewScheduler 创建调度器
func NewScheduler(storage Storage, config SchedulerConfig) *Scheduler {
	s := &Scheduler{
		storage: storage,
		config:  config,
	}
	
	// 注册所有爬虫
	s.crawlers = []Crawler{
		NewBossCrawler(),
		NewLagouCrawler(),
	}
	
	return s
}

// Start 启动调度器
func (s *Scheduler) Start() {
	s.mu.Lock()
	if s.running {
		s.mu.Unlock()
		return
	}
	s.running = true
	s.mu.Unlock()
	
	// TODO: 使用 cron 库实现定时任务
	// c := cron.New()
	// c.AddFunc(s.config.CronExpr, s.RunOnce)
	// c.Start()
	
	log.Println("Crawler scheduler started")
}

// Stop 停止调度器
func (s *Scheduler) Stop() {
	s.mu.Lock()
	s.running = false
	s.mu.Unlock()
	log.Println("Crawler scheduler stopped")
}

// RunOnce 执行一次爬取
func (s *Scheduler) RunOnce() {
	ctx := context.Background()
	config := CrawlConfig{
		Keywords:  s.config.Keywords,
		Locations: s.config.Locations,
		MaxPages:  s.config.MaxPagesPerPlatform,
	}
	
	var allJobs []JobPosting
	var wg sync.WaitGroup
	var mu sync.Mutex
	
	// 并行执行所有爬虫
	for _, crawler := range s.crawlers {
		wg.Add(1)
		go func(c Crawler) {
			defer wg.Done()
			
			jobs, err := c.Crawl(ctx, config)
			if err != nil {
				log.Printf("Crawler %s failed: %v", c.Platform(), err)
				return
			}
			
			mu.Lock()
			allJobs = append(allJobs, jobs...)
			mu.Unlock()
			
			log.Printf("Crawler %s fetched %d jobs", c.Platform(), len(jobs))
		}(crawler)
	}
	
	wg.Wait()
	
	// 保存到存储
	if len(allJobs) > 0 {
		if err := s.storage.SaveJobs(ctx, allJobs); err != nil {
			log.Printf("Failed to save jobs: %v", err)
		} else {
			log.Printf("Saved %d jobs total", len(allJobs))
		}
	}
}
