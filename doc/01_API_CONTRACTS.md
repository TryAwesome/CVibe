# API 契约规范

> **重要**: 本文档定义了前端期望的所有 API 接口，后端必须严格遵守。

---

## 1. Auth 模块 (`/api/auth`)

### 1.1 注册
```
POST /api/auth/register
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123",
  "nickname": "John Doe"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "uuid-string",
      "email": "user@example.com",
      "nickname": "John Doe",
      "role": "ROLE_USER",
      "hasPassword": true,
      "createdAt": "2026-01-17T10:00:00Z",
      "googleUser": false
    }
  }
}
```

### 1.2 登录
```
POST /api/auth/login
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123"
}
```

**Response:** 同注册响应

### 1.3 Google 登录
```
POST /api/auth/google
```

**Request Body:**
```json
{
  "idToken": "google-id-token"
}
```

**Response:** 同注册响应

### 1.4 刷新 Token
```
POST /api/auth/refresh
```

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response:** 同注册响应

### 1.5 获取当前用户
```
GET /api/auth/me
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid-string",
    "email": "user@example.com",
    "nickname": "John Doe",
    "role": "ROLE_USER",
    "hasPassword": true,
    "createdAt": "2026-01-17T10:00:00Z",
    "googleUser": false
  }
}
```

### 1.6 登出
```
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "success": true,
  "data": null
}
```

---

## 2. Profile 模块 (`/api/profile`)

### 2.1 获取资料
```
GET /api/profile
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "headline": "Senior Software Engineer",
    "summary": "Passionate developer...",
    "location": "San Francisco, CA",
    "experiences": [
      {
        "id": "uuid",
        "company": "Google",
        "title": "Software Engineer",
        "location": "Mountain View, CA",
        "employmentType": "FULL_TIME",
        "startDate": "2022-01-01",
        "endDate": null,
        "isCurrent": true,
        "description": "Working on...",
        "achievements": ["Increased performance by 50%"],
        "technologies": ["Java", "Python"]
      }
    ],
    "skills": [
      {
        "id": "uuid",
        "name": "Java",
        "level": "EXPERT"
      }
    ],
    "createdAt": "2026-01-01T00:00:00Z",
    "updatedAt": "2026-01-17T10:00:00Z"
  }
}
```

### 2.2 更新资料
```
PUT /api/profile
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "headline": "Senior Software Engineer",
  "summary": "Passionate developer...",
  "location": "San Francisco, CA"
}
```

### 2.3 获取工作经历
```
GET /api/profile/experiences
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "company": "Google",
      "title": "Software Engineer",
      "location": "Mountain View, CA",
      "employmentType": "FULL_TIME",
      "startDate": "2022-01-01",
      "endDate": null,
      "isCurrent": true,
      "description": "Working on...",
      "achievements": ["Increased performance by 50%"],
      "technologies": ["Java", "Python"]
    }
  ]
}
```

### 2.4 添加工作经历
```
POST /api/profile/experiences
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "company": "Google",
  "title": "Software Engineer",
  "location": "Mountain View, CA",
  "employmentType": "FULL_TIME",
  "startDate": "2022-01-01",
  "endDate": null,
  "isCurrent": true,
  "description": "Working on cloud infrastructure",
  "achievements": ["Increased performance by 50%"],
  "technologies": ["Java", "Python", "Kubernetes"]
}
```

**employmentType 枚举值:**
- `FULL_TIME`
- `PART_TIME`
- `CONTRACT`
- `INTERNSHIP`
- `FREELANCE`

### 2.5 更新工作经历
```
PUT /api/profile/experiences/{experienceId}
Authorization: Bearer {token}
```

### 2.6 删除工作经历
```
DELETE /api/profile/experiences/{experienceId}
Authorization: Bearer {token}
```

### 2.7 获取技能
```
GET /api/profile/skills
Authorization: Bearer {token}
```

### 2.8 添加技能
```
POST /api/profile/skills
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Java",
  "level": "EXPERT"
}
```

### 2.9 删除技能
```
DELETE /api/profile/skills/{skillId}
Authorization: Bearer {token}
```

---

## 3. Resume 模块 (`/api/resumes`)

### 3.1 上传简历
```
POST /api/resumes
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Form Data:**
- `file`: PDF/Word 文件
- `notes` (optional): 备注

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "fileName": "resume_20260117.pdf",
    "originalName": "MyResume.pdf",
    "status": "COMPLETED",
    "isPrimary": false,
    "skills": ["Java", "Python"],
    "parsedData": {},
    "createdAt": "2026-01-17T10:00:00Z",
    "updatedAt": "2026-01-17T10:00:00Z",
    "downloadUrl": "https://..."
  }
}
```

### 3.2 获取所有简历
```
GET /api/resumes
Authorization: Bearer {token}
```

### 3.3 获取单个简历
```
GET /api/resumes/{resumeId}
Authorization: Bearer {token}
```

### 3.4 获取主简历
```
GET /api/resumes/primary
Authorization: Bearer {token}
```

### 3.5 设置主简历
```
PUT /api/resumes/{resumeId}/primary
Authorization: Bearer {token}
```

### 3.6 删除简历
```
DELETE /api/resumes/{resumeId}
Authorization: Bearer {token}
```

---

## 4. Resume Builder 模块 (`/api/v1/resume-builder`)

### 4.1 获取模板列表
```
GET /api/v1/resume-builder/templates
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Professional",
      "description": "Clean professional template",
      "category": "PROFESSIONAL",
      "previewUrl": "https://...",
      "isFeatured": true,
      "createdAt": "2026-01-01T00:00:00Z"
    }
  ]
}
```

### 4.2 获取精选模板
```
GET /api/v1/resume-builder/templates/featured
```

### 4.3 按分类获取模板
```
GET /api/v1/resume-builder/templates/category/{category}
```

### 4.4 获取我的模板
```
GET /api/v1/resume-builder/templates/my
```

### 4.5 获取模板内容
```
GET /api/v1/resume-builder/templates/{templateId}/content
```

**Response:**
```json
{
  "success": true,
  "data": {
    "latexTemplate": "\\documentclass{article}..."
  }
}
```

### 4.6 生成简历
```
POST /api/v1/resume-builder/generate
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "templateId": "uuid",
  "targetJob": "Software Engineer",
  "targetCompany": "Google",
  "customInstructions": "Focus on backend experience"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "templateId": "uuid",
    "status": "PROCESSING",
    "latexContent": null,
    "pdfUrl": null,
    "createdAt": "2026-01-17T10:00:00Z"
  }
}
```

### 4.7 获取生成历史
```
GET /api/v1/resume-builder/generations
```

### 4.8 获取单个生成记录
```
GET /api/v1/resume-builder/generations/{generationId}
```

### 4.9 更新 LaTeX 内容
```
PUT /api/v1/resume-builder/generations/{generationId}/latex
Content-Type: application/json
```

**Request Body:**
```json
{
  "latexContent": "\\documentclass{article}..."
}
```

### 4.10 导出
```
POST /api/v1/resume-builder/generations/{generationId}/export
Content-Type: application/json
```

**Request Body:**
```json
{
  "format": "pdf"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "downloadUrl": "https://..."
  }
}
```

---

## 5. Interview 模块 (`/api/v1/interviews`)

### 5.1 创建面试会话
```
POST /api/v1/interviews/sessions
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "sessionType": "INITIAL_PROFILE",
  "focusArea": "WORK_EXPERIENCE",
  "targetRole": "Software Engineer",
  "language": "en"
}
```

**sessionType 枚举:**
- `INITIAL_PROFILE` - 初始资料收集
- `DEEP_DIVE` - 深入挖掘

**Response:**
```json
{
  "success": true,
  "data": {
    "session": {
      "id": "uuid",
      "sessionType": "INITIAL_PROFILE",
      "status": "IN_PROGRESS",
      "currentQuestionIndex": 0,
      "totalQuestions": 10,
      "focusArea": "WORK_EXPERIENCE",
      "targetRole": "Software Engineer",
      "extractionStatus": "PENDING",
      "startedAt": "2026-01-17T10:00:00Z",
      "lastActivityAt": "2026-01-17T10:00:00Z",
      "completedAt": null,
      "answeredCount": 0,
      "progressPercentage": 0
    },
    "currentQuestion": {
      "id": "uuid",
      "questionId": "uuid",
      "question": "Tell me about yourself",
      "answer": null,
      "category": "INTRO",
      "score": null,
      "feedback": null
    },
    "answeredQuestions": [],
    "hasMoreQuestions": true,
    "sessionCompleted": false,
    "nextAction": "ANSWER_QUESTION"
  }
}
```

### 5.2 获取会话状态
```
GET /api/v1/interviews/sessions/{sessionId}
```

### 5.3 获取所有会话
```
GET /api/v1/interviews/sessions
```

### 5.4 提交答案
```
POST /api/v1/interviews/sessions/{sessionId}/answers
Content-Type: application/json
```

**Request Body:**
```json
{
  "questionId": "uuid",
  "answer": "I am a software engineer with 5 years of experience..."
}
```

### 5.5 获取会话答案
```
GET /api/v1/interviews/sessions/{sessionId}/answers
```

### 5.6 暂停会话
```
POST /api/v1/interviews/sessions/{sessionId}/pause
```

### 5.7 恢复会话
```
POST /api/v1/interviews/sessions/{sessionId}/resume
```

### 5.8 删除会话
```
DELETE /api/v1/interviews/sessions/{sessionId}
```

---

## 6. Mock Interview 模块 (`/api/v1/mock-interview`)

### 6.1 开始模拟面试
```
POST /api/v1/mock-interview/start
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "company": "Google",
  "position": "Software Engineer",
  "resumeId": "uuid"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "company": "Google",
    "position": "Software Engineer",
    "resumeId": "uuid",
    "status": "ACTIVE",
    "currentQuestionNumber": 1,
    "totalQuestions": 10,
    "createdAt": "2026-01-17T10:00:00Z"
  }
}
```

### 6.2 获取面试详情
```
GET /api/v1/mock-interview/{interviewId}
```

### 6.3 获取面试历史
```
GET /api/v1/mock-interview/history
```

### 6.4 获取下一题
```
GET /api/v1/mock-interview/{interviewId}/next-question
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "questionNumber": 1,
    "question": "Tell me about a challenging project...",
    "category": "BEHAVIORAL",
    "hints": ["Think about STAR method"]
  }
}
```

### 6.5 提交答案
```
POST /api/v1/mock-interview/{interviewId}/answer
Content-Type: application/json
```

**Request Body:**
```json
{
  "questionId": "uuid",
  "answer": "In my previous role..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "score": 85,
    "feedback": "Good answer, but could be more specific",
    "sampleAnswer": "A strong answer would include...",
    "nextQuestion": { ... },
    "nextQuestionId": "uuid",
    "isComplete": false
  }
}
```

### 6.6 暂停面试
```
POST /api/v1/mock-interview/{interviewId}/pause
```

### 6.7 恢复面试
```
POST /api/v1/mock-interview/{interviewId}/resume
```

### 6.8 获取统计摘要
```
GET /api/v1/mock-interview/summary
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalInterviews": 10,
    "completedInterviews": 8,
    "averageScore": 78.5,
    "topCompanies": ["Google", "Meta", "Amazon"]
  }
}
```

---

## 7. Growth 模块 (`/api/v1/growth`)

### 7.1 创建目标
```
POST /api/v1/growth/goals
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "targetCompany": "Google",
  "targetPosition": "Senior Software Engineer",
  "deadline": "2026-12-31"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "targetCompany": "Google",
    "targetPosition": "Senior Software Engineer",
    "deadline": "2026-12-31",
    "isPrimary": false,
    "status": "ACTIVE",
    "progress": 0,
    "createdAt": "2026-01-17T10:00:00Z"
  }
}
```

### 7.2 获取所有目标
```
GET /api/v1/growth/goals
```

### 7.3 获取单个目标
```
GET /api/v1/growth/goals/{goalId}
```

### 7.4 更新目标
```
PUT /api/v1/growth/goals/{goalId}
```

### 7.5 删除目标
```
DELETE /api/v1/growth/goals/{goalId}
```

### 7.6 分析差距
```
POST /api/v1/growth/goals/{goalId}/analyze
```

**Response:**
```json
{
  "success": true,
  "data": {
    "score": 65,
    "gaps": [
      {
        "id": "uuid",
        "skill": "System Design",
        "currentLevel": 40,
        "requiredLevel": 80,
        "priority": "HIGH"
      }
    ],
    "strengths": ["Strong coding skills", "Good communication"]
  }
}
```

### 7.7 获取技能差距
```
GET /api/v1/growth/goals/{goalId}/gaps
```

### 7.8 生成学习路径
```
POST /api/v1/growth/goals/{goalId}/generate-paths
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "title": "System Design Mastery",
      "description": "Learn advanced system design",
      "duration": "3 months",
      "milestones": [
        {
          "id": "uuid",
          "title": "Learn fundamentals",
          "description": "Study basic concepts",
          "type": "LEARN",
          "resources": ["Book: Designing Data-Intensive Applications"],
          "isCompleted": false,
          "order": 1
        }
      ]
    }
  ]
}
```

### 7.9 获取学习路径
```
GET /api/v1/growth/goals/{goalId}/paths
```

### 7.10 完成里程碑
```
POST /api/v1/growth/milestones/{milestoneId}/complete
```

### 7.11 取消完成里程碑
```
POST /api/v1/growth/milestones/{milestoneId}/uncomplete
```

### 7.12 获取成长摘要
```
GET /api/v1/growth/summary
```

**Response:**
```json
{
  "success": true,
  "data": {
    "activeGoals": 2,
    "completedMilestones": 5,
    "totalMilestones": 15,
    "overallProgress": 33
  }
}
```

---

## 8. Jobs 模块 (`/api/v1/jobs`)

### 8.1 搜索职位
```
GET /api/v1/jobs?page=0&size=20&title=engineer&company=google&location=sf
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "title": "Software Engineer",
        "company": "Google",
        "location": "San Francisco, CA",
        "description": "We are looking for...",
        "requirements": ["5+ years experience", "Java/Python"],
        "salary": "$150,000 - $200,000",
        "source": "LINKEDIN",
        "sourceUrl": "https://...",
        "isRemote": false,
        "postedAt": "2026-01-15T00:00:00Z",
        "createdAt": "2026-01-16T00:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 8.2 获取最新职位
```
GET /api/v1/jobs/latest
```

### 8.3 获取远程职位
```
GET /api/v1/jobs/remote?page=0&size=20
```

### 8.4 获取职位详情
```
GET /api/v1/jobs/{jobId}
```

### 8.5 生成匹配
```
POST /api/v1/jobs/matches/generate
Authorization: Bearer {token}
```

### 8.6 获取匹配列表
```
GET /api/v1/jobs/matches?page=0&size=20
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "job": { ... },
        "matchScore": 85,
        "matchReasons": ["Skills match", "Experience level match"],
        "status": "NEW",
        "createdAt": "2026-01-17T10:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

### 8.7 获取匹配摘要
```
GET /api/v1/jobs/matches/summary
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalMatches": 50,
    "newMatches": 10,
    "savedJobs": 5,
    "appliedJobs": 3,
    "averageMatchScore": 78.5
  }
}
```

### 8.8 标记已查看
```
POST /api/v1/jobs/matches/{matchId}/view
```

### 8.9 保存职位
```
POST /api/v1/jobs/matches/{matchId}/save
```

### 8.10 标记已申请
```
POST /api/v1/jobs/matches/{matchId}/apply
```

### 8.11 获取已保存职位
```
GET /api/v1/jobs/saved
```

### 8.12 获取已申请职位
```
GET /api/v1/jobs/applied
```

---

## 9. Community 模块 (`/api/v1/community`)

### 9.1 创建帖子
```
POST /api/v1/community/posts
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "content": "Just got my dream job!",
  "category": "SUCCESS_STORY"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "authorId": "uuid",
    "authorName": "John Doe",
    "authorRole": "Software Engineer",
    "content": "Just got my dream job!",
    "category": "SUCCESS_STORY",
    "likesCount": 0,
    "commentsCount": 0,
    "isLiked": false,
    "createdAt": "2026-01-17T10:00:00Z",
    "updatedAt": "2026-01-17T10:00:00Z"
  }
}
```

### 9.2 获取帖子
```
GET /api/v1/community/posts/{postId}
```

### 9.3 更新帖子
```
PUT /api/v1/community/posts/{postId}
```

### 9.4 删除帖子
```
DELETE /api/v1/community/posts/{postId}
```

### 9.5 获取 Feed
```
GET /api/v1/community/feed?page=0&size=20
```

### 9.6 获取热门帖子
```
GET /api/v1/community/posts/trending
```

### 9.7 搜索帖子
```
GET /api/v1/community/posts/search?keyword=interview
```

### 9.8 添加评论
```
POST /api/v1/community/posts/{postId}/comments
Content-Type: application/json
```

**Request Body:**
```json
{
  "content": "Congratulations!"
}
```

### 9.9 获取评论
```
GET /api/v1/community/posts/{postId}/comments
```

### 9.10 点赞帖子
```
POST /api/v1/community/posts/{postId}/like
```

### 9.11 取消点赞
```
DELETE /api/v1/community/posts/{postId}/like
```

---

## 10. Notifications 模块 (`/api/v1/notifications`)

### 10.1 获取通知列表
```
GET /api/v1/notifications?page=0&size=20
```

**Response:**
```json
{
  "success": true,
  "data": {
    "notifications": [
      {
        "id": "uuid",
        "type": "JOB_MATCH",
        "category": "JOBS",
        "title": "New job match",
        "content": "You have a new job match from Google",
        "priority": "NORMAL",
        "actionUrl": "/jobs/matches/uuid",
        "actionText": "View Match",
        "isRead": false,
        "createdAt": "2026-01-17T10:00:00Z"
      }
    ],
    "unreadCount": 5,
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

### 10.2 获取最近通知
```
GET /api/v1/notifications/recent?limit=5
```

### 10.3 获取未读数量
```
GET /api/v1/notifications/unread/count
```

**Response:**
```json
{
  "success": true,
  "data": {
    "total": 5,
    "byCategory": {
      "JOBS": 2,
      "COMMUNITY": 3
    },
    "highPriority": 1
  }
}
```

### 10.4 标记已读
```
POST /api/v1/notifications/{notificationId}/read
```

### 10.5 全部标记已读
```
POST /api/v1/notifications/read-all
```

---

## 11. Settings 模块 (`/api/settings`)

### 11.1 修改密码
```
PUT /api/settings/password
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "currentPassword": "OldPassword123",
  "newPassword": "NewPassword123"
}
```

### 11.2 更新用户信息
```
PUT /api/settings/profile
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "nickname": "John Doe",
  "avatar": "https://..."
}
```

### 11.3 获取 AI 配置
```
GET /api/settings/ai-config
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "provider": "openai",
    "apiKey": "sk-***masked***",
    "modelName": "gpt-4o",
    "baseUrl": null,
    "configured": true,
    "visionProvider": "openai",
    "visionApiKey": "sk-***masked***",
    "visionModelName": "gpt-4o",
    "visionBaseUrl": null,
    "visionConfigured": true,
    "createdAt": "2026-01-01T00:00:00Z",
    "updatedAt": "2026-01-17T10:00:00Z"
  }
}
```

### 11.4 更新 AI 配置
```
PUT /api/settings/ai-config
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "provider": "openai",
  "apiKey": "sk-xxx",
  "modelName": "gpt-4o",
  "baseUrl": null,
  "visionProvider": "openai",
  "visionApiKey": "sk-xxx",
  "visionModelName": "gpt-4o",
  "visionBaseUrl": null
}
```

### 11.5 删除 AI 配置
```
DELETE /api/settings/ai-config
Authorization: Bearer {token}
```

---

## 12. Health 检查 (`/api/health`)

```
GET /api/health
```

**Response:**
```json
{
  "success": true,
  "data": {
    "status": "UP"
  }
}
```
