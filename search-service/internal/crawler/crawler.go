/*
Crawler Module - 职位爬虫
================================================================================

每日抓取各大招聘平台的职位信息：
- Boss直聘
- 拉勾
- 智联招聘
- 猎聘

TODO: 后续独立开发完善
*/

package crawler

import (
	"context"
	"time"
)

// JobPosting 职位信息
type JobPosting struct {
	ID              string    `json:"id"`
	Title           string    `json:"title"`
	Company         string    `json:"company"`
	Location        string    `json:"location"`
	SalaryMin       int       `json:"salary_min"`
	SalaryMax       int       `json:"salary_max"`
	ExperienceYears string    `json:"experience_years"`
	Education       string    `json:"education"`
	Description     string    `json:"description"`
	Requirements    []string  `json:"requirements"`
	Skills          []string  `json:"skills"`
	SourceURL       string    `json:"source_url"`
	SourcePlatform  string    `json:"source_platform"`
	PostedAt        time.Time `json:"posted_at"`
	CrawledAt       time.Time `json:"crawled_at"`
}

// Crawler 爬虫接口
type Crawler interface {
	// Crawl 执行爬取
	Crawl(ctx context.Context, config CrawlConfig) ([]JobPosting, error)
	// Platform 返回平台名称
	Platform() string
}

// CrawlConfig 爬取配置
type CrawlConfig struct {
	Keywords  []string `json:"keywords"`
	Locations []string `json:"locations"`
	MaxPages  int      `json:"max_pages"`
}

// BaseCrawler 基础爬虫实现
type BaseCrawler struct {
	platform string
}

func (c *BaseCrawler) Platform() string {
	return c.platform
}
