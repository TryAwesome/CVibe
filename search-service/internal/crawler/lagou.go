/*
拉勾爬虫
================================================================================

TODO: 实现 拉勾网 职位抓取
*/

package crawler

import (
	"context"
)

// LagouCrawler 拉勾爬虫
type LagouCrawler struct {
	BaseCrawler
}

// NewLagouCrawler 创建拉勾爬虫
func NewLagouCrawler() *LagouCrawler {
	return &LagouCrawler{
		BaseCrawler: BaseCrawler{platform: "lagou"},
	}
}

// Crawl 爬取拉勾职位
func (c *LagouCrawler) Crawl(ctx context.Context, config CrawlConfig) ([]JobPosting, error) {
	// TODO: 实现爬取逻辑
	return []JobPosting{}, nil
}
