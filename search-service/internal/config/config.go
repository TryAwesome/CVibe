/*
Config - 配置管理
*/

package config

import (
	"os"
)

// Config 应用配置
type Config struct {
	GRPCPort string
	
	// Database
	DBHost     string
	DBPort     string
	DBUser     string
	DBPassword string
	DBName     string
	
	// Elasticsearch
	ESHost string
	
	// Redis
	RedisHost     string
	RedisPassword string
	
	// Crawler
	CrawlerCron     string
	CrawlerKeywords []string
}

// Load 加载配置
func Load() *Config {
	return &Config{
		GRPCPort:        getEnv("GRPC_PORT", "50052"),
		DBHost:          getEnv("DB_HOST", "localhost"),
		DBPort:          getEnv("DB_PORT", "5432"),
		DBUser:          getEnv("DB_USER", "cvibe"),
		DBPassword:      getEnv("DB_PASSWORD", "cvibe"),
		DBName:          getEnv("DB_NAME", "cvibe"),
		ESHost:          getEnv("ES_HOST", "http://localhost:9200"),
		RedisHost:       getEnv("REDIS_HOST", "localhost:6379"),
		RedisPassword:   getEnv("REDIS_PASSWORD", ""),
		CrawlerCron:     getEnv("CRAWLER_CRON", "0 2 * * *"), // 每天凌晨2点
		CrawlerKeywords: []string{"软件工程师", "后端开发", "前端开发", "全栈"},
	}
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
