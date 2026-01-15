"""
Growth Advisor Agent Workflow - 个人成长规划
================================================================================

业务流程：
1. 用户上传目标职位的 HC (Hiring Criteria) 要求
2. AI 对比 HC 与用户「个人资料库」
3. 找出技能差距
4. 制定学习路径
5. 提供具体的、可执行的建议

TODO: 后续独立优化开发此 Agent Workflow
"""

from typing import Optional
from dataclasses import dataclass, field

from ..llm import LLMClient


@dataclass
class SkillGap:
    """技能差距"""
    skill_name: str
    current_level: int  # 0-10
    required_level: int  # 0-10
    gap: int
    priority: str  # high, medium, low
    learning_resources: list[str] = field(default_factory=list)


@dataclass
class LearningMilestone:
    """学习里程碑"""
    id: str
    title: str
    description: str
    skills_covered: list[str] = field(default_factory=list)
    estimated_time: str = ""  # e.g., "2 weeks"
    resources: list[str] = field(default_factory=list)
    deliverables: list[str] = field(default_factory=list)  # 可验证的产出


@dataclass
class LearningPath:
    """学习路径"""
    target_role: str
    target_company: str
    total_duration: str
    milestones: list[LearningMilestone] = field(default_factory=list)
    skill_gaps: list[SkillGap] = field(default_factory=list)


class GrowthAdvisorAgentWorkflow:
    """
    Growth Advisor Agent Workflow
    
    核心功能：
    - 分析用户当前能力与目标职位的差距
    - 制定个性化学习路径
    - 提供具体可执行的建议
    - 推荐学习资源
    
    占位实现 - 后续独立优化开发
    """

    SYSTEM_PROMPT = """你是一位资深的职业发展顾问。

你的任务是帮助用户分析与目标职位的差距，并制定切实可行的学习计划。

要求：
1. 分析要客观，不要过度打击也不要过度鼓励
2. 差距分析要具体到技能点
3. 学习路径要有明确的里程碑和时间节点
4. 每个里程碑要有可验证的产出
5. 建议要具体可执行，不要空泛

用户当前资料：
{user_profile}

目标职位要求：
{target_hc}
"""

    def __init__(self, llm_client: LLMClient):
        self.llm = llm_client

    def analyze_gap(
        self,
        user_profile: dict,
        target_hc: dict,
    ) -> list[SkillGap]:
        """
        分析技能差距
        
        Args:
            user_profile: 用户个人资料库
            target_hc: 目标职位要求
            
        Returns:
            技能差距列表
        """
        # TODO: 使用 LLM 进行深度分析
        # 占位实现
        
        user_skills = set(s.lower() for s in user_profile.get("skills", []))
        required_skills = set(s.lower() for s in target_hc.get("required_skills", []))
        
        gaps = []
        for skill in required_skills:
            if skill not in user_skills:
                gaps.append(SkillGap(
                    skill_name=skill,
                    current_level=0,
                    required_level=7,
                    gap=7,
                    priority="high",
                    learning_resources=[f"推荐学习资源：{skill} 官方文档"],
                ))
            else:
                # 已有技能，可能需要提升
                gaps.append(SkillGap(
                    skill_name=skill,
                    current_level=5,
                    required_level=8,
                    gap=3,
                    priority="medium",
                    learning_resources=[f"进阶学习：{skill} 高级用法"],
                ))
        
        # 按优先级排序
        priority_order = {"high": 0, "medium": 1, "low": 2}
        gaps.sort(key=lambda x: priority_order.get(x.priority, 2))
        
        return gaps

    def generate_learning_path(
        self,
        user_profile: dict,
        target_hc: dict,
        available_time_per_week: int = 10,  # 每周可用学习时间（小时）
    ) -> LearningPath:
        """
        生成学习路径
        
        Args:
            user_profile: 用户资料
            target_hc: 目标职位要求
            available_time_per_week: 每周学习时间
            
        Returns:
            学习路径
        """
        gaps = self.analyze_gap(user_profile, target_hc)
        
        # TODO: 使用 LLM 生成详细的学习路径
        # 占位实现
        
        milestones = []
        for i, gap in enumerate(gaps[:5], 1):  # 取前5个优先级最高的
            milestones.append(LearningMilestone(
                id=f"milestone_{i}",
                title=f"掌握 {gap.skill_name}",
                description=f"从{gap.current_level}级提升到{gap.required_level}级",
                skills_covered=[gap.skill_name],
                estimated_time="2 weeks" if gap.gap > 5 else "1 week",
                resources=gap.learning_resources,
                deliverables=[
                    f"完成 {gap.skill_name} 实战项目",
                    f"输出学习笔记",
                ],
            ))
        
        return LearningPath(
            target_role=target_hc.get("job_title", "目标职位"),
            target_company=target_hc.get("company", "目标公司"),
            total_duration=self._estimate_duration(gaps, available_time_per_week),
            milestones=milestones,
            skill_gaps=gaps,
        )

    def _estimate_duration(self, gaps: list[SkillGap], hours_per_week: int) -> str:
        """估算学习总时长"""
        total_gap = sum(g.gap for g in gaps)
        # 假设每个差距点需要 10 小时学习
        total_hours = total_gap * 10
        weeks = total_hours // hours_per_week
        
        if weeks <= 4:
            return "1 个月"
        elif weeks <= 12:
            return f"{weeks // 4} 个月"
        else:
            return "3 个月以上"

    def get_actionable_suggestions(
        self,
        user_profile: dict,
        target_hc: dict,
    ) -> list[dict]:
        """
        获取具体可执行的建议
        
        Returns:
            建议列表，每个建议包含具体的行动步骤
        """
        # TODO: 使用 LLM 生成具体建议
        # 占位实现
        
        gaps = self.analyze_gap(user_profile, target_hc)
        suggestions = []
        
        for gap in gaps[:3]:  # 取最高优先级的3个
            suggestions.append({
                "skill": gap.skill_name,
                "priority": gap.priority,
                "action": f"系统学习 {gap.skill_name}",
                "steps": [
                    f"1. 阅读 {gap.skill_name} 官方文档",
                    f"2. 完成入门教程",
                    f"3. 做一个实战项目",
                    f"4. 总结输出到博客或笔记",
                ],
                "resources": gap.learning_resources,
                "expected_outcome": f"能够独立使用 {gap.skill_name} 完成实际工作",
                "time_estimate": "2-3周",
            })
        
        return suggestions
