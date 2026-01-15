/*
Matching Module - 职位匹配算法
================================================================================

高性能的职位匹配：
- 技能匹配
- 经验匹配
- 学历匹配
- 综合评分

使用并行计算加速
*/

package matching

import (
	"context"
	"sort"
	"sync"

	"github.com/cvibe/search-service/internal/crawler"
)

// UserProfile 用户画像（简化版）
type UserProfile struct {
	UserID          string   `json:"user_id"`
	Skills          []string `json:"skills"`
	ExperienceYears int      `json:"experience_years"`
	Education       string   `json:"education"`
	TargetRoles     []string `json:"target_roles"`
	TargetLocations []string `json:"target_locations"`
	SalaryExpect    int      `json:"salary_expect"`
}

// MatchResult 匹配结果
type MatchResult struct {
	Job           crawler.JobPosting `json:"job"`
	Score         float64            `json:"score"`
	SkillMatch    float64            `json:"skill_match"`
	ExpMatch      float64            `json:"exp_match"`
	LocationMatch float64            `json:"location_match"`
	MatchedSkills []string           `json:"matched_skills"`
	MissingSkills []string           `json:"missing_skills"`
}

// Matcher 匹配器
type Matcher struct {
	// 权重配置
	skillWeight    float64
	expWeight      float64
	locationWeight float64
	eduWeight      float64
}

// NewMatcher 创建匹配器
func NewMatcher() *Matcher {
	return &Matcher{
		skillWeight:    0.5, // 技能匹配占 50%
		expWeight:      0.2, // 经验匹配占 20%
		locationWeight: 0.2, // 地点匹配占 20%
		eduWeight:      0.1, // 学历匹配占 10%
	}
}

// MatchJobs 批量匹配职位（并行计算）
func (m *Matcher) MatchJobs(ctx context.Context, profile UserProfile, jobs []crawler.JobPosting, topK int) []MatchResult {
	results := make([]MatchResult, len(jobs))
	
	// 并行计算每个职位的匹配分数
	var wg sync.WaitGroup
	for i, job := range jobs {
		wg.Add(1)
		go func(idx int, j crawler.JobPosting) {
			defer wg.Done()
			results[idx] = m.matchSingle(profile, j)
		}(i, job)
	}
	wg.Wait()
	
	// 按分数排序
	sort.Slice(results, func(i, j int) bool {
		return results[i].Score > results[j].Score
	})
	
	// 返回 Top K
	if topK > 0 && topK < len(results) {
		return results[:topK]
	}
	return results
}

// matchSingle 计算单个职位的匹配分数
func (m *Matcher) matchSingle(profile UserProfile, job crawler.JobPosting) MatchResult {
	result := MatchResult{
		Job: job,
	}
	
	// 技能匹配
	skillScore, matched, missing := m.calculateSkillMatch(profile.Skills, job.Skills)
	result.SkillMatch = skillScore
	result.MatchedSkills = matched
	result.MissingSkills = missing
	
	// 经验匹配
	result.ExpMatch = m.calculateExpMatch(profile.ExperienceYears, job.ExperienceYears)
	
	// 地点匹配
	result.LocationMatch = m.calculateLocationMatch(profile.TargetLocations, job.Location)
	
	// 综合评分
	result.Score = result.SkillMatch*m.skillWeight +
		result.ExpMatch*m.expWeight +
		result.LocationMatch*m.locationWeight
	
	return result
}

// calculateSkillMatch 计算技能匹配分数
func (m *Matcher) calculateSkillMatch(userSkills, jobSkills []string) (score float64, matched, missing []string) {
	if len(jobSkills) == 0 {
		return 0.5, nil, nil // 无要求时给中等分
	}
	
	userSkillSet := make(map[string]bool)
	for _, s := range userSkills {
		userSkillSet[s] = true
	}
	
	for _, skill := range jobSkills {
		if userSkillSet[skill] {
			matched = append(matched, skill)
		} else {
			missing = append(missing, skill)
		}
	}
	
	score = float64(len(matched)) / float64(len(jobSkills)) * 100
	return
}

// calculateExpMatch 计算经验匹配分数
func (m *Matcher) calculateExpMatch(userExp int, jobExpReq string) float64 {
	// TODO: 解析 jobExpReq（如 "3-5年"）进行匹配
	// 占位实现
	return 80.0
}

// calculateLocationMatch 计算地点匹配分数
func (m *Matcher) calculateLocationMatch(userLocations []string, jobLocation string) float64 {
	for _, loc := range userLocations {
		if loc == jobLocation {
			return 100.0
		}
	}
	return 0.0
}
