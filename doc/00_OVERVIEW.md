# CVibe 后端重写总览

## 文档结构

本文档集旨在指导后端重写，确保与现有前端完美匹配。

| 文档 | 内容 |
|------|------|
| [01_API_CONTRACTS.md](./01_API_CONTRACTS.md) | 前端期望的完整 API 契约 |
| [02_DATA_MODELS.md](./02_DATA_MODELS.md) | 数据库实体与 DTO 设计 |
| [03_AUTH_MODULE.md](./03_AUTH_MODULE.md) | 认证模块详细设计 |
| [04_PROFILE_MODULE.md](./04_PROFILE_MODULE.md) | 用户资料模块详细设计 |
| [05_RESUME_MODULE.md](./05_RESUME_MODULE.md) | 简历管理模块详细设计 |
| [06_RESUME_BUILDER_MODULE.md](./06_RESUME_BUILDER_MODULE.md) | 简历构建器模块详细设计 |
| [07_INTERVIEW_MODULE.md](./07_INTERVIEW_MODULE.md) | AI 面试模块详细设计 |
| [08_MOCK_INTERVIEW_MODULE.md](./08_MOCK_INTERVIEW_MODULE.md) | 模拟面试模块详细设计 |
| [09_GROWTH_MODULE.md](./09_GROWTH_MODULE.md) | 职业成长模块详细设计 |
| [10_JOB_MODULE.md](./10_JOB_MODULE.md) | 职位匹配模块详细设计 |
| [11_COMMUNITY_MODULE.md](./11_COMMUNITY_MODULE.md) | 社区模块详细设计 |
| [12_NOTIFICATION_MODULE.md](./12_NOTIFICATION_MODULE.md) | 通知模块详细设计 |
| [13_SETTINGS_MODULE.md](./13_SETTINGS_MODULE.md) | 设置模块详细设计 |
| [14_ERROR_HANDLING.md](./14_ERROR_HANDLING.md) | 统一错误处理规范 |
| [15_CODING_STANDARDS.md](./15_CODING_STANDARDS.md) | 代码规范与最佳实践 |
| [16_AI_ENGINE.md](./16_AI_ENGINE.md) | Python AI Agent 服务设计 |
| [17_SEARCH_SERVICE.md](./17_SEARCH_SERVICE.md) | Go 搜索/并发服务设计 |
| [18_SERVICE_INTEGRATION.md](./18_SERVICE_INTEGRATION.md) | 服务间调用与编排 |

---

## 整体架构

### 多服务架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Frontend (Next.js)                          │
│                    http://localhost:3000                            │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTP REST API
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     biz-service (Java/Spring Boot)                  │
│                       http://localhost:8080                         │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                      业务核心 (主框架)                         │  │
│  │  • 用户认证 (Auth)      • 用户资料 (Profile)                  │  │
│  │  • 简历管理 (Resume)    • 社区功能 (Community)                │  │
│  │  • 通知系统 (Notify)    • 设置管理 (Settings)                 │  │
│  │  • 数据持久化           • 业务逻辑编排                        │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                    │                          │                     │
│            gRPC 调用                    gRPC 调用                   │
│                    ▼                          ▼                     │
│  ┌─────────────────────────┐  ┌─────────────────────────────────┐  │
│  │   AI Engine (Python)    │  │   Search Service (Go)           │  │
│  │   localhost:50051       │  │   localhost:50052               │  │
│  │                         │  │                                 │  │
│  │  • Resume Agent         │  │  • 职位搜索 (高并发)             │  │
│  │  • Interview Agent      │  │  • 简历匹配 (并行计算)           │  │
│  │  • Growth Agent         │  │  • 数据爬虫                     │  │
│  │  • Job Agent            │  │  • 向量搜索                     │  │
│  │  • Mock Interview Agent │  │  • 分析聚合                     │  │
│  │  • Resume Builder Agent │  │                                 │  │
│  └─────────────────────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                    │                          │
                    ▼                          ▼
           ┌───────────────┐          ┌───────────────┐
           │  LLM Service  │          │   外部 API     │
           │  (OpenAI等)   │          │  (Boss/Lagou)  │
           └───────────────┘          └───────────────┘
```

### 职责划分

| 服务 | 语言 | 职责 | 端口 |
|------|------|------|------|
| **biz-service** | Java/Spring Boot | 主框架、业务逻辑、数据持久化、API 网关 | 8080 (HTTP) |
| **ai-engine** | Python | AI Agent 服务、LLM 调用、智能编排 | 50051 (gRPC) |
| **search-service** | Go | 高并发搜索、并行计算、数据爬虫 | 50052 (gRPC) |

### 调用关系

```
Frontend ──HTTP──▶ biz-service ──gRPC──▶ ai-engine
                       │
                       └────gRPC──▶ search-service
```

**关键点：**
- 前端 **只与 biz-service 通信**（HTTP REST）
- biz-service 作为 **API 网关** 和 **业务编排层**
- biz-service 通过 **gRPC 调用** ai-engine 和 search-service
- ai-engine 的每个 Agent **独立编排**，互不依赖
- search-service 负责所有 **高并发** 和 **计算密集型** 任务

---

## 核心原则

### 1. 前端优先 (Frontend-First)
前端已经完成，后端必须严格匹配前端期望的：
- API 路径（Path）
- 请求参数（Query/Body）
- 响应格式（Response）
- 字段命名（Field Names）

### 2. 统一响应格式
所有 API 必须返回统一格式：

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": {
    "timestamp": "2026-01-17T10:00:00Z"
  }
}
```

错误响应：
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": 20001,
    "message": "Invalid credentials"
  },
  "meta": {
    "timestamp": "2026-01-17T10:00:00Z"
  }
}
```

### 3. API 路径前缀规则
| 模块 | 路径前缀 |
|------|---------|
| Auth | `/api/auth` |
| Profile | `/api/profile` |
| Resumes | `/api/resumes` |
| Resume Builder | `/api/v1/resume-builder` |
| Interview | `/api/v1/interviews` |
| Mock Interview | `/api/v1/mock-interview` |
| Growth | `/api/v1/growth` |
| Jobs | `/api/v1/jobs` |
| Community | `/api/v1/community` |
| Notifications | `/api/v1/notifications` |
| Settings | `/api/settings` |
| Health | `/api/health` |

### 4. 认证机制
- JWT Bearer Token
- Token 存储在 `localStorage`（accessToken）
- Header: `Authorization: Bearer {token}`

---

## 重写优先级

### P0 - 核心功能（必须先完成）
1. Auth 模块 - 登录注册
2. Profile 模块 - 用户资料
3. Settings 模块 - AI 配置

### P1 - 主要功能
4. Resume 模块 - 简历上传
5. Interview 模块 - AI 面试
6. Jobs 模块 - 职位匹配

### P2 - 增强功能
7. Resume Builder 模块
8. Mock Interview 模块
9. Growth 模块

### P3 - 社交功能
10. Community 模块
11. Notification 模块

---

## 技术栈要求

### biz-service (Java)
- **框架**: Spring Boot 3.2+
- **数据库**: H2 (开发) / PostgreSQL (生产)
- **ORM**: Spring Data JPA
- **验证**: Jakarta Validation
- **安全**: Spring Security + JWT
- **gRPC 客户端**: grpc-java
- **文档**: SpringDoc OpenAPI
- **工具**: Lombok, MapStruct

### ai-engine (Python)
- **框架**: gRPC Server (grpcio)
- **AI 编排**: LangGraph / LangChain
- **LLM 客户端**: OpenAI SDK
- **异步**: asyncio
- **Agent 框架**: 每个 Agent 独立 workflow

### search-service (Go)
- **框架**: gRPC Server (google.golang.org/grpc)
- **并发**: goroutine + channel
- **搜索**: 向量数据库 / Elasticsearch
- **爬虫**: colly / goquery

---

## 项目结构

### 整体目录

```
CVibe/
├── frontend/                   # Next.js 前端 (已完成，不修改)
├── biz-service/                # Java 业务主框架
├── ai-engine/                  # Python AI Agent 服务
├── search-service/             # Go 搜索/并发服务
├── infra/                      # 基础设施配置
└── doc/                        # 文档
```

### biz-service 结构

```
biz-service/
├── src/main/java/com/cvibe/
│   ├── CVibeApplication.java
│   ├── biz/                    # 业务模块
│   │   ├── user/               # 用户 & 认证
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   └── dto/
│   │   ├── profile/            # 用户资料
│   │   ├── resume/             # 简历管理
│   │   ├── interview/          # AI 面试 (调用 ai-engine)
│   │   ├── mock/               # 模拟面试 (调用 ai-engine)
│   │   ├── growth/             # 职业成长 (调用 ai-engine)
│   │   ├── job/                # 职位匹配 (调用 search-service)
│   │   ├── community/          # 社区
│   │   ├── notification/       # 通知
│   │   └── settings/           # 设置
│   ├── common/                 # 公共模块
│   │   ├── config/             # 配置类
│   │   ├── exception/          # 异常处理
│   │   ├── response/           # 响应封装
│   │   ├── security/           # 安全相关
│   │   └── storage/            # 文件存储服务
│   └── grpc/                   # gRPC 客户端
│       ├── ai/                 # ai-engine 客户端
│       └── search/             # search-service 客户端
└── src/main/resources/
    ├── application.yml
    └── proto/                  # gRPC Proto 定义 (共享)
```

### ai-engine 结构

```
ai-engine/
├── pyproject.toml
├── requirements.txt
├── src/
│   ├── __init__.py
│   ├── grpc_server/            # gRPC 服务端
│   │   ├── __init__.py
│   │   ├── server.py           # 主服务启动
│   │   └── handlers.py         # RPC 方法实现
│   ├── agents/                 # AI Agents (每个独立编排)
│   │   ├── __init__.py
│   │   ├── resume/             # 简历解析 Agent
│   │   │   ├── __init__.py
│   │   │   └── workflow.py     # LangGraph workflow
│   │   ├── resumebuilder/      # 简历构建 Agent
│   │   │   ├── __init__.py
│   │   │   └── workflow.py
│   │   ├── interview/          # AI 面试 Agent
│   │   │   ├── __init__.py
│   │   │   └── workflow.py
│   │   ├── mockinterview/      # 模拟面试 Agent
│   │   │   ├── __init__.py
│   │   │   └── workflow.py
│   │   ├── growth/             # 职业成长 Agent
│   │   │   ├── __init__.py
│   │   │   └── workflow.py
│   │   └── job/                # 职位分析 Agent
│   │       ├── __init__.py
│   │       └── workflow.py
│   ├── llm/                    # LLM 客户端
│   │   ├── __init__.py
│   │   └── client.py
│   └── utils/                  # 工具函数
│       └── __init__.py
└── proto/                      # gRPC Proto 定义
```

### search-service 结构

```
search-service/
├── go.mod
├── go.sum
├── cmd/
│   └── server/
│       └── main.go             # 入口
├── internal/
│   ├── grpc/                   # gRPC 服务端
│   │   └── handlers.go
│   ├── config/                 # 配置
│   │   └── config.go
│   ├── search/                 # 搜索引擎
│   │   ├── engine.go
│   │   └── query.go
│   ├── matching/               # 匹配算法 (并行计算)
│   │   ├── matcher.go
│   │   └── parallel.go
│   ├── crawler/                # 数据爬虫
│   │   ├── crawler.go
│   │   ├── boss.go
│   │   ├── lagou.go
│   │   └── scheduler.go
│   └── analytics/              # 数据分析
│       └── aggregator.go
└── proto/                      # gRPC Proto 定义
```

---

## 模块与服务映射

| 功能模块 | biz-service 职责 | ai-engine 职责 | search-service 职责 |
|----------|------------------|----------------|---------------------|
| **Auth** | JWT 认证、用户管理 | - | - |
| **Profile** | CRUD、数据持久化 | - | - |
| **Resume** | 文件存储、元数据 | 简历解析 (OCR/NLP) | - |
| **Resume Builder** | 保存生成结果 | AI 生成简历内容 | - |
| **Interview** | 会话管理、历史记录 | AI 面试问答 | - |
| **Mock Interview** | 会话管理、评分存储 | AI 模拟面试 + 评估 | - |
| **Growth** | 目标管理、进度追踪 | 差距分析、学习路径生成 | - |
| **Jobs** | 收藏、申请记录 | 职位分析 | 职位搜索、匹配计算 |
| **Community** | 帖子 CRUD、点赞 | - | - |
| **Notification** | 通知 CRUD | - | - |
| **Settings** | 配置存储 | - | - |

---

## gRPC Proto 定义 (共享)

三个服务共享同一套 Proto 定义，存放在各自的 `proto/` 目录：

```protobuf
// ai_engine.proto
syntax = "proto3";
package cvibe.ai;

service AIEngine {
  // 简历解析
  rpc ParseResume(ParseResumeRequest) returns (ParseResumeResponse);
  
  // 简历生成
  rpc BuildResume(BuildResumeRequest) returns (BuildResumeResponse);
  
  // AI 面试
  rpc StartInterview(StartInterviewRequest) returns (StartInterviewResponse);
  rpc SendMessage(SendMessageRequest) returns (stream MessageChunk);
  
  // 模拟面试
  rpc StartMockInterview(StartMockRequest) returns (StartMockResponse);
  rpc EvaluateAnswer(EvaluateRequest) returns (EvaluateResponse);
  
  // 职业成长
  rpc AnalyzeGap(GapAnalysisRequest) returns (GapAnalysisResponse);
  rpc GenerateLearningPath(LearningPathRequest) returns (LearningPathResponse);
  
  // 职位分析
  rpc AnalyzeJob(AnalyzeJobRequest) returns (AnalyzeJobResponse);
}

// search_service.proto
syntax = "proto3";
package cvibe.search;

service SearchService {
  // 职位搜索
  rpc SearchJobs(SearchJobsRequest) returns (SearchJobsResponse);
  
  // 简历匹配
  rpc MatchResume(MatchResumeRequest) returns (MatchResumeResponse);
  
  // 批量匹配 (并行计算)
  rpc BatchMatch(BatchMatchRequest) returns (BatchMatchResponse);
  
  // 职位推荐
  rpc GetRecommendations(RecommendRequest) returns (RecommendResponse);
}
```
