"""
Resume Builder Agent Workflow - 基于HC要求的智能简历构建
================================================================================

业务流程：
1. 用户选择或上传 LaTeX 模板
2. 用户提供目标职位的 HC (Hiring Criteria) 要求
3. AI 检索用户「个人资料库」，找到与 HC 匹配的经历
4. AI 对经历进行润色、着重强调相关技能和成果
5. 将内容填充到 LaTeX 模板的相应位置
6. 用户在左侧编辑器精细调整 LaTeX，右侧实时预览
7. 支持导出 PDF（导出后自动加入简历历史）

TODO: 后续独立优化开发此 Agent Workflow
"""

from typing import Optional
from dataclasses import dataclass, field
from enum import Enum

from ...llm import LLMClient


class ResumeSection(Enum):
    """简历区块"""
    HEADER = "header"          # 头部（姓名、联系方式）
    SUMMARY = "summary"        # 个人简介
    EDUCATION = "education"    # 教育背景
    EXPERIENCE = "experience"  # 工作经历
    PROJECTS = "projects"      # 项目经历
    SKILLS = "skills"          # 技能
    ACHIEVEMENTS = "achievements"  # 获奖/证书


@dataclass
class HiringCriteria:
    """职位招聘要求"""
    job_title: str
    company: str
    required_skills: list[str] = field(default_factory=list)
    preferred_skills: list[str] = field(default_factory=list)
    responsibilities: list[str] = field(default_factory=list)
    requirements: list[str] = field(default_factory=list)
    raw_jd: str = ""


@dataclass
class ResumeContent:
    """简历内容"""
    template_id: str
    latex_content: str
    sections: dict = field(default_factory=dict)
    highlighted_skills: list[str] = field(default_factory=list)


class ResumeBuilderAgentWorkflow:
    """
    Resume Builder Agent Workflow
    
    核心功能：
    - 解析 HC 要求，提取关键技能和职责
    - 从个人资料库中检索匹配的经历
    - 润色经历描述，突出与 HC 相关的成果
    - 生成 LaTeX 内容
    
    占位实现 - 后续独立优化开发
    """

    SYSTEM_PROMPT = """你是一位专业的简历撰写专家。

你的任务是根据目标职位的要求，从用户的个人资料库中选择最相关的经历，并进行专业的润色：

1. 分析职位要求（HC），提取关键技能和期望
2. 从用户资料库中匹配最相关的经历
3. 对经历进行润色：
   - 使用 STAR 法则（Situation, Task, Action, Result）
   - 突出量化成果（数据、百分比、影响范围）
   - 强调与 HC 匹配的技能
   - 使用动作动词开头
4. 确保内容真实，只润色不虚构

目标职位：{job_title} @ {company}
关键要求：{key_requirements}
"""

    def __init__(self, llm_client: LLMClient):
        self.llm = llm_client

    def parse_hiring_criteria(self, jd_text: str) -> HiringCriteria:
        """
        解析职位描述，提取招聘要求
        
        Args:
            jd_text: 职位描述原文
            
        Returns:
            结构化的招聘要求
        """
        # TODO: 使用 LLM 解析 JD
        # 占位实现
        return HiringCriteria(
            job_title="Software Engineer",
            company="Tech Company",
            required_skills=["Python", "Java", "SQL"],
            preferred_skills=["Kubernetes", "AWS"],
            responsibilities=["Develop features", "Code review"],
            requirements=["3+ years experience", "CS degree"],
            raw_jd=jd_text,
        )

    def match_experiences(
        self, 
        user_profile: dict, 
        criteria: HiringCriteria
    ) -> dict:
        """
        从用户资料库匹配与 HC 相关的经历
        
        Args:
            user_profile: 用户个人资料库
            criteria: 招聘要求
            
        Returns:
            匹配的经历及相关度评分
        """
        # TODO: 使用语义匹配或 LLM 进行智能匹配
        # 占位实现
        return {
            "matched_experiences": user_profile.get("work_experience", []),
            "matched_projects": user_profile.get("projects", []),
            "matched_skills": criteria.required_skills,
            "relevance_scores": {},
        }

    def polish_experience(
        self, 
        experience: dict, 
        criteria: HiringCriteria
    ) -> str:
        """
        润色单条经历描述
        
        Args:
            experience: 原始经历
            criteria: 招聘要求（用于突出相关点）
            
        Returns:
            润色后的描述
        """
        # TODO: 使用 LLM 润色
        # 占位实现
        return experience.get("description", "")

    def generate_resume_content(
        self,
        user_profile: dict,
        criteria: HiringCriteria,
        template_id: str,
    ) -> ResumeContent:
        """
        生成完整的简历内容
        
        Args:
            user_profile: 用户个人资料库
            criteria: 目标职位要求
            template_id: LaTeX 模板 ID
            
        Returns:
            生成的简历内容
        """
        # TODO: 完整实现
        # 1. 匹配经历
        # 2. 润色每条经历
        # 3. 组装到 LaTeX 模板
        
        # 占位实现
        sections = {
            ResumeSection.HEADER.value: self._generate_header_placeholder(user_profile),
            ResumeSection.SUMMARY.value: self._generate_summary_placeholder(user_profile, criteria),
            ResumeSection.EDUCATION.value: self._generate_education_placeholder(user_profile),
            ResumeSection.EXPERIENCE.value: self._generate_experience_placeholder(user_profile, criteria),
            ResumeSection.PROJECTS.value: self._generate_projects_placeholder(user_profile, criteria),
            ResumeSection.SKILLS.value: self._generate_skills_placeholder(user_profile, criteria),
        }

        latex_content = self._assemble_latex_placeholder(template_id, sections)

        return ResumeContent(
            template_id=template_id,
            latex_content=latex_content,
            sections=sections,
            highlighted_skills=criteria.required_skills,
        )

    def _generate_header_placeholder(self, profile: dict) -> str:
        """占位：生成头部"""
        return "% Header section placeholder"

    def _generate_summary_placeholder(self, profile: dict, criteria: HiringCriteria) -> str:
        """占位：生成个人简介"""
        return "% Summary section placeholder"

    def _generate_education_placeholder(self, profile: dict) -> str:
        """占位：生成教育背景"""
        return "% Education section placeholder"

    def _generate_experience_placeholder(self, profile: dict, criteria: HiringCriteria) -> str:
        """占位：生成工作经历"""
        return "% Experience section placeholder"

    def _generate_projects_placeholder(self, profile: dict, criteria: HiringCriteria) -> str:
        """占位：生成项目经历"""
        return "% Projects section placeholder"

    def _generate_skills_placeholder(self, profile: dict, criteria: HiringCriteria) -> str:
        """占位：生成技能列表"""
        return "% Skills section placeholder"

    def _assemble_latex_placeholder(self, template_id: str, sections: dict) -> str:
        """占位：组装 LaTeX"""
        return f"""\\documentclass{{article}}
% Template: {template_id}
\\begin{{document}}

{sections.get(ResumeSection.HEADER.value, '')}

{sections.get(ResumeSection.SUMMARY.value, '')}

{sections.get(ResumeSection.EDUCATION.value, '')}

{sections.get(ResumeSection.EXPERIENCE.value, '')}

{sections.get(ResumeSection.PROJECTS.value, '')}

{sections.get(ResumeSection.SKILLS.value, '')}

\\end{{document}}
"""

    def update_section(
        self,
        current_content: ResumeContent,
        section: ResumeSection,
        new_latex: str,
    ) -> ResumeContent:
        """
        更新单个区块内容（用户手动编辑后）
        
        Args:
            current_content: 当前简历内容
            section: 要更新的区块
            new_latex: 新的 LaTeX 内容
            
        Returns:
            更新后的简历内容
        """
        current_content.sections[section.value] = new_latex
        # 重新组装完整 LaTeX
        current_content.latex_content = self._assemble_latex_placeholder(
            current_content.template_id, 
            current_content.sections
        )
        return current_content
