"""
Job Recommender Agent Workflow - 职位智能推荐
================================================================================

业务流程：
1. 每天抓取所有职位（由 Go search-service 负责）
2. 根据用户「个人资料库」进行智能匹配
3. 返回与用户背景高度重合的职位
4. 支持跳转到原职位页面
5. 上次推荐的结果会展示在 Dashboard

TODO: 后续独立优化开发此 Agent Workflow
"""

from typing import Optional
from dataclasses import dataclass, field

from ...llm import LLMClient


@dataclass
class JobPosting:
    """职位信息"""
    id: str
    title: str
    company: str
    location: str
    salary_range: Optional[str] = None
    required_skills: list[str] = field(default_factory=list)
    preferred_skills: list[str] = field(default_factory=list)
    description: str = ""
    requirements: list[str] = field(default_factory=list)
    responsibilities: list[str] = field(default_factory=list)
    source_url: str = ""  # 原始链接，支持跳转
    source_platform: str = ""  # boss直聘、拉勾等
    posted_date: str = ""


@dataclass
class UserProfile:
    """用户资料库（简化版）"""
    user_id: str
    skills: list[str] = field(default_factory=list)
    experience_years: int = 0
    education_level: str = ""
    target_roles: list[str] = field(default_factory=list)
    target_locations: list[str] = field(default_factory=list)
    salary_expectation: Optional[str] = None


@dataclass
class JobMatch:
    """职位匹配结果"""
    job: JobPosting
    match_score: float  # 0-100
    matched_skills: list[str] = field(default_factory=list)
    missing_skills: list[str] = field(default_factory=list)
    match_reasons: list[str] = field(default_factory=list)
    recommendation_text: str = ""


class JobRecommenderAgentWorkflow:
    """
    Job Recommender Agent Workflow
    
    核心功能：
    - 解析用户资料库，提取关键技能和偏好
    - 与职位要求进行语义匹配
    - 生成个性化推荐理由
    - 指出技能差距
    
    占位实现 - 后续独立优化开发
    """

    SYSTEM_PROMPT = """你是一位职业规划专家，帮助用户匹配合适的工作机会。

你的任务：
1. 分析用户的技能、经验、偏好
2. 理解职位的要求和期望
3. 评估匹配程度
4. 给出推荐理由
5. 指出可能的差距

用户资料：
{user_profile}

职位信息：
{job_info}

请分析匹配度，并给出推荐建议。
"""

    def __init__(self, llm_client: LLMClient):
        self.llm = llm_client

    def match_jobs(
        self,
        user_profile: UserProfile,
        jobs: list[JobPosting],
        top_k: int = 10,
    ) -> list[JobMatch]:
        """
        为用户匹配职位
        
        Args:
            user_profile: 用户资料
            jobs: 职位列表（由 Go service 抓取）
            top_k: 返回前 k 个最匹配的职位
            
        Returns:
            排序后的匹配结果
        """
        matches = []
        
        for job in jobs:
            match = self._calculate_match(user_profile, job)
            matches.append(match)
        
        # 按匹配分数排序
        matches.sort(key=lambda x: x.match_score, reverse=True)
        
        return matches[:top_k]

    def _calculate_match(self, profile: UserProfile, job: JobPosting) -> JobMatch:
        """
        计算单个职位的匹配度
        
        占位实现 - 后续使用 LLM 进行语义匹配
        """
        # 简单的技能匹配（占位）
        user_skills_lower = [s.lower() for s in profile.skills]
        required_skills_lower = [s.lower() for s in job.required_skills]
        
        matched = [s for s in required_skills_lower if s in user_skills_lower]
        missing = [s for s in required_skills_lower if s not in user_skills_lower]
        
        # 简单计算匹配分数
        if len(required_skills_lower) > 0:
            score = (len(matched) / len(required_skills_lower)) * 100
        else:
            score = 50.0
        
        return JobMatch(
            job=job,
            match_score=round(score, 1),
            matched_skills=matched,
            missing_skills=missing,
            match_reasons=self._generate_match_reasons_placeholder(matched, profile, job),
            recommendation_text=self._generate_recommendation_placeholder(job, score),
        )

    def _generate_match_reasons_placeholder(
        self, 
        matched_skills: list[str], 
        profile: UserProfile, 
        job: JobPosting
    ) -> list[str]:
        """占位：生成匹配理由"""
        reasons = []
        if matched_skills:
            reasons.append(f"你掌握了 {len(matched_skills)} 项核心技能要求")
        if profile.target_roles and job.title:
            reasons.append(f"职位与你的目标方向相符")
        return reasons

    def _generate_recommendation_placeholder(self, job: JobPosting, score: float) -> str:
        """占位：生成推荐文案"""
        if score >= 80:
            return f"强烈推荐！这个 {job.company} 的 {job.title} 职位与你的背景高度匹配。"
        elif score >= 60:
            return f"推荐！{job.company} 的这个职位与你的技能较为匹配，值得尝试。"
        else:
            return f"可以考虑。虽然有一些技能差距，但可以作为发展方向。"

    def generate_personalized_recommendation(
        self,
        user_profile: UserProfile,
        match: JobMatch,
    ) -> str:
        """
        生成个性化推荐说明（使用 LLM）
        
        占位实现
        """
        # TODO: 调用 LLM 生成更自然的推荐文案
        return match.recommendation_text

    def analyze_skill_gap(
        self,
        user_profile: UserProfile,
        target_job: JobPosting,
    ) -> dict:
        """
        分析用户与目标职位的技能差距
        
        Returns:
            差距分析报告
        """
        # 占位实现
        required = set(s.lower() for s in target_job.required_skills)
        current = set(s.lower() for s in user_profile.skills)
        
        missing = list(required - current)
        
        return {
            "target_job": target_job.title,
            "target_company": target_job.company,
            "current_skills": list(current),
            "required_skills": list(required),
            "missing_skills": missing,
            "gap_severity": "moderate" if len(missing) < 3 else "significant",
            "learning_suggestions": [f"建议学习 {s}" for s in missing[:3]],
        }
