# CVibe Backend Rewrite - Agent Guide

> 本文档为 AI Agent 提供项目上下文和执行指南

---

## 1. Project Overview

### 1.1 Architecture

```
Frontend (Next.js:3000)
    |
    | HTTP REST API
    v
biz-service (Java:8080) <---> PostgreSQL + Redis + Kafka
    |                   
    +--- gRPC(:50051) ---> ai-engine (Python)
    |                          |
    +--- gRPC(:50052) ---> search-service (Go)
                               |
                          Elasticsearch
```

### 1.2 Services

| Service | Language | Port | Responsibilities |
|---------|----------|------|------------------|
| **biz-service** | Java 21 + Spring Boot 3.2 | 8080 | API Gateway, Business Logic, Auth, Data Persistence |
| **ai-engine** | Python 3.12 + gRPC | 50051 | AI Agents (Resume, Interview, Growth) |
| **search-service** | Go 1.21 + gRPC | 50052 | Search, Matching, Crawler, Analytics |
| **frontend** | Next.js 14 | 3000 | UI (DO NOT MODIFY) |

### 1.3 Infrastructure

| Component | Port | Purpose |
|-----------|------|---------|
| PostgreSQL (pgvector) | 5432 | Primary Database |
| Redis | 6379 | Cache, Session, Rate Limit |
| Kafka | 9092 | Event Streaming |
| Elasticsearch | 9200 | Full-text Search |
| MinIO | 9000/9001 | File Storage (S3-compatible) |

---

## 2. Documentation Index

> Quick reference for finding specifications

### 2.1 Core Documents

| Need to Implement | Reference Document | Key Points |
|-------------------|-------------------|------------|
| Overall Architecture | `doc/00_OVERVIEW.md` | Service responsibilities, tech stack |
| **API Contracts** | `doc/01_API_CONTRACTS.md` | All REST endpoints, request/response formats |
| **Data Models** | `doc/02_DATA_MODELS.md` | Entity design, relationships |
| Error Handling | `doc/14_ERROR_HANDLING.md` | Error codes, response format |
| Coding Standards | `doc/15_CODING_STANDARDS.md` | Code style, naming conventions |

### 2.2 biz-service Modules

| Module | Reference Document | Key Points |
|--------|-------------------|------------|
| Auth (User, JWT) | `doc/03_AUTH_MODULE.md` | **User.nickname** (not fullName), JWT tokens |
| **Profile** | `doc/04_PROFILE_MODULE.md` | UserProfile, Experience, Skill entities |
| Resume | `doc/05_RESUME_MODULE.md` | Resume CRUD, versioning |
| Resume Builder | `doc/06_RESUME_BUILDER_MODULE.md` | AI-powered resume generation |
| Interview | `doc/07_INTERVIEW_MODULE.md` | Interview sessions, AI chat |
| Mock Interview | `doc/08_MOCK_INTERVIEW_MODULE.md` | Practice sessions, evaluation |
| Growth | `doc/09_GROWTH_MODULE.md` | Career goals, learning paths |
| Job | `doc/10_JOB_MODULE.md` | Job search, recommendations |
| Community | `doc/11_COMMUNITY_MODULE.md` | Posts, comments, likes |
| Notification | `doc/12_NOTIFICATION_MODULE.md` | Push notifications, preferences |
| Settings | `doc/13_SETTINGS_MODULE.md` | User preferences, AI config |

### 2.3 Microservices

| Service | Reference Document | Key Points |
|---------|-------------------|------------|
| AI Engine | `doc/16_AI_ENGINE.md` | gRPC service, Agent workflows |
| Search Service | `doc/17_SEARCH_SERVICE.md` | gRPC service, parallel matching |
| **Integration** | `doc/18_SERVICE_INTEGRATION.md` | gRPC config, client stubs |

---

## 3. Critical Implementation Notes

### 3.1 User Entity Fix

**IMPORTANT**: The existing code uses `fullName`, but frontend expects `nickname`:

```java
// WRONG (existing code)
@Column(name = "full_name")
private String fullName;

// CORRECT (must change to)
@Column(name = "nickname", nullable = false, length = 50)
private String nickname;
```

### 3.2 Missing Profile Module

Profile module is **completely missing** from existing code. Must create:

```
com.cvibe.profile/
├── controller/
│   └── ProfileController.java
├── service/
│   ├── ProfileService.java
│   └── ProfileServiceImpl.java
├── repository/
│   ├── UserProfileRepository.java
│   ├── ProfileExperienceRepository.java
│   └── ProfileSkillRepository.java
├── entity/
│   ├── UserProfile.java
│   ├── ProfileExperience.java
│   └── ProfileSkill.java
└── dto/
    ├── ProfileResponse.java
    ├── ProfileUpdateRequest.java
    ├── ExperienceRequest.java
    └── SkillRequest.java
```

### 3.3 Unified Response Format

All API responses MUST follow this format:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": {
    "timestamp": "2024-01-15T10:30:00Z",
    "requestId": "uuid"
  }
}
```

```java
// com.cvibe.common.dto.ApiResponse<T>
@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorInfo error;
    private MetaInfo meta;
}
```

### 3.4 Authentication Pattern

Use `@AuthenticationPrincipal` consistently:

```java
@GetMapping("/me")
public ApiResponse<ProfileResponse> getProfile(
    @AuthenticationPrincipal UserPrincipal principal
) {
    return profileService.getProfile(principal.getUserId());
}
```

**DO NOT** use `@RequestHeader("X-User-Id")` - that pattern is removed.

---

## 4. Execution Plan

### Phase 1: Infrastructure (Current)

- [x] Stop local PostgreSQL/Redis, use Docker
- [x] Update .gitignore
- [x] Create agent.md
- [ ] Create `infra/proto/ai_engine.proto`
- [ ] Create `infra/proto/search_service.proto`
- [ ] Update `infra/docker-compose.yml` (add Kafka, Elasticsearch)
- [ ] Create `.env.example`

### Phase 2: biz-service Core

1. Delete all existing source code in `biz-service/src/main/java`
2. Rebuild with proper structure:

```
com.cvibe/
├── CVibeApplication.java
├── common/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── RedisConfig.java
│   │   ├── KafkaConfig.java
│   │   └── GrpcConfig.java
│   ├── dto/
│   │   ├── ApiResponse.java
│   │   ├── ErrorInfo.java
│   │   └── PageResponse.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── BusinessException.java
│   │   └── ErrorCode.java
│   └── security/
│       ├── JwtTokenProvider.java
│       ├── JwtAuthenticationFilter.java
│       └── UserPrincipal.java
├── auth/
├── profile/
├── resume/
├── interview/
├── mockinterview/
├── growth/
├── job/
├── community/
├── notification/
├── settings/
└── grpc/
    ├── AiEngineClient.java
    └── SearchServiceClient.java
```

### Phase 3: ai-engine

1. Delete existing code
2. Implement gRPC server
3. Implement each Agent:
   - ResumeParserAgent
   - ResumeBuilderAgent
   - InterviewAgent
   - MockInterviewAgent
   - GrowthAgent
   - JobAnalyzerAgent

### Phase 4: search-service

1. Delete existing code
2. Implement gRPC server
3. Implement:
   - Search engine (Elasticsearch)
   - Parallel matcher
   - Crawler (Boss, Lagou)
   - Analytics aggregator

---

## 5. Testing Checklist

### 5.1 Auth Module

- [ ] POST /api/auth/register - Create user with nickname
- [ ] POST /api/auth/login - Get access/refresh tokens
- [ ] POST /api/auth/refresh - Refresh access token
- [ ] POST /api/auth/logout - Invalidate tokens
- [ ] GET /api/auth/me - Get current user

### 5.2 Profile Module

- [ ] GET /api/profile - Get user profile
- [ ] PUT /api/profile - Update profile
- [ ] POST /api/profile/experience - Add experience
- [ ] PUT /api/profile/experience/:id - Update experience
- [ ] DELETE /api/profile/experience/:id - Delete experience
- [ ] POST /api/profile/skills - Add skills
- [ ] DELETE /api/profile/skills/:id - Delete skill

### 5.3 Resume Module

- [ ] POST /api/resumes/upload - Upload resume file
- [ ] GET /api/resumes - List user's resumes
- [ ] GET /api/resumes/:id - Get resume details
- [ ] DELETE /api/resumes/:id - Delete resume

### 5.4 gRPC Integration

- [ ] biz-service can connect to ai-engine:50051
- [ ] biz-service can connect to search-service:50052
- [ ] ParseResume RPC works
- [ ] SearchJobs RPC works
- [ ] MatchResumeToJob RPC works

---

## 6. Common Issues & Solutions

### 6.1 Port Conflicts

If ports are already in use:

```bash
# Check what's using ports
lsof -i :5432 -i :6379 -i :8080 -i :50051 -i :50052

# Stop local services
brew services stop postgresql@14
brew services stop redis
```

### 6.2 Docker Compose

```bash
# Start infrastructure
cd infra && docker-compose up -d

# View logs
docker-compose logs -f postgres redis

# Reset database
docker-compose down -v && docker-compose up -d
```

### 6.3 Proto Compilation

**Java (biz-service)**:
```bash
cd biz-service
mvn clean compile
# Generated code: target/generated-sources/protobuf
```

**Python (ai-engine)**:
```bash
cd ai-engine
python -m grpc_tools.protoc -I./proto --python_out=./src --grpc_python_out=./src ./proto/*.proto
```

**Go (search-service)**:
```bash
cd search-service
protoc --go_out=. --go-grpc_out=. proto/search_service.proto
```

---

## 7. Environment Variables

See `.env.example` for all required variables:

```bash
# Database
DATABASE_URL=postgresql://cvibe:cvibe123@localhost:5432/cvibe

# Redis
REDIS_URL=redis://localhost:6379

# JWT
JWT_SECRET=your-256-bit-secret
JWT_ACCESS_EXPIRY=15m
JWT_REFRESH_EXPIRY=7d

# gRPC
AI_ENGINE_HOST=localhost
AI_ENGINE_PORT=50051
SEARCH_SERVICE_HOST=localhost
SEARCH_SERVICE_PORT=50052

# AI
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Elasticsearch
ELASTICSEARCH_URL=http://localhost:9200
```

---

## 8. Git Workflow

- Branch: `dev` (local and remote)
- Commit style: Conventional Commits (`feat:`, `fix:`, `refactor:`, etc.)
- No force push to `main` or `dev`

---

*Last updated: 2025-01-17*
