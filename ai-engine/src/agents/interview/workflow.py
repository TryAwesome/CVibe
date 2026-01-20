"""
Interview Agent Workflow - 详细深入的简历信息采集面试
================================================================================

业务流程：
1. Interview 是构建「个人资料库」的核心入口
2. 需要详细深入提问，覆盖简历所需的所有方面：
   - 基本信息（教育背景、技能）
   - 工作经历（每段经历都要深入追问细节、成果、数据）
   - 科研/项目经历（深入了解技术细节、贡献、难点）
   - 获奖荣誉、证书
   - 个人特质、职业目标
3. 面试结果会更新到用户的「个人资料库」

TODO: 后续独立优化开发此 Agent Workflow
"""

from typing import Optional, TYPE_CHECKING
from dataclasses import dataclass, field
from enum import Enum

# Use TYPE_CHECKING to avoid import issues
if TYPE_CHECKING:
    from ...llm import LLMClient


class InterviewPhase(Enum):
    """面试阶段"""
    INTRO = "intro"                    # 开场介绍
    EDUCATION = "education"            # 教育背景
    SKILLS = "skills"                  # 技能评估
    WORK_EXPERIENCE = "work_experience"  # 工作经历
    PROJECT = "project"                # 项目/科研经历
    ACHIEVEMENT = "achievement"        # 获奖/成就
    CAREER_GOAL = "career_goal"        # 职业目标
    SUMMARY = "summary"                # 总结


@dataclass
class InterviewContext:
    """面试上下文"""
    session_id: str
    user_id: str
    current_phase: InterviewPhase = InterviewPhase.INTRO
    collected_data: dict = field(default_factory=dict)
    conversation_history: list = field(default_factory=list)
    current_topic_depth: int = 0  # 当前话题深入程度


class InterviewAgentWorkflow:
    """
    Interview Agent Workflow
    
    核心功能：
    - 深入细致的提问，挖掘用户经历细节
    - 多轮追问，确保每个经历都有足够深度
    - 结构化数据收集，构建个人资料库
    
    占位实现 - 后续独立优化开发
    """

    SYSTEM_PROMPT = """你是一位专业的职业顾问，正在进行一次深入的背景采集面试。

你的目标是全面了解用户的背景，收集简历所需的所有信息：
1. 教育背景：学校、专业、GPA、相关课程
2. 技能：编程语言、框架、工具、软技能
3. 工作经历：公司、职位、职责、成就（要有具体数据和成果）
4. 项目/科研：技术栈、你的贡献、难点、结果
5. 获奖/证书
6. 职业目标

面试要求：
- 对每段经历都要深入追问细节，不要浅尝辄止
- 追问具体的数据和成果（"提升了多少%？"、"影响了多少用户？"）
- 关于技术选型要追问原因
- 关于困难要追问如何解决
- 每个话题至少追问2-3轮才能进入下一个

当前阶段：{phase}
已收集信息：{collected_summary}
"""

    def __init__(self, llm_client: "LLMClient"):
        self.llm = llm_client

    def start_session(self, session_id: str, user_id: str) -> tuple[InterviewContext, str]:
        """
        开始面试会话
        
        Returns:
            (context, first_question)
        """
        context = InterviewContext(session_id=session_id, user_id=user_id)
        
        # 占位：返回开场问题
        first_question = "你好！我是你的职业顾问。接下来我会详细了解你的背景，帮助你构建完整的个人资料库。首先，请介绍一下你的教育背景——你在哪里读书，什么专业，什么时候毕业？"
        
        context.conversation_history.append({
            "role": "assistant",
            "content": first_question
        })
        
        return context, first_question

    def process_answer(
        self, 
        context: InterviewContext, 
        user_answer: str
    ) -> tuple[InterviewContext, str, Optional[dict]]:
        """
        处理用户回答，生成下一个问题
        
        Args:
            context: 面试上下文
            user_answer: 用户回答
            
        Returns:
            (updated_context, next_question, extracted_data)
        """
        context.conversation_history.append({
            "role": "user",
            "content": user_answer
        })

        # TODO: 实际实现时调用 LLM 进行：
        # 1. 从回答中提取结构化数据
        # 2. 判断是否需要追问（深度不够）
        # 3. 判断是否切换到下一阶段
        # 4. 生成下一个问题

        # 占位实现
        extracted_data = self._extract_data_placeholder(context, user_answer)
        next_question = self._generate_next_question_placeholder(context)
        
        context.conversation_history.append({
            "role": "assistant", 
            "content": next_question
        })

        return context, next_question, extracted_data

    def _extract_data_placeholder(self, context: InterviewContext, answer: str) -> dict:
        """占位：提取结构化数据"""
        # TODO: 使用 LLM 提取
        return {
            "phase": context.current_phase.value,
            "raw_answer": answer,
            "structured": {}  # 待实现
        }

    def _generate_next_question_placeholder(self, context: InterviewContext) -> str:
        """占位：生成下一个问题"""
        # TODO: 使用 LLM 根据上下文生成
        
        phase_questions = {
            InterviewPhase.INTRO: "请详细介绍一下你的教育背景，包括学校、专业、GPA等。",
            InterviewPhase.EDUCATION: "在校期间有什么印象深刻的课程或项目吗？学到了什么？",
            InterviewPhase.SKILLS: "你熟悉哪些编程语言和技术框架？分别用在什么场景？",
            InterviewPhase.WORK_EXPERIENCE: "请介绍你最近的一份工作经历，你的职责是什么？",
            InterviewPhase.PROJECT: "能详细说说你负责的某个具体项目吗？技术栈是什么？",
            InterviewPhase.ACHIEVEMENT: "有没有什么获奖或证书？",
            InterviewPhase.CAREER_GOAL: "你的职业目标是什么？希望往什么方向发展？",
            InterviewPhase.SUMMARY: "感谢你的分享！我已经收集了你的详细背景信息。",
        }
        
        return phase_questions.get(
            context.current_phase, 
            "能再详细说说这个吗？具体做了什么，有什么成果？"
        )

    def end_session(self, context: InterviewContext) -> dict:
        """
        结束面试，返回收集的完整数据
        
        Returns:
            用户个人资料库数据
        """
        # TODO: 整合所有收集的数据，生成结构化的个人资料库
        return {
            "session_id": context.session_id,
            "user_id": context.user_id,
            "collected_data": context.collected_data,
            "profile": {
                "education": [],
                "skills": [],
                "work_experience": [],
                "projects": [],
                "achievements": [],
                "career_goal": "",
            }
        }
