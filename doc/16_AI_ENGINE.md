# AI Engine 服务设计 (Python)

> Python gRPC 服务，负责所有 AI Agent 的独立编排

---

## 1. 服务概述

### 1.1 定位

AI Engine 是一个独立的 Python 微服务，专门处理所有 AI 相关任务：
- 简历解析（OCR + NLP）
- 简历生成（AI 写作）
- AI 面试（对话生成）
- 模拟面试（评估打分）
- 职业差距分析
- 学习路径生成

### 1.2 设计原则

1. **Agent 独立编排** - 每个 Agent 是独立的 workflow，互不依赖
2. **gRPC 通信** - 与 biz-service 通过 gRPC 通信
3. **流式响应** - AI 生成内容支持 streaming
4. **可插拔 LLM** - 支持切换不同的 LLM 提供商

---

## 2. 项目结构

```
ai-engine/
├── pyproject.toml              # 项目配置
├── requirements.txt            # 依赖
├── proto/                      # Proto 定义
│   └── ai_engine.proto
├── src/
│   ├── __init__.py
│   ├── grpc_server/            # gRPC 服务
│   │   ├── __init__.py
│   │   ├── server.py           # 服务启动
│   │   └── handlers.py         # RPC 实现
│   ├── agents/                 # AI Agents
│   │   ├── __init__.py
│   │   ├── base.py             # Agent 基类
│   │   ├── resume/             # 简历解析
│   │   ├── resumebuilder/      # 简历构建
│   │   ├── interview/          # AI 面试
│   │   ├── mockinterview/      # 模拟面试
│   │   ├── growth/             # 职业成长
│   │   └── job/                # 职位分析
│   ├── llm/                    # LLM 客户端
│   │   ├── __init__.py
│   │   ├── client.py           # 统一客户端
│   │   ├── openai_client.py
│   │   └── ollama_client.py
│   └── utils/
│       ├── __init__.py
│       └── prompts.py          # Prompt 模板
└── tests/
```

---

## 3. gRPC 服务定义

### 3.1 Proto 文件

```protobuf
// proto/ai_engine.proto
syntax = "proto3";
package cvibe.ai;

option java_package = "com.cvibe.grpc.ai";
option java_outer_classname = "AIEngineProto";

// ==================== 服务定义 ====================

service AIEngine {
  // 简历解析
  rpc ParseResume(ParseResumeRequest) returns (ParseResumeResponse);
  
  // 简历构建
  rpc BuildResume(BuildResumeRequest) returns (stream BuildResumeChunk);
  
  // AI 面试 - 开始会话
  rpc StartInterview(StartInterviewRequest) returns (StartInterviewResponse);
  
  // AI 面试 - 发送消息 (流式响应)
  rpc SendInterviewMessage(SendMessageRequest) returns (stream MessageChunk);
  
  // 模拟面试 - 开始
  rpc StartMockInterview(StartMockRequest) returns (StartMockResponse);
  
  // 模拟面试 - 获取问题
  rpc GetNextQuestion(GetQuestionRequest) returns (QuestionResponse);
  
  // 模拟面试 - 评估答案
  rpc EvaluateAnswer(EvaluateAnswerRequest) returns (EvaluationResponse);
  
  // 模拟面试 - 结束并生成报告
  rpc FinishMockInterview(FinishMockRequest) returns (MockReportResponse);
  
  // 职业成长 - 差距分析
  rpc AnalyzeGap(GapAnalysisRequest) returns (GapAnalysisResponse);
  
  // 职业成长 - 生成学习路径
  rpc GenerateLearningPath(LearningPathRequest) returns (stream LearningPathChunk);
  
  // 职位分析
  rpc AnalyzeJob(AnalyzeJobRequest) returns (JobAnalysisResponse);
}

// ==================== 简历解析 ====================

message ParseResumeRequest {
  bytes file_content = 1;       // 文件二进制内容
  string file_name = 2;         // 文件名
  string file_type = 3;         // MIME type
}

message ParseResumeResponse {
  bool success = 1;
  ResumeData data = 2;
  string error_message = 3;
}

message ResumeData {
  string name = 1;
  string email = 2;
  string phone = 3;
  string summary = 4;
  repeated Experience experiences = 5;
  repeated Education educations = 6;
  repeated string skills = 7;
  string raw_text = 8;          // 原始文本
}

message Experience {
  string company = 1;
  string title = 2;
  string start_date = 3;
  string end_date = 4;
  string description = 5;
  bool is_current = 6;
}

message Education {
  string school = 1;
  string degree = 2;
  string field = 3;
  string start_date = 4;
  string end_date = 5;
}

// ==================== 简历构建 ====================

message BuildResumeRequest {
  string user_id = 1;
  string job_title = 2;         // 目标职位
  string job_description = 3;   // 职位描述
  ProfileData profile = 4;      // 用户资料
  string language = 5;          // zh / en
}

message ProfileData {
  string name = 1;
  string title = 2;
  string summary = 3;
  repeated Experience experiences = 4;
  repeated Education educations = 5;
  repeated string skills = 6;
}

message BuildResumeChunk {
  string section = 1;           // summary / experience / skills
  string content = 2;           // 生成的内容
  bool is_final = 3;            // 是否最后一块
}

// ==================== AI 面试 ====================

message StartInterviewRequest {
  string user_id = 1;
  string session_id = 2;
  string job_title = 3;
  string job_description = 4;
  string resume_content = 5;
  InterviewConfig config = 6;
}

message InterviewConfig {
  string language = 1;          // zh / en
  string difficulty = 2;        // easy / medium / hard
  repeated string focus_areas = 3;  // ["technical", "behavioral"]
}

message StartInterviewResponse {
  bool success = 1;
  string welcome_message = 2;
  string first_question = 3;
}

message SendMessageRequest {
  string session_id = 1;
  string user_message = 2;
  repeated ChatMessage history = 3;
}

message ChatMessage {
  string role = 1;              // user / assistant
  string content = 2;
}

message MessageChunk {
  string content = 1;
  bool is_final = 2;
  string next_question = 3;     // 下一个问题（如果有）
}

// ==================== 模拟面试 ====================

message StartMockRequest {
  string user_id = 1;
  string session_id = 2;
  string job_title = 3;
  string interview_type = 4;    // TECHNICAL / BEHAVIORAL / MIXED
  int32 question_count = 5;
  string language = 6;
}

message StartMockResponse {
  bool success = 1;
  string session_id = 2;
  int32 total_questions = 3;
}

message GetQuestionRequest {
  string session_id = 1;
  int32 question_index = 2;
}

message QuestionResponse {
  int32 question_index = 1;
  string question = 2;
  string category = 3;          // technical / behavioral / situational
  int32 time_limit_seconds = 4;
}

message EvaluateAnswerRequest {
  string session_id = 1;
  int32 question_index = 2;
  string question = 3;
  string answer_text = 4;       // 转写后的文本
  string answer_audio_url = 5;  // 音频URL（可选）
}

message EvaluationResponse {
  int32 score = 1;              // 0-100
  string feedback = 2;
  repeated string strengths = 3;
  repeated string improvements = 4;
}

message FinishMockRequest {
  string session_id = 1;
}

message MockReportResponse {
  int32 overall_score = 1;
  string overall_feedback = 2;
  repeated QuestionResult results = 3;
  repeated string recommendations = 4;
}

message QuestionResult {
  int32 index = 1;
  string question = 2;
  string answer = 3;
  int32 score = 4;
  string feedback = 5;
}

// ==================== 职业成长 ====================

message GapAnalysisRequest {
  string user_id = 1;
  string goal_title = 2;        // 目标职位
  string target_date = 3;       // 目标日期
  ProfileData current_profile = 4;
}

message GapAnalysisResponse {
  repeated GapItem gaps = 1;
  repeated string recommendations = 2;
  int32 readiness_score = 3;    // 0-100 准备度
}

message GapItem {
  string skill = 1;
  string current_level = 2;     // NONE / BASIC / INTERMEDIATE / ADVANCED
  string required_level = 3;
  int32 priority = 4;           // 1-5
}

message LearningPathRequest {
  string user_id = 1;
  string goal_id = 2;
  repeated GapItem gaps = 3;
  string preferred_style = 4;   // VIDEO / READING / PRACTICE
}

message LearningPathChunk {
  string phase = 1;             // 阶段名
  string content = 2;           // 内容
  bool is_final = 3;
}

// ==================== 职位分析 ====================

message AnalyzeJobRequest {
  string job_id = 1;
  string job_title = 2;
  string job_description = 3;
  string company = 4;
}

message JobAnalysisResponse {
  repeated string required_skills = 1;
  repeated string nice_to_have_skills = 2;
  string experience_level = 3;
  string salary_estimate = 4;
  repeated string interview_tips = 5;
}
```

---

## 4. Agent 设计

### 4.1 Agent 基类

```python
# src/agents/base.py
from abc import ABC, abstractmethod
from typing import Any, AsyncIterator
from dataclasses import dataclass

@dataclass
class AgentContext:
    """Agent 执行上下文"""
    user_id: str
    session_id: str | None = None
    config: dict | None = None

class BaseAgent(ABC):
    """Agent 基类"""
    
    def __init__(self, llm_client):
        self.llm = llm_client
    
    @abstractmethod
    async def run(self, context: AgentContext, **kwargs) -> Any:
        """执行 Agent"""
        pass
    
    @abstractmethod
    async def stream(self, context: AgentContext, **kwargs) -> AsyncIterator[str]:
        """流式执行 Agent"""
        pass
```

### 4.2 简历解析 Agent

```python
# src/agents/resume/workflow.py
from langgraph.graph import StateGraph, END
from typing import TypedDict
import fitz  # PyMuPDF
import docx

class ResumeState(TypedDict):
    file_content: bytes
    file_type: str
    raw_text: str
    parsed_data: dict
    error: str | None

class ResumeParserAgent(BaseAgent):
    """简历解析 Agent - 使用 LangGraph 编排"""
    
    def __init__(self, llm_client):
        super().__init__(llm_client)
        self.workflow = self._build_workflow()
    
    def _build_workflow(self) -> StateGraph:
        """构建解析 workflow"""
        workflow = StateGraph(ResumeState)
        
        # 添加节点
        workflow.add_node("extract_text", self._extract_text)
        workflow.add_node("parse_with_llm", self._parse_with_llm)
        workflow.add_node("validate", self._validate)
        
        # 定义边
        workflow.set_entry_point("extract_text")
        workflow.add_edge("extract_text", "parse_with_llm")
        workflow.add_edge("parse_with_llm", "validate")
        workflow.add_edge("validate", END)
        
        return workflow.compile()
    
    async def _extract_text(self, state: ResumeState) -> ResumeState:
        """提取文本"""
        content = state["file_content"]
        file_type = state["file_type"]
        
        if "pdf" in file_type:
            text = self._extract_pdf(content)
        elif "word" in file_type or "docx" in file_type:
            text = self._extract_docx(content)
        else:
            text = content.decode("utf-8", errors="ignore")
        
        return {**state, "raw_text": text}
    
    async def _parse_with_llm(self, state: ResumeState) -> ResumeState:
        """使用 LLM 解析结构化数据"""
        prompt = f"""
        请解析以下简历文本，提取结构化信息。
        返回 JSON 格式，包含：
        - name: 姓名
        - email: 邮箱
        - phone: 电话
        - summary: 个人简介
        - experiences: 工作经历数组
        - educations: 教育背景数组
        - skills: 技能数组
        
        简历文本：
        {state["raw_text"]}
        """
        
        response = await self.llm.chat(prompt, response_format="json")
        return {**state, "parsed_data": response}
    
    async def _validate(self, state: ResumeState) -> ResumeState:
        """验证解析结果"""
        data = state["parsed_data"]
        # 验证必填字段
        if not data.get("name"):
            state["error"] = "无法解析姓名"
        return state
    
    async def run(self, context: AgentContext, **kwargs) -> dict:
        """执行解析"""
        initial_state = ResumeState(
            file_content=kwargs["file_content"],
            file_type=kwargs["file_type"],
            raw_text="",
            parsed_data={},
            error=None
        )
        
        result = await self.workflow.ainvoke(initial_state)
        return result["parsed_data"]
```

### 4.3 AI 面试 Agent

```python
# src/agents/interview/workflow.py
from langgraph.graph import StateGraph, END
from typing import TypedDict, List

class InterviewState(TypedDict):
    session_id: str
    job_title: str
    job_description: str
    resume_content: str
    config: dict
    history: List[dict]
    current_question: str
    response: str

class InterviewAgent(BaseAgent):
    """AI 面试 Agent"""
    
    def __init__(self, llm_client):
        super().__init__(llm_client)
        self.system_prompt = """
        你是一位专业的面试官，正在面试候选人。
        根据职位要求和候选人简历，进行深入的面试对话。
        
        面试指南：
        1. 提问应该循序渐进，从简单到复杂
        2. 根据候选人回答追问细节
        3. 覆盖技术能力、项目经验、软技能
        4. 保持专业友好的态度
        """
    
    async def start_session(
        self,
        session_id: str,
        job_title: str,
        job_description: str,
        resume_content: str,
        config: dict
    ) -> dict:
        """开始面试会话"""
        prompt = f"""
        职位：{job_title}
        职位描述：{job_description}
        
        候选人简历：
        {resume_content}
        
        请生成一段欢迎语和第一个面试问题。
        语言：{config.get('language', 'zh')}
        难度：{config.get('difficulty', 'medium')}
        """
        
        response = await self.llm.chat(
            prompt,
            system=self.system_prompt,
            response_format="json"
        )
        
        return {
            "welcome_message": response.get("welcome"),
            "first_question": response.get("question")
        }
    
    async def stream(
        self,
        context: AgentContext,
        user_message: str,
        history: List[dict]
    ) -> AsyncIterator[str]:
        """流式响应用户消息"""
        messages = [{"role": "system", "content": self.system_prompt}]
        messages.extend(history)
        messages.append({"role": "user", "content": user_message})
        
        async for chunk in self.llm.stream_chat(messages):
            yield chunk
```

### 4.4 职业成长 Agent

```python
# src/agents/growth/workflow.py
from langgraph.graph import StateGraph, END
from typing import TypedDict, List

class GrowthState(TypedDict):
    goal_title: str
    target_date: str
    current_profile: dict
    gaps: List[dict]
    learning_path: List[dict]
    readiness_score: int

class GrowthAgent(BaseAgent):
    """职业成长 Agent - 差距分析 + 学习路径"""
    
    def __init__(self, llm_client):
        super().__init__(llm_client)
        self.workflow = self._build_workflow()
    
    def _build_workflow(self) -> StateGraph:
        workflow = StateGraph(GrowthState)
        
        workflow.add_node("analyze_requirements", self._analyze_requirements)
        workflow.add_node("identify_gaps", self._identify_gaps)
        workflow.add_node("calculate_readiness", self._calculate_readiness)
        workflow.add_node("generate_path", self._generate_path)
        
        workflow.set_entry_point("analyze_requirements")
        workflow.add_edge("analyze_requirements", "identify_gaps")
        workflow.add_edge("identify_gaps", "calculate_readiness")
        workflow.add_edge("calculate_readiness", "generate_path")
        workflow.add_edge("generate_path", END)
        
        return workflow.compile()
    
    async def _analyze_requirements(self, state: GrowthState) -> GrowthState:
        """分析目标职位要求"""
        prompt = f"""
        分析职位 "{state['goal_title']}" 需要的技能和经验。
        返回 JSON 格式的要求列表。
        """
        requirements = await self.llm.chat(prompt, response_format="json")
        return {**state, "requirements": requirements}
    
    async def _identify_gaps(self, state: GrowthState) -> GrowthState:
        """识别技能差距"""
        prompt = f"""
        对比候选人当前能力和目标要求，识别差距。
        
        当前能力：
        {state['current_profile']}
        
        目标要求：
        {state.get('requirements', [])}
        
        返回差距列表，包含 skill, current_level, required_level, priority
        """
        gaps = await self.llm.chat(prompt, response_format="json")
        return {**state, "gaps": gaps}
    
    async def _calculate_readiness(self, state: GrowthState) -> GrowthState:
        """计算准备度分数"""
        gaps = state.get("gaps", [])
        if not gaps:
            return {**state, "readiness_score": 100}
        
        # 简单计算：根据差距数量和优先级
        total_gap = sum(g.get("priority", 3) for g in gaps)
        max_gap = len(gaps) * 5
        score = max(0, 100 - int(total_gap / max_gap * 100))
        
        return {**state, "readiness_score": score}
    
    async def generate_learning_path(
        self,
        context: AgentContext,
        gaps: List[dict],
        preferred_style: str
    ) -> AsyncIterator[dict]:
        """生成学习路径（流式）"""
        prompt = f"""
        根据以下技能差距，生成详细的学习路径。
        
        差距：
        {gaps}
        
        学习偏好：{preferred_style}
        
        请分阶段输出，每个阶段包含：
        - phase: 阶段名
        - duration: 建议时长
        - resources: 学习资源
        - milestones: 里程碑
        """
        
        async for chunk in self.llm.stream_chat(prompt):
            yield {"phase": "learning", "content": chunk, "is_final": False}
        
        yield {"phase": "complete", "content": "", "is_final": True}
```

---

## 5. LLM 客户端

### 5.1 统一接口

```python
# src/llm/client.py
from abc import ABC, abstractmethod
from typing import AsyncIterator, List, Optional
import os

class LLMClient(ABC):
    """LLM 客户端抽象基类"""
    
    @abstractmethod
    async def chat(
        self,
        prompt: str,
        system: Optional[str] = None,
        response_format: Optional[str] = None
    ) -> str | dict:
        pass
    
    @abstractmethod
    async def stream_chat(
        self,
        messages: List[dict]
    ) -> AsyncIterator[str]:
        pass

def create_llm_client(provider: str = None) -> LLMClient:
    """工厂方法创建 LLM 客户端"""
    provider = provider or os.getenv("LLM_PROVIDER", "openai")
    
    if provider == "openai":
        from .openai_client import OpenAIClient
        return OpenAIClient()
    elif provider == "ollama":
        from .ollama_client import OllamaClient
        return OllamaClient()
    else:
        raise ValueError(f"Unknown LLM provider: {provider}")
```

### 5.2 OpenAI 实现

```python
# src/llm/openai_client.py
from openai import AsyncOpenAI
from typing import AsyncIterator, List, Optional
import json
import os

from .client import LLMClient

class OpenAIClient(LLMClient):
    """OpenAI 客户端"""
    
    def __init__(self):
        self.client = AsyncOpenAI(
            api_key=os.getenv("OPENAI_API_KEY"),
            base_url=os.getenv("OPENAI_BASE_URL")  # 支持自定义端点
        )
        self.model = os.getenv("OPENAI_MODEL", "gpt-4o")
    
    async def chat(
        self,
        prompt: str,
        system: Optional[str] = None,
        response_format: Optional[str] = None
    ) -> str | dict:
        messages = []
        if system:
            messages.append({"role": "system", "content": system})
        messages.append({"role": "user", "content": prompt})
        
        kwargs = {"model": self.model, "messages": messages}
        if response_format == "json":
            kwargs["response_format"] = {"type": "json_object"}
        
        response = await self.client.chat.completions.create(**kwargs)
        content = response.choices[0].message.content
        
        if response_format == "json":
            return json.loads(content)
        return content
    
    async def stream_chat(
        self,
        messages: List[dict]
    ) -> AsyncIterator[str]:
        stream = await self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            stream=True
        )
        
        async for chunk in stream:
            if chunk.choices[0].delta.content:
                yield chunk.choices[0].delta.content
```

---

## 6. gRPC Server

### 6.1 服务启动

```python
# src/grpc_server/server.py
import asyncio
import grpc
from concurrent import futures
import os

from .handlers import AIEngineServicer
from ..llm.client import create_llm_client

# 导入生成的 proto
import ai_engine_pb2_grpc

async def serve():
    """启动 gRPC 服务"""
    server = grpc.aio.server(
        futures.ThreadPoolExecutor(max_workers=10),
        options=[
            ('grpc.max_receive_message_length', 50 * 1024 * 1024),  # 50MB
            ('grpc.max_send_message_length', 50 * 1024 * 1024),
        ]
    )
    
    # 创建 LLM 客户端
    llm_client = create_llm_client()
    
    # 注册服务
    ai_engine_pb2_grpc.add_AIEngineServicer_to_server(
        AIEngineServicer(llm_client),
        server
    )
    
    port = os.getenv("GRPC_PORT", "50051")
    server.add_insecure_port(f"[::]:{port}")
    
    print(f"AI Engine gRPC server starting on port {port}")
    await server.start()
    await server.wait_for_termination()

if __name__ == "__main__":
    asyncio.run(serve())
```

### 6.2 RPC 实现

```python
# src/grpc_server/handlers.py
import grpc
from typing import AsyncIterator

import ai_engine_pb2
import ai_engine_pb2_grpc

from ..agents.resume.workflow import ResumeParserAgent
from ..agents.interview.workflow import InterviewAgent
from ..agents.growth.workflow import GrowthAgent
from ..agents.base import AgentContext

class AIEngineServicer(ai_engine_pb2_grpc.AIEngineServicer):
    """gRPC 服务实现"""
    
    def __init__(self, llm_client):
        self.llm = llm_client
        self.resume_agent = ResumeParserAgent(llm_client)
        self.interview_agent = InterviewAgent(llm_client)
        self.growth_agent = GrowthAgent(llm_client)
        
        # 会话存储（生产环境应使用 Redis）
        self.sessions = {}
    
    async def ParseResume(
        self,
        request: ai_engine_pb2.ParseResumeRequest,
        context: grpc.aio.ServicerContext
    ) -> ai_engine_pb2.ParseResumeResponse:
        """简历解析"""
        try:
            agent_context = AgentContext(user_id="", session_id=None)
            result = await self.resume_agent.run(
                agent_context,
                file_content=request.file_content,
                file_type=request.file_type
            )
            
            return ai_engine_pb2.ParseResumeResponse(
                success=True,
                data=ai_engine_pb2.ResumeData(
                    name=result.get("name", ""),
                    email=result.get("email", ""),
                    phone=result.get("phone", ""),
                    summary=result.get("summary", ""),
                    skills=result.get("skills", [])
                )
            )
        except Exception as e:
            return ai_engine_pb2.ParseResumeResponse(
                success=False,
                error_message=str(e)
            )
    
    async def StartInterview(
        self,
        request: ai_engine_pb2.StartInterviewRequest,
        context: grpc.aio.ServicerContext
    ) -> ai_engine_pb2.StartInterviewResponse:
        """开始面试会话"""
        result = await self.interview_agent.start_session(
            session_id=request.session_id,
            job_title=request.job_title,
            job_description=request.job_description,
            resume_content=request.resume_content,
            config={
                "language": request.config.language,
                "difficulty": request.config.difficulty,
                "focus_areas": list(request.config.focus_areas)
            }
        )
        
        # 保存会话
        self.sessions[request.session_id] = {
            "job_title": request.job_title,
            "history": []
        }
        
        return ai_engine_pb2.StartInterviewResponse(
            success=True,
            welcome_message=result["welcome_message"],
            first_question=result["first_question"]
        )
    
    async def SendInterviewMessage(
        self,
        request: ai_engine_pb2.SendMessageRequest,
        context: grpc.aio.ServicerContext
    ) -> AsyncIterator[ai_engine_pb2.MessageChunk]:
        """发送面试消息（流式响应）"""
        session = self.sessions.get(request.session_id, {})
        history = [{"role": m.role, "content": m.content} for m in request.history]
        
        agent_context = AgentContext(
            user_id="",
            session_id=request.session_id
        )
        
        async for chunk in self.interview_agent.stream(
            agent_context,
            user_message=request.user_message,
            history=history
        ):
            yield ai_engine_pb2.MessageChunk(
                content=chunk,
                is_final=False
            )
        
        yield ai_engine_pb2.MessageChunk(
            content="",
            is_final=True
        )
    
    async def AnalyzeGap(
        self,
        request: ai_engine_pb2.GapAnalysisRequest,
        context: grpc.aio.ServicerContext
    ) -> ai_engine_pb2.GapAnalysisResponse:
        """差距分析"""
        agent_context = AgentContext(user_id=request.user_id)
        
        result = await self.growth_agent.run(
            agent_context,
            goal_title=request.goal_title,
            target_date=request.target_date,
            current_profile=self._profile_to_dict(request.current_profile)
        )
        
        return ai_engine_pb2.GapAnalysisResponse(
            gaps=[
                ai_engine_pb2.GapItem(
                    skill=g["skill"],
                    current_level=g["current_level"],
                    required_level=g["required_level"],
                    priority=g["priority"]
                )
                for g in result.get("gaps", [])
            ],
            recommendations=result.get("recommendations", []),
            readiness_score=result.get("readiness_score", 0)
        )
    
    def _profile_to_dict(self, profile) -> dict:
        return {
            "name": profile.name,
            "title": profile.title,
            "summary": profile.summary,
            "skills": list(profile.skills)
        }
```

---

## 7. 配置

### 7.1 环境变量

```bash
# .env
# gRPC 配置
GRPC_PORT=50051

# LLM 配置
LLM_PROVIDER=openai
OPENAI_API_KEY=sk-xxx
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=gpt-4o

# 备选：Ollama
# LLM_PROVIDER=ollama
# OLLAMA_BASE_URL=http://localhost:11434
# OLLAMA_MODEL=llama3

# 日志
LOG_LEVEL=INFO
```

### 7.2 依赖

```txt
# requirements.txt
grpcio>=1.60.0
grpcio-tools>=1.60.0
protobuf>=4.25.0
openai>=1.10.0
langgraph>=0.0.20
langchain>=0.1.0
PyMuPDF>=1.23.0
python-docx>=1.1.0
pydantic>=2.5.0
python-dotenv>=1.0.0
```

---

## 8. 启动命令

```bash
# 生成 Proto
python -m grpc_tools.protoc \
  -I./proto \
  --python_out=./src \
  --grpc_python_out=./src \
  ./proto/ai_engine.proto

# 启动服务
cd ai-engine
python -m src.grpc_server.server
```
