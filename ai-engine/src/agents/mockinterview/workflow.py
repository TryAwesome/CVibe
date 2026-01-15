"""
Mock Interview Agent Workflow - 高压模拟面试
================================================================================

业务流程：
1. 面试类型：
   - Coding：大厂题库随机抽取
   - 八股文：技术知识点拷问
   - 简历深挖：对简历细节追根刨底
   - 业务面试：业务场景问题

2. 面试风格：
   - 有压迫感，不要一直鼓励
   - 持怀疑态度，追问细节
   - 模拟真实面试的紧张感

3. 评估维度：
   - 技术深度
   - 表达清晰度
   - 问题解决能力
   - 抗压能力

TODO: 后续独立优化开发此 Agent Workflow
"""

from typing import Optional
from dataclasses import dataclass, field
from enum import Enum

from ..llm import LLMClient


class MockInterviewType(Enum):
    """模拟面试类型"""
    CODING = "coding"           # Coding 面试
    FUNDAMENTALS = "fundamentals"  # 八股文
    RESUME_DEEP_DIVE = "resume_deep_dive"  # 简历深挖
    BUSINESS = "business"       # 业务面试
    BEHAVIORAL = "behavioral"   # 行为面试
    SYSTEM_DESIGN = "system_design"  # 系统设计


class InterviewerTone(Enum):
    """面试官语气"""
    SKEPTICAL = "skeptical"     # 怀疑的
    PRESSING = "pressing"       # 追问的
    NEUTRAL = "neutral"         # 中性的
    CHALLENGING = "challenging"  # 挑战的


@dataclass
class CodingQuestion:
    """Coding 题目"""
    id: str
    title: str
    difficulty: str  # easy, medium, hard
    company_tags: list[str] = field(default_factory=list)
    description: str = ""
    examples: list[dict] = field(default_factory=list)
    constraints: list[str] = field(default_factory=list)
    solution_hints: list[str] = field(default_factory=list)


@dataclass
class MockInterviewContext:
    """模拟面试上下文"""
    session_id: str
    user_id: str
    interview_type: MockInterviewType
    current_question_index: int = 0
    questions: list = field(default_factory=list)
    answers: list = field(default_factory=list)
    evaluations: list = field(default_factory=list)
    conversation_history: list = field(default_factory=list)
    pressure_level: int = 5  # 1-10 压迫感等级


class MockInterviewAgentWorkflow:
    """
    Mock Interview Agent Workflow
    
    核心功能：
    - 根据面试类型准备题目
    - 以怀疑和追问的态度进行面试
    - 制造压迫感，模拟真实面试
    - 对简历细节深入追问
    - 提供详细的评估反馈
    
    占位实现 - 后续独立优化开发
    """

    SYSTEM_PROMPT_CODING = """你是一位严格的技术面试官，正在进行 Coding 面试。

面试风格：
- 保持专业但略带怀疑的态度
- 对答案追问时间复杂度、空间复杂度
- 如果代码有 bug，直接指出并要求修改
- 追问是否有更优解
- 不要给太多提示，让候选人自己思考
- 适当施加时间压力

当前题目：{question}
"""

    SYSTEM_PROMPT_FUNDAMENTALS = """你是一位严格的技术面试官，正在考察技术基础知识（八股文）。

面试风格：
- 问题要有深度，不只是表面定义
- 追问原理、底层实现
- 问"为什么"比"是什么"更重要
- 候选人回答不完整时要追问
- 保持怀疑态度："你确定吗？"、"真的是这样吗？"

当前话题：{topic}
"""

    SYSTEM_PROMPT_RESUME = """你是一位严格的面试官，正在对候选人的简历进行深度追问。

面试风格：
- 对每个项目/经历都要追根刨底
- 追问具体的数据和成果
- 质疑不合理的地方："这个数据是怎么得出的？"
- 追问技术细节："为什么选择这个技术？有没有考虑过其他方案？"
- 追问困难和挑战："遇到过什么困难？怎么解决的？"
- 追问个人贡献："这是团队成果还是你个人的贡献？你具体负责什么？"

候选人简历关键信息：
{resume_highlights}
"""

    SYSTEM_PROMPT_BUSINESS = """你是一位产品/业务面试官，正在考察候选人的业务理解能力。

面试风格：
- 提出业务场景问题
- 追问思考过程
- 质疑方案的可行性
- 追问如何平衡各方利益
- 考察商业敏感度

业务场景：{scenario}
"""

    def __init__(self, llm_client: LLMClient):
        self.llm = llm_client
        self._question_bank = self._load_question_bank()

    def _load_question_bank(self) -> dict:
        """加载题库（占位）"""
        # TODO: 从数据库或文件加载真实题库
        return {
            "coding": [
                CodingQuestion(
                    id="lc-1",
                    title="Two Sum",
                    difficulty="easy",
                    company_tags=["Google", "Amazon", "Meta"],
                    description="Given an array of integers nums and an integer target...",
                ),
                CodingQuestion(
                    id="lc-146",
                    title="LRU Cache",
                    difficulty="medium",
                    company_tags=["Amazon", "Microsoft", "ByteDance"],
                    description="Design a data structure that follows the constraints of LRU cache...",
                ),
            ],
            "fundamentals": [
                {"topic": "HashMap 底层原理", "followups": ["红黑树转换条件", "扩容机制", "线程安全"]},
                {"topic": "JVM 垃圾回收", "followups": ["GC Roots", "分代回收", "G1 vs ZGC"]},
                {"topic": "TCP 三次握手", "followups": ["为什么不是两次", "TIME_WAIT", "半连接队列"]},
            ],
        }

    def start_session(
        self,
        session_id: str,
        user_id: str,
        interview_type: MockInterviewType,
        user_resume: Optional[dict] = None,
    ) -> tuple[MockInterviewContext, str]:
        """
        开始模拟面试
        
        Returns:
            (context, first_message)
        """
        context = MockInterviewContext(
            session_id=session_id,
            user_id=user_id,
            interview_type=interview_type,
        )

        # 根据类型准备题目
        if interview_type == MockInterviewType.CODING:
            context.questions = self._select_coding_questions()
            first_message = self._start_coding_interview(context)
        elif interview_type == MockInterviewType.FUNDAMENTALS:
            context.questions = self._select_fundamental_topics()
            first_message = self._start_fundamentals_interview(context)
        elif interview_type == MockInterviewType.RESUME_DEEP_DIVE:
            context.questions = self._prepare_resume_questions(user_resume)
            first_message = self._start_resume_interview(context, user_resume)
        else:
            first_message = "让我们开始面试。准备好了吗？"

        context.conversation_history.append({
            "role": "assistant",
            "content": first_message
        })

        return context, first_message

    def _select_coding_questions(self, count: int = 2) -> list:
        """随机选择 Coding 题目"""
        # TODO: 实现智能选题（根据难度、公司偏好等）
        return self._question_bank.get("coding", [])[:count]

    def _select_fundamental_topics(self, count: int = 3) -> list:
        """选择八股文话题"""
        return self._question_bank.get("fundamentals", [])[:count]

    def _prepare_resume_questions(self, resume: Optional[dict]) -> list:
        """根据简历准备追问点"""
        # TODO: 解析简历，找出可追问的点
        return []

    def _start_coding_interview(self, context: MockInterviewContext) -> str:
        """开始 Coding 面试"""
        if not context.questions:
            return "今天没有准备好题目，下次再来。"
        
        question = context.questions[0]
        return f"""好的，我们开始 Coding 面试。

第一题：{question.title}

{question.description}

你有15分钟时间，计时开始。请先说一下你的思路。"""

    def _start_fundamentals_interview(self, context: MockInterviewContext) -> str:
        """开始八股文面试"""
        if not context.questions:
            return "我们开始技术基础面试。"
        
        topic = context.questions[0]
        return f"""好的，我们来聊聊技术基础。

首先，请你详细解释一下 {topic['topic']}。不要只说表面，我要听你对底层原理的理解。"""

    def _start_resume_interview(self, context: MockInterviewContext, resume: Optional[dict]) -> str:
        """开始简历深挖面试"""
        return """我看了你的简历，有一些问题想详细了解。

首先，你简历上写的第一段工作经历，能详细说说你具体负责什么吗？
我想听你说说，你个人的贡献是什么，不要说"我们团队"怎样怎样。"""

    def process_answer(
        self,
        context: MockInterviewContext,
        user_answer: str,
    ) -> tuple[MockInterviewContext, str, Optional[dict]]:
        """
        处理用户回答
        
        Returns:
            (updated_context, next_message, evaluation)
        """
        context.conversation_history.append({
            "role": "user",
            "content": user_answer
        })
        context.answers.append(user_answer)

        # TODO: 使用 LLM 分析回答，决定下一步
        # 1. 回答不完整 -> 追问
        # 2. 回答有漏洞 -> 质疑
        # 3. 回答完整 -> 进入下一题

        # 占位实现：生成追问或下一题
        evaluation = self._evaluate_answer_placeholder(context, user_answer)
        next_message = self._generate_response_placeholder(context, evaluation)

        context.conversation_history.append({
            "role": "assistant",
            "content": next_message
        })
        context.evaluations.append(evaluation)

        return context, next_message, evaluation

    def _evaluate_answer_placeholder(self, context: MockInterviewContext, answer: str) -> dict:
        """占位：评估回答"""
        return {
            "score": 7,
            "completeness": "partial",
            "needs_followup": True,
            "weak_points": [],
            "strong_points": [],
        }

    def _generate_response_placeholder(self, context: MockInterviewContext, evaluation: dict) -> str:
        """占位：生成面试官回复（带压迫感）"""
        skeptical_responses = [
            "嗯...你确定是这样吗？再想想。",
            "这个答案不够完整。具体一点，底层是怎么实现的？",
            "好的。但是我有个疑问，如果遇到这种情况呢？你怎么处理？",
            "这个数据是怎么得出来的？有具体的监控数据支撑吗？",
            "你说的这个功能，你自己写的还是调用的现成的？具体说说你的贡献。",
            "时间复杂度多少？空间复杂度呢？能优化吗？",
        ]
        
        import random
        return random.choice(skeptical_responses)

    def end_session(self, context: MockInterviewContext) -> dict:
        """
        结束面试，返回完整评估报告
        """
        return {
            "session_id": context.session_id,
            "interview_type": context.interview_type.value,
            "total_questions": len(context.questions),
            "overall_score": 7.0,  # TODO: 计算真实分数
            "dimensions": {
                "technical_depth": 7,
                "communication": 8,
                "problem_solving": 7,
                "stress_handling": 6,
            },
            "strengths": ["技术基础扎实"],
            "weaknesses": ["细节描述不够具体"],
            "suggestions": ["多准备项目的量化成果"],
            "detailed_feedback": context.evaluations,
        }
