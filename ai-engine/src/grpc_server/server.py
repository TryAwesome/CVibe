"""
gRPC Server - AI Engine 服务入口
================================================================================

提供 gRPC 接口供 Java biz-service 调用
"""

import asyncio
from concurrent import futures

# import grpc
# from generated import ai_pb2, ai_pb2_grpc  # 由 proto 生成

from ..llm import (
    create_llm_client, 
    create_vision_client,
    get_default_llm_config,
    get_default_vision_config,
)
from ..agents import (
    InterviewAgentWorkflow,
    ResumeBuilderAgentWorkflow,
    MockInterviewAgentWorkflow,
    JobRecommenderAgentWorkflow,
    GrowthAdvisorAgentWorkflow,
    ResumeParser,
)


class AIEngineService:
    """
    AI Engine gRPC 服务
    
    提供以下服务：
    - Interview: 背景采集面试
    - ResumeBuilder: 简历构建
    - MockInterview: 模拟面试
    - JobRecommender: 职位推荐
    - GrowthAdvisor: 成长规划
    - ResumeParser: 简历解析
    """

    def __init__(self):
        # 默认 LLM 客户端（用户可通过 API 传入自己的配置）
        self._default_llm = None
        self._default_vision = None
        self._agents = {}
        
        # 初始化默认客户端（从环境变量）
        self._init_default_clients()
    
    def _init_default_clients(self):
        """从环境变量初始化默认客户端"""
        llm_config = get_default_llm_config()
        if llm_config:
            self._default_llm = create_llm_client(
                api_key=llm_config.api_key,
                model=llm_config.model,
                provider=llm_config.provider,
                base_url=llm_config.base_url,
            )
        
        vision_config = get_default_vision_config()
        if vision_config:
            self._default_vision = create_vision_client(
                api_key=vision_config.api_key,
                model=vision_config.model,
                provider=vision_config.provider,
                base_url=vision_config.base_url,
            )

    def get_llm_client(self, user_config: dict = None):
        """获取 LLM 客户端（支持用户自定义）"""
        if user_config and user_config.get("api_key"):
            return create_llm_client(
                api_key=user_config.get("api_key"),
                model=user_config.get("model", "gpt-4o"),
                provider=user_config.get("provider", "openai"),
                base_url=user_config.get("base_url"),
            )
        return self._default_llm
    
    def get_vision_client(self, user_config: dict = None):
        """获取视觉模型客户端（支持用户自定义）"""
        if user_config and user_config.get("vision_api_key"):
            return create_vision_client(
                api_key=user_config.get("vision_api_key"),
                model=user_config.get("vision_model", "gpt-4o"),
                provider=user_config.get("vision_provider", "openai"),
                base_url=user_config.get("vision_base_url"),
            )
        return self._default_vision

    # ================== Interview ==================
    
    def start_interview(self, session_id: str, user_id: str, llm_config: dict = None):
        """开始面试"""
        llm = self.get_llm_client(llm_config)
        agent = InterviewAgentWorkflow(llm)
        context, question = agent.start_session(session_id, user_id)
        self._agents[session_id] = {"agent": agent, "context": context}
        return {"session_id": session_id, "question": question}

    def submit_interview_answer(self, session_id: str, answer: str):
        """提交面试回答"""
        session = self._agents.get(session_id)
        if not session:
            return {"error": "Session not found"}
        
        agent = session["agent"]
        context = session["context"]
        context, question, data = agent.process_answer(context, answer)
        session["context"] = context
        
        return {"question": question, "extracted_data": data}

    def end_interview(self, session_id: str):
        """结束面试"""
        session = self._agents.get(session_id)
        if not session:
            return {"error": "Session not found"}
        
        result = session["agent"].end_session(session["context"])
        del self._agents[session_id]
        return result

    # ================== Resume Builder ==================
    
    def build_resume(
        self, 
        user_profile: dict, 
        target_hc: dict, 
        template_id: str,
        llm_config: dict = None,
    ):
        """构建简历"""
        llm = self.get_llm_client(llm_config)
        agent = ResumeBuilderAgentWorkflow(llm)
        
        # 解析 HC
        criteria = agent.parse_hiring_criteria(target_hc.get("raw_jd", ""))
        
        # 生成简历
        content = agent.generate_resume_content(user_profile, criteria, template_id)
        
        return {
            "latex_content": content.latex_content,
            "sections": content.sections,
            "highlighted_skills": content.highlighted_skills,
        }

    # ================== Mock Interview ==================
    
    def start_mock_interview(
        self,
        session_id: str,
        user_id: str,
        interview_type: str,
        user_resume: dict = None,
        llm_config: dict = None,
    ):
        """开始模拟面试"""
        from ..agents.mockinterview import MockInterviewType
        
        llm = self.get_llm_client(llm_config)
        agent = MockInterviewAgentWorkflow(llm)
        
        type_enum = MockInterviewType(interview_type)
        context, message = agent.start_session(session_id, user_id, type_enum, user_resume)
        self._agents[f"mock_{session_id}"] = {"agent": agent, "context": context}
        
        return {"session_id": session_id, "message": message}

    def submit_mock_answer(self, session_id: str, answer: str):
        """提交模拟面试回答"""
        session = self._agents.get(f"mock_{session_id}")
        if not session:
            return {"error": "Session not found"}
        
        agent = session["agent"]
        context = session["context"]
        context, message, evaluation = agent.process_answer(context, answer)
        session["context"] = context
        
        return {"message": message, "evaluation": evaluation}

    def end_mock_interview(self, session_id: str):
        """结束模拟面试"""
        session = self._agents.get(f"mock_{session_id}")
        if not session:
            return {"error": "Session not found"}
        
        result = session["agent"].end_session(session["context"])
        del self._agents[f"mock_{session_id}"]
        return result

    # ================== Job Recommender ==================
    
    def recommend_jobs(
        self,
        user_profile: dict,
        jobs: list,
        top_k: int = 10,
        llm_config: dict = None,
    ):
        """推荐职位"""
        from ..agents.job import UserProfile, JobPosting
        
        llm = self.get_llm_client(llm_config)
        agent = JobRecommenderAgentWorkflow(llm)
        
        # 转换数据格式
        profile = UserProfile(
            user_id=user_profile.get("user_id", ""),
            skills=user_profile.get("skills", []),
            experience_years=user_profile.get("experience_years", 0),
            target_roles=user_profile.get("target_roles", []),
        )
        
        job_postings = [
            JobPosting(
                id=j.get("id", ""),
                title=j.get("title", ""),
                company=j.get("company", ""),
                location=j.get("location", ""),
                required_skills=j.get("required_skills", []),
                source_url=j.get("source_url", ""),
            )
            for j in jobs
        ]
        
        matches = agent.match_jobs(profile, job_postings, top_k)
        
        return {
            "recommendations": [
                {
                    "job_id": m.job.id,
                    "title": m.job.title,
                    "company": m.job.company,
                    "match_score": m.match_score,
                    "matched_skills": m.matched_skills,
                    "missing_skills": m.missing_skills,
                    "recommendation_text": m.recommendation_text,
                    "source_url": m.job.source_url,
                }
                for m in matches
            ]
        }

    # ================== Growth Advisor ==================
    
    def analyze_growth_gap(
        self,
        user_profile: dict,
        target_hc: dict,
        llm_config: dict = None,
    ):
        """分析成长差距"""
        llm = self.get_llm_client(llm_config)
        agent = GrowthAdvisorAgentWorkflow(llm)
        
        gaps = agent.analyze_gap(user_profile, target_hc)
        
        return {
            "gaps": [
                {
                    "skill": g.skill_name,
                    "current_level": g.current_level,
                    "required_level": g.required_level,
                    "gap": g.gap,
                    "priority": g.priority,
                    "resources": g.learning_resources,
                }
                for g in gaps
            ]
        }

    def generate_learning_path(
        self,
        user_profile: dict,
        target_hc: dict,
        hours_per_week: int = 10,
        llm_config: dict = None,
    ):
        """生成学习路径"""
        llm = self.get_llm_client(llm_config)
        agent = GrowthAdvisorAgentWorkflow(llm)
        
        path = agent.generate_learning_path(user_profile, target_hc, hours_per_week)
        
        return {
            "target_role": path.target_role,
            "target_company": path.target_company,
            "total_duration": path.total_duration,
            "milestones": [
                {
                    "id": m.id,
                    "title": m.title,
                    "description": m.description,
                    "skills": m.skills_covered,
                    "time": m.estimated_time,
                    "resources": m.resources,
                    "deliverables": m.deliverables,
                }
                for m in path.milestones
            ],
        }

    # ================== Resume Parser ==================
    
    def parse_resume_pdf(self, pdf_path: str, llm_config: dict = None):
        """解析 PDF 简历"""
        llm = self.get_llm_client(llm_config)
        parser = ResumeParser(llm)
        
        result = parser.parse_pdf(pdf_path)
        
        return {
            "name": result.name,
            "email": result.email,
            "phone": result.phone,
            "education": result.education,
            "work_experience": result.work_experience,
            "projects": result.projects,
            "skills": result.skills,
            "achievements": result.achievements,
        }


def serve(port: int = 50051):
    """启动 gRPC 服务"""
    # TODO: 实际启动 gRPC 服务
    # server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    # ai_pb2_grpc.add_AIEngineServicer_to_server(AIEngineService(), server)
    # server.add_insecure_port(f'[::]:{port}')
    # server.start()
    # server.wait_for_termination()
    print(f"AI Engine gRPC server would start on port {port}")


if __name__ == "__main__":
    serve()
