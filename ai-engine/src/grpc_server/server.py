"""
AI Engine gRPC Service - 完整实现
================================================================================

实现 ai_engine.proto 定义的所有 RPC 方法
"""

import logging
from concurrent import futures
from typing import Iterator, Optional
import json
import uuid

import grpc

# 导入生成的 proto 代码
try:
    from ..generated import ai_engine_pb2 as pb
    from ..generated import ai_engine_pb2_grpc as pb_grpc
except ImportError:
    # 开发时可能还未生成
    pb = None
    pb_grpc = None

from ..config import settings
from ..llm import (
    LLMClient, 
    VisionClient,
    create_llm_client,
    create_vision_client,
    get_default_llm_config,
    get_default_vision_config,
)
from ..agents.resume.parser import ResumeParser, ParsedResume

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# ================== Session Store ==================

class SessionStore:
    """简单的内存会话存储（生产环境应使用 Redis）"""
    
    def __init__(self):
        self._sessions: dict = {}
    
    def get(self, session_id: str) -> Optional[dict]:
        return self._sessions.get(session_id)
    
    def set(self, session_id: str, data: dict):
        self._sessions[session_id] = data
    
    def delete(self, session_id: str):
        self._sessions.pop(session_id, None)
    
    def update(self, session_id: str, **kwargs):
        if session_id in self._sessions:
            self._sessions[session_id].update(kwargs)


session_store = SessionStore()


# ================== AI Engine Service ==================

class AIEngineServicer:
    """AI Engine gRPC 服务实现"""

    def __init__(self):
        # 初始化默认 LLM 客户端
        self._default_llm: Optional[LLMClient] = None
        self._default_vision: Optional[VisionClient] = None
        self._init_default_clients()
    
    def _init_default_clients(self):
        """从环境变量初始化默认客户端"""
        llm_config = get_default_llm_config()
        if llm_config:
            self._default_llm = LLMClient(llm_config)
            logger.info(f"Default LLM initialized: {llm_config.provider}/{llm_config.model}")
        
        vision_config = get_default_vision_config()
        if vision_config:
            self._default_vision = VisionClient(vision_config)
            logger.info(f"Default Vision model initialized: {vision_config.provider}/{vision_config.model}")

    def _get_llm(self, user_config: Optional[dict] = None) -> Optional[LLMClient]:
        """获取 LLM 客户端"""
        if user_config and user_config.get("api_key"):
            return create_llm_client(
                api_key=user_config["api_key"],
                model=user_config.get("model", "gpt-4o"),
                provider=user_config.get("provider", "openai"),
                base_url=user_config.get("base_url"),
            )
        return self._default_llm

    # ================== Resume Parsing ==================
    
    def ParseResume(self, request, context):
        """解析简历文件"""
        logger.info(f"ParseResume: file={request.file_name}, type={request.file_type}")
        
        try:
            parser = ResumeParser(
                llm_client=self._default_llm,
                vision_client=self._default_vision
            )
            
            file_bytes = bytes(request.file_content)
            file_type = request.file_type.lower()
            
            if "pdf" in file_type:
                result = parser.parse_pdf_bytes(file_bytes)
            elif "image" in file_type or file_type in ["jpg", "jpeg", "png"]:
                result = parser.parse_image(file_bytes)
            else:
                # 尝试作为文本处理
                text = file_bytes.decode('utf-8', errors='ignore')
                result = parser.parse_text(text)
            
            # 构建响应
            resume_data = pb.ResumeData(
                name=result.name,
                email=result.email,
                phone=result.phone,
                summary=result.summary,
                skills=result.skills,
                raw_text=result.raw_text,
            )
            
            # 添加经历
            for exp in result.work_experience:
                resume_data.experiences.append(pb.ExperienceData(
                    company=exp.get("company", ""),
                    title=exp.get("title", ""),
                    start_date=exp.get("start_date", ""),
                    end_date=exp.get("end_date", ""),
                    description=exp.get("description", ""),
                    is_current=exp.get("is_current", False),
                ))
            
            # 添加教育
            for edu in result.education:
                resume_data.educations.append(pb.EducationData(
                    school=edu.get("school", ""),
                    degree=edu.get("degree", ""),
                    field=edu.get("field", ""),
                    start_date=edu.get("start_date", ""),
                    end_date=edu.get("end_date", ""),
                ))
            
            return pb.ParseResumeResponse(
                success=True,
                data=resume_data,
            )
            
        except Exception as e:
            logger.error(f"ParseResume error: {e}")
            return pb.ParseResumeResponse(
                success=False,
                error_message=str(e),
            )

    # ================== Resume Building ==================
    
    def BuildResume(self, request, context) -> Iterator:
        """构建简历（流式输出）"""
        logger.info(f"BuildResume: user={request.user_id}, job={request.job_title}")
        
        llm = self._get_llm()
        if not llm:
            yield pb.BuildResumeChunk(
                section="error",
                content="LLM not configured",
                is_final=True
            )
            return
        
        # 构建 prompt
        profile = request.profile
        prompt = f"""请为以下求职者生成一份针对性的简历内容。

目标职位: {request.job_title}
职位描述: {request.job_description}
语言: {"中文" if request.language == "zh" else "英文"}

求职者信息:
- 姓名: {profile.name}
- 当前职位: {profile.title}
- 个人简介: {profile.summary}
- 技能: {', '.join(profile.skills)}

请分段生成以下部分:
1. summary - 个人简介（针对目标职位优化）
2. experience - 工作经历（突出相关经验）
3. skills - 技能列表（匹配职位要求）
4. education - 教育背景

每个部分用 [SECTION:xxx] 标记开始。"""

        messages = [
            {"role": "system", "content": "你是一个专业的简历优化专家。"},
            {"role": "user", "content": prompt}
        ]
        
        current_section = "summary"
        try:
            for chunk in llm.stream_chat(messages):
                # 检测 section 标记
                if "[SECTION:" in chunk:
                    parts = chunk.split("[SECTION:")
                    for part in parts:
                        if "]" in part:
                            new_section = part.split("]")[0].lower()
                            current_section = new_section
                            content = part.split("]", 1)[1] if "]" in part else ""
                            if content:
                                yield pb.BuildResumeChunk(
                                    section=current_section,
                                    content=content,
                                    is_final=False
                                )
                        elif part:
                            yield pb.BuildResumeChunk(
                                section=current_section,
                                content=part,
                                is_final=False
                            )
                else:
                    yield pb.BuildResumeChunk(
                        section=current_section,
                        content=chunk,
                        is_final=False
                    )
            
            yield pb.BuildResumeChunk(
                section="complete",
                content="",
                is_final=True
            )
        except Exception as e:
            logger.error(f"BuildResume error: {e}")
            yield pb.BuildResumeChunk(
                section="error",
                content=str(e),
                is_final=True
            )

    # ================== AI Interview ==================
    
    def StartInterview(self, request, context):
        """开始 AI 面试会话"""
        logger.info(f"StartInterview: session={request.session_id}, job={request.job_title}")
        
        llm = self._get_llm()
        if not llm:
            return pb.StartInterviewResponse(
                success=False,
            )
        
        # 生成欢迎消息和第一个问题
        config = request.config
        language = "中文" if config.language == "zh" else "English"
        difficulty = config.difficulty or "medium"
        
        system_prompt = f"""你是一位专业的面试官，正在为候选人进行模拟面试。

职位: {request.job_title}
职位描述: {request.job_description}

候选人简历概要:
{request.resume_content[:2000]}

面试设置:
- 语言: {language}
- 难度: {difficulty}
- 重点领域: {', '.join(config.focus_areas) if config.focus_areas else '综合'}

请用{language}进行面试。每次只问一个问题，根据候选人的回答进行追问或切换话题。
保持专业、友好的态度。"""

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": "请开始面试。"}
        ]
        
        try:
            response = llm.chat(messages)
            
            # 存储会话
            session_store.set(request.session_id, {
                "type": "interview",
                "user_id": request.user_id,
                "job_title": request.job_title,
                "system_prompt": system_prompt,
                "messages": messages + [{"role": "assistant", "content": response}],
                "config": {
                    "language": config.language,
                    "difficulty": difficulty,
                }
            })
            
            # 分离欢迎语和问题
            parts = response.split("\n\n", 1)
            welcome = parts[0] if len(parts) > 1 else "欢迎参加面试！"
            question = parts[1] if len(parts) > 1 else response
            
            return pb.StartInterviewResponse(
                success=True,
                welcome_message=welcome,
                first_question=question,
            )
        except Exception as e:
            logger.error(f"StartInterview error: {e}")
            return pb.StartInterviewResponse(
                success=False,
            )

    def SendInterviewMessage(self, request, context) -> Iterator:
        """发送面试消息（流式响应）"""
        logger.info(f"SendInterviewMessage: session={request.session_id}")
        
        session = session_store.get(request.session_id)
        if not session:
            yield pb.MessageChunk(
                content="Session not found",
                is_final=True
            )
            return
        
        llm = self._get_llm()
        if not llm:
            yield pb.MessageChunk(
                content="LLM not configured",
                is_final=True
            )
            return
        
        # 添加用户消息到历史
        messages = session["messages"]
        messages.append({"role": "user", "content": request.user_message})
        
        try:
            full_response = ""
            for chunk in llm.stream_chat(messages):
                full_response += chunk
                yield pb.MessageChunk(
                    content=chunk,
                    is_final=False
                )
            
            # 更新会话
            messages.append({"role": "assistant", "content": full_response})
            session_store.update(request.session_id, messages=messages)
            
            yield pb.MessageChunk(
                content="",
                is_final=True
            )
        except Exception as e:
            logger.error(f"SendInterviewMessage error: {e}")
            yield pb.MessageChunk(
                content=str(e),
                is_final=True
            )

    # ================== Mock Interview ==================
    
    def StartMockInterview(self, request, context):
        """开始模拟面试"""
        logger.info(f"StartMockInterview: session={request.session_id}, type={request.interview_type}")
        
        llm = self._get_llm()
        if not llm:
            return pb.StartMockResponse(success=False)
        
        # 根据类型生成问题
        interview_type = request.interview_type or "MIXED"
        question_count = min(max(request.question_count, 3), 10)
        language = "中文" if request.language == "zh" else "English"
        
        prompt = f"""请为以下职位生成 {question_count} 个面试问题：

职位: {request.job_title}
面试类型: {interview_type}
语言: {language}

请按以下 JSON 格式输出：
[
    {{"question": "问题内容", "category": "technical/behavioral/situational", "time_limit": 120}}
]"""

        messages = [
            {"role": "system", "content": "你是面试问题生成专家。"},
            {"role": "user", "content": prompt}
        ]
        
        try:
            response = llm.chat(messages)
            
            # 解析问题
            try:
                if "```json" in response:
                    json_str = response.split("```json")[1].split("```")[0]
                elif "```" in response:
                    json_str = response.split("```")[1].split("```")[0]
                else:
                    json_str = response
                questions = json.loads(json_str.strip())
            except:
                # 使用默认问题
                questions = [
                    {"question": "请介绍一下你自己", "category": "behavioral", "time_limit": 120},
                    {"question": "你最大的优势是什么？", "category": "behavioral", "time_limit": 90},
                    {"question": "描述一个你克服困难的经历", "category": "situational", "time_limit": 180},
                ]
            
            # 存储会话
            session_store.set(request.session_id, {
                "type": "mock_interview",
                "user_id": request.user_id,
                "job_title": request.job_title,
                "interview_type": interview_type,
                "questions": questions,
                "current_index": 0,
                "answers": [],
                "evaluations": [],
            })
            
            return pb.StartMockResponse(
                success=True,
                session_id=request.session_id,
                total_questions=len(questions),
            )
        except Exception as e:
            logger.error(f"StartMockInterview error: {e}")
            return pb.StartMockResponse(success=False)

    def GetNextQuestion(self, request, context):
        """获取下一个问题"""
        session = session_store.get(request.session_id)
        if not session:
            return pb.QuestionResponse(question_index=-1, question="Session not found")
        
        questions = session.get("questions", [])
        index = request.question_index
        
        if index >= len(questions):
            return pb.QuestionResponse(question_index=-1, question="No more questions")
        
        q = questions[index]
        return pb.QuestionResponse(
            question_index=index,
            question=q.get("question", ""),
            category=q.get("category", "general"),
            time_limit_seconds=q.get("time_limit", 120),
        )

    def EvaluateAnswer(self, request, context):
        """评估答案"""
        session = session_store.get(request.session_id)
        if not session:
            return pb.EvaluationResponse(score=0, feedback="Session not found")
        
        llm = self._get_llm()
        if not llm:
            return pb.EvaluationResponse(score=50, feedback="LLM not configured")
        
        prompt = f"""请评估以下面试回答：

问题: {request.question}
回答: {request.answer_text}

请给出:
1. 分数 (0-100)
2. 详细反馈
3. 优点 (列表)
4. 改进建议 (列表)

以 JSON 格式输出：
{{"score": 80, "feedback": "...", "strengths": ["...", "..."], "improvements": ["...", "..."]}}"""

        messages = [
            {"role": "system", "content": "你是面试评估专家。"},
            {"role": "user", "content": prompt}
        ]
        
        try:
            response = llm.chat(messages)
            
            # 解析响应
            try:
                if "```json" in response:
                    json_str = response.split("```json")[1].split("```")[0]
                else:
                    json_str = response
                data = json.loads(json_str.strip())
            except:
                data = {"score": 70, "feedback": response, "strengths": [], "improvements": []}
            
            # 存储评估结果
            session["answers"].append(request.answer_text)
            session["evaluations"].append(data)
            session_store.set(request.session_id, session)
            
            return pb.EvaluationResponse(
                score=data.get("score", 70),
                feedback=data.get("feedback", ""),
                strengths=data.get("strengths", []),
                improvements=data.get("improvements", []),
            )
        except Exception as e:
            logger.error(f"EvaluateAnswer error: {e}")
            return pb.EvaluationResponse(score=50, feedback=str(e))

    def FinishMockInterview(self, request, context):
        """结束模拟面试，生成报告"""
        session = session_store.get(request.session_id)
        if not session:
            return pb.MockReportResponse(overall_score=0)
        
        evaluations = session.get("evaluations", [])
        questions = session.get("questions", [])
        answers = session.get("answers", [])
        
        # 计算总分
        total_score = sum(e.get("score", 0) for e in evaluations)
        overall_score = int(total_score / len(evaluations)) if evaluations else 0
        
        # 生成结果
        results = []
        for i, (q, a, e) in enumerate(zip(questions, answers, evaluations)):
            results.append(pb.QuestionResult(
                index=i,
                question=q.get("question", ""),
                answer=a,
                score=e.get("score", 0),
                feedback=e.get("feedback", ""),
            ))
        
        # 生成建议
        recommendations = []
        for e in evaluations:
            recommendations.extend(e.get("improvements", []))
        recommendations = list(set(recommendations))[:5]
        
        # 清理会话
        session_store.delete(request.session_id)
        
        return pb.MockReportResponse(
            overall_score=overall_score,
            overall_feedback=f"面试完成！总分: {overall_score}/100",
            results=results,
            recommendations=recommendations,
        )

    # ================== Growth - Gap Analysis ==================
    
    def AnalyzeGap(self, request, context):
        """分析技能差距"""
        logger.info(f"AnalyzeGap: user={request.user_id}, goal={request.goal_title}")
        
        llm = self._get_llm()
        if not llm:
            return pb.GapAnalysisResponse(readiness_score=0)
        
        profile = request.current_profile
        prompt = f"""请分析以下求职者与目标职位的差距：

目标职位: {request.goal_title}
目标日期: {request.target_date}

当前情况:
- 职位: {profile.title}
- 技能: {', '.join(profile.skills)}

请分析:
1. 技能差距（每个技能的当前水平和所需水平）
2. 准备度评分 (0-100)
3. 改进建议

以 JSON 格式输出：
{{
    "gaps": [
        {{"skill": "技能名", "current_level": "NONE/BASIC/INTERMEDIATE/ADVANCED", "required_level": "...", "priority": 1-5}}
    ],
    "recommendations": ["建议1", "建议2"],
    "readiness_score": 65
}}"""

        messages = [
            {"role": "system", "content": "你是职业发展顾问。"},
            {"role": "user", "content": prompt}
        ]
        
        try:
            response = llm.chat(messages)
            
            # 解析响应
            try:
                if "```json" in response:
                    json_str = response.split("```json")[1].split("```")[0]
                else:
                    json_str = response
                data = json.loads(json_str.strip())
            except:
                data = {"gaps": [], "recommendations": [response], "readiness_score": 50}
            
            # 构建响应
            gaps = [
                pb.GapItem(
                    skill=g.get("skill", ""),
                    current_level=g.get("current_level", "BASIC"),
                    required_level=g.get("required_level", "ADVANCED"),
                    priority=g.get("priority", 3),
                )
                for g in data.get("gaps", [])
            ]
            
            return pb.GapAnalysisResponse(
                gaps=gaps,
                recommendations=data.get("recommendations", []),
                readiness_score=data.get("readiness_score", 50),
            )
        except Exception as e:
            logger.error(f"AnalyzeGap error: {e}")
            return pb.GapAnalysisResponse(readiness_score=0)

    # ================== Growth - Learning Path ==================
    
    def GenerateLearningPath(self, request, context) -> Iterator:
        """生成学习路径（流式）"""
        logger.info(f"GenerateLearningPath: user={request.user_id}, goal={request.goal_id}")
        
        llm = self._get_llm()
        if not llm:
            yield pb.LearningPathChunk(phase="error", content="LLM not configured", is_final=True)
            return
        
        gaps = [f"{g.skill}: {g.current_level} -> {g.required_level}" for g in request.gaps]
        
        prompt = f"""请为以下技能差距生成详细的学习路径：

技能差距:
{chr(10).join(gaps)}

学习偏好: {request.preferred_style or 'MIXED'}

请分阶段输出学习计划，每个阶段包含：
- 阶段名称
- 学习目标
- 具体资源和行动
- 预计时间

用 [PHASE:阶段名] 标记每个阶段的开始。"""

        messages = [
            {"role": "system", "content": "你是学习规划专家。"},
            {"role": "user", "content": prompt}
        ]
        
        current_phase = "overview"
        try:
            for chunk in llm.stream_chat(messages):
                if "[PHASE:" in chunk:
                    parts = chunk.split("[PHASE:")
                    for part in parts:
                        if "]" in part:
                            phase_name = part.split("]")[0]
                            current_phase = phase_name
                            content = part.split("]", 1)[1] if "]" in part else ""
                            if content:
                                yield pb.LearningPathChunk(
                                    phase=current_phase,
                                    content=content,
                                    is_final=False
                                )
                        elif part:
                            yield pb.LearningPathChunk(
                                phase=current_phase,
                                content=part,
                                is_final=False
                            )
                else:
                    yield pb.LearningPathChunk(
                        phase=current_phase,
                        content=chunk,
                        is_final=False
                    )
            
            yield pb.LearningPathChunk(
                phase="complete",
                content="",
                is_final=True
            )
        except Exception as e:
            logger.error(f"GenerateLearningPath error: {e}")
            yield pb.LearningPathChunk(
                phase="error",
                content=str(e),
                is_final=True
            )

    # ================== Job Analysis ==================
    
    def AnalyzeJob(self, request, context):
        """分析职位"""
        logger.info(f"AnalyzeJob: job={request.job_title}")
        
        llm = self._get_llm()
        if not llm:
            return pb.JobAnalysisResponse()
        
        prompt = f"""请分析以下职位信息：

职位: {request.job_title}
公司: {request.company}
描述: {request.job_description}

请提取：
1. 必需技能
2. 加分技能
3. 经验级别
4. 预估薪资范围
5. 面试准备建议

以 JSON 格式输出：
{{
    "required_skills": ["..."],
    "nice_to_have_skills": ["..."],
    "experience_level": "entry/mid/senior/lead",
    "salary_estimate": "$xxx - $xxx",
    "interview_tips": ["..."]
}}"""

        messages = [
            {"role": "system", "content": "你是职位分析专家。"},
            {"role": "user", "content": prompt}
        ]
        
        try:
            response = llm.chat(messages)
            
            try:
                if "```json" in response:
                    json_str = response.split("```json")[1].split("```")[0]
                else:
                    json_str = response
                data = json.loads(json_str.strip())
            except:
                data = {}
            
            return pb.JobAnalysisResponse(
                required_skills=data.get("required_skills", []),
                nice_to_have_skills=data.get("nice_to_have_skills", []),
                experience_level=data.get("experience_level", "mid"),
                salary_estimate=data.get("salary_estimate", ""),
                interview_tips=data.get("interview_tips", []),
            )
        except Exception as e:
            logger.error(f"AnalyzeJob error: {e}")
            return pb.JobAnalysisResponse()


# ================== Server Entry Point ==================

def serve(port: int = None):
    """启动 gRPC 服务器"""
    port = port or settings.grpc_port
    
    if pb_grpc is None:
        logger.error("Proto files not generated. Run generate_proto.sh first.")
        return
    
    server = grpc.server(
        futures.ThreadPoolExecutor(max_workers=settings.grpc_max_workers)
    )
    pb_grpc.add_AIEngineServicer_to_server(AIEngineServicer(), server)
    
    server.add_insecure_port(f'[::]:{port}')
    server.start()
    
    logger.info(f"AI Engine gRPC server started on port {port}")
    
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        logger.info("Shutting down...")
        server.stop(0)


if __name__ == "__main__":
    serve()
