/**
 * API Client for CVibe Backend
 */

const API_BASE_URL = '/api';

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  meta?: {
    timestamp: string;
  };
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface User {
  id: string;
  email: string;
  nickname?: string;
  role: string;
  hasPassword: boolean;
  createdAt: string;
  googleUser: boolean;
}

class ApiClient {
  private getToken(): string | null {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('accessToken');
    }
    return null;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const token = this.getToken();
    
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (token) {
      (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers,
      });

      const data = await response.json();
      
      if (!response.ok) {
        // Handle error object format: {code, message}
        const errorMessage = typeof data.error === 'object' && data.error?.message 
          ? data.error.message 
          : (data.error || data.message || 'Request failed');
        return {
          success: false,
          error: errorMessage,
        };
      }

      return data;
    } catch (error) {
      console.error('API Error:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Network error',
      };
    }
  }

  // Auth endpoints
  async register(email: string, password: string, nickname: string): Promise<ApiResponse<AuthResponse>> {
    return this.request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password, nickname }),
    });
  }

  async login(email: string, password: string): Promise<ApiResponse<AuthResponse>> {
    return this.request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  }

  async logout(): Promise<ApiResponse<void>> {
    return this.request<void>('/auth/logout', {
      method: 'POST',
    });
  }

  async getCurrentUser(): Promise<ApiResponse<User>> {
    return this.request<User>('/auth/me');
  }

  async refreshToken(refreshToken: string): Promise<ApiResponse<AuthResponse>> {
    return this.request<AuthResponse>('/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    });
  }

  // Health check
  async health(): Promise<ApiResponse<{ status: string }>> {
    return this.request<{ status: string }>('/health');
  }

  // ==================== Jobs API ====================
  async getJobs(params?: {
    page?: number;
    size?: number;
    title?: string;
    company?: string;
    location?: string;
  }): Promise<ApiResponse<PagedResponse<Job>>> {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());
    if (params?.title) query.append('title', params.title);
    if (params?.company) query.append('company', params.company);
    if (params?.location) query.append('location', params.location);
    return this.request<PagedResponse<Job>>(`/jobs?${query.toString()}`);
  }

  async getLatestJobs(): Promise<ApiResponse<Job[]>> {
    return this.request<Job[]>('/jobs/latest');
  }

  async getRemoteJobs(page?: number, size?: number): Promise<ApiResponse<PagedResponse<Job>>> {
    const query = new URLSearchParams();
    if (page) query.append('page', page.toString());
    if (size) query.append('size', size.toString());
    return this.request<PagedResponse<Job>>(`/jobs/remote?${query.toString()}`);
  }

  async getJobById(jobId: string): Promise<ApiResponse<Job>> {
    return this.request<Job>(`/jobs/${jobId}`);
  }

  async generateJobMatches(): Promise<ApiResponse<void>> {
    return this.request<void>('/jobs/matches/generate', { method: 'POST' });
  }

  async getJobMatches(page?: number, size?: number): Promise<ApiResponse<PagedResponse<JobMatch>>> {
    const query = new URLSearchParams();
    if (page) query.append('page', page.toString());
    if (size) query.append('size', size.toString());
    return this.request<PagedResponse<JobMatch>>(`/jobs/matches?${query.toString()}`);
  }

  async getJobMatchSummary(): Promise<ApiResponse<JobMatchSummary>> {
    return this.request<JobMatchSummary>('/jobs/matches/summary');
  }

  async viewJobMatch(matchId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/jobs/matches/${matchId}/view`, { method: 'POST' });
  }

  async saveJobMatch(matchId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/jobs/matches/${matchId}/save`, { method: 'POST' });
  }

  async applyToJob(matchId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/jobs/matches/${matchId}/apply`, { method: 'POST' });
  }

  async getSavedJobs(): Promise<ApiResponse<JobMatch[]>> {
    return this.request<JobMatch[]>('/jobs/saved');
  }

  async getAppliedJobs(): Promise<ApiResponse<JobMatch[]>> {
    return this.request<JobMatch[]>('/jobs/applied');
  }

  // ==================== Resume API ====================
  async uploadResume(file: File): Promise<ApiResponse<Resume>> {
    const formData = new FormData();
    formData.append('file', file);
    
    const token = this.getToken();
    const headers: HeadersInit = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}/resumes`, {
      method: 'POST',
      headers,
      body: formData,
    });
    return response.json();
  }

  async getResumes(): Promise<ApiResponse<Resume[]>> {
    return this.request<Resume[]>('/resumes');
  }

  async getResumeById(resumeId: string): Promise<ApiResponse<Resume>> {
    return this.request<Resume>(`/resumes/${resumeId}`);
  }

  async getPrimaryResume(): Promise<ApiResponse<Resume>> {
    return this.request<Resume>('/resumes/primary');
  }

  async setPrimaryResume(resumeId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/resumes/${resumeId}/primary`, { method: 'PUT' });
  }

  async deleteResume(resumeId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/resumes/${resumeId}`, { method: 'DELETE' });
  }

  // ==================== Resume Builder API ====================
  async getTemplates(): Promise<ApiResponse<ResumeTemplate[]>> {
    return this.request<ResumeTemplate[]>('/resume-builder/templates');
  }

  async getFeaturedTemplates(): Promise<ApiResponse<ResumeTemplate[]>> {
    return this.request<ResumeTemplate[]>('/resume-builder/templates/featured');
  }

  async getTemplatesByCategory(category: string): Promise<ApiResponse<ResumeTemplate[]>> {
    return this.request<ResumeTemplate[]>(`/resume-builder/templates/category/${category}`);
  }

  async getMyTemplates(): Promise<ApiResponse<ResumeTemplate[]>> {
    return this.request<ResumeTemplate[]>('/resume-builder/templates/my');
  }

  async getTemplateContent(templateId: string): Promise<ApiResponse<{ latexTemplate: string }>> {
    return this.request<{ latexTemplate: string }>(`/resume-builder/templates/${templateId}/content`);
  }

  async generateResume(request: GenerateResumeRequest): Promise<ApiResponse<ResumeGeneration>> {
    return this.request<ResumeGeneration>('/resume-builder/generate', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getGenerations(): Promise<ApiResponse<ResumeGeneration[]>> {
    return this.request<ResumeGeneration[]>('/resume-builder/generations');
  }

  async getGenerationById(generationId: string): Promise<ApiResponse<ResumeGeneration>> {
    return this.request<ResumeGeneration>(`/resume-builder/generations/${generationId}`);
  }

  async updateGenerationLatex(generationId: string, latexContent: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/resume-builder/generations/${generationId}/latex`, {
      method: 'PUT',
      body: JSON.stringify({ latexContent }),
    });
  }

  async exportGeneration(generationId: string, format: 'pdf' | 'latex'): Promise<ApiResponse<{ downloadUrl: string }>> {
    return this.request<{ downloadUrl: string }>(`/resume-builder/generations/${generationId}/export`, {
      method: 'POST',
      body: JSON.stringify({ format }),
    });
  }

  // ==================== Interview API ====================
  async createInterviewSession(request: CreateInterviewRequest): Promise<ApiResponse<InterviewSession>> {
    return this.request<InterviewSession>('/interviews/sessions', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getInterviewSession(sessionId: string): Promise<ApiResponse<InterviewSession>> {
    return this.request<InterviewSession>(`/interviews/sessions/${sessionId}`);
  }

  async getInterviewSessions(): Promise<ApiResponse<InterviewSession[]>> {
    return this.request<InterviewSession[]>('/interviews/sessions');
  }

  async submitAnswer(sessionId: string, answer: SubmitAnswerRequest): Promise<ApiResponse<AnswerFeedback>> {
    return this.request<AnswerFeedback>(`/interviews/sessions/${sessionId}/answers`, {
      method: 'POST',
      body: JSON.stringify(answer),
    });
  }

  async getAnswers(sessionId: string): Promise<ApiResponse<Answer[]>> {
    return this.request<Answer[]>(`/interviews/sessions/${sessionId}/answers`);
  }

  async pauseInterviewSession(sessionId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/interviews/sessions/${sessionId}/pause`, { method: 'POST' });
  }

  async resumeInterviewSession(sessionId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/interviews/sessions/${sessionId}/resume`, { method: 'POST' });
  }

  async deleteInterviewSession(sessionId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/interviews/sessions/${sessionId}`, { method: 'DELETE' });
  }

  // ==================== Mock Interview API ====================
  async startMockInterview(request: StartMockInterviewRequest): Promise<ApiResponse<MockInterview>> {
    return this.request<MockInterview>('/mock-interviews/start', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getMockInterview(interviewId: string): Promise<ApiResponse<MockInterview>> {
    return this.request<MockInterview>(`/mock-interviews/${interviewId}`);
  }

  async getMockInterviewHistory(): Promise<ApiResponse<MockInterview[]>> {
    return this.request<MockInterview[]>('/mock-interviews/history');
  }

  async getNextQuestion(interviewId: string): Promise<ApiResponse<MockQuestion>> {
    return this.request<MockQuestion>(`/mock-interviews/${interviewId}/next-question`);
  }

  async submitMockAnswer(interviewId: string, answer: SubmitMockAnswerRequest): Promise<ApiResponse<MockAnswerFeedback>> {
    return this.request<MockAnswerFeedback>(`/mock-interviews/${interviewId}/answer`, {
      method: 'POST',
      body: JSON.stringify(answer),
    });
  }

  async pauseMockInterview(interviewId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/mock-interviews/${interviewId}/pause`, { method: 'POST' });
  }

  async resumeMockInterview(interviewId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/mock-interviews/${interviewId}/resume`, { method: 'POST' });
  }

  async getMockInterviewSummary(): Promise<ApiResponse<MockInterviewSummary>> {
    return this.request<MockInterviewSummary>('/mock-interviews/summary');
  }

  // ==================== Growth API ====================
  async createGoal(request: CreateGoalRequest): Promise<ApiResponse<GrowthGoal>> {
    return this.request<GrowthGoal>('/growth/goals', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getGoals(): Promise<ApiResponse<GrowthGoal[]>> {
    return this.request<GrowthGoal[]>('/growth/goals');
  }

  async getGoalById(goalId: string): Promise<ApiResponse<GrowthGoal>> {
    return this.request<GrowthGoal>(`/growth/goals/${goalId}`);
  }

  async updateGoal(goalId: string, request: UpdateGoalRequest): Promise<ApiResponse<GrowthGoal>> {
    return this.request<GrowthGoal>(`/growth/goals/${goalId}`, {
      method: 'PUT',
      body: JSON.stringify(request),
    });
  }

  async deleteGoal(goalId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/growth/goals/${goalId}`, { method: 'DELETE' });
  }

  async analyzeGoal(goalId: string): Promise<ApiResponse<GapAnalysis>> {
    return this.request<GapAnalysis>(`/growth/goals/${goalId}/analyze`, { method: 'POST' });
  }

  async getGaps(goalId: string): Promise<ApiResponse<SkillGap[]>> {
    return this.request<SkillGap[]>(`/growth/goals/${goalId}/gaps`);
  }

  async generateLearningPaths(goalId: string): Promise<ApiResponse<LearningPath[]>> {
    return this.request<LearningPath[]>(`/growth/goals/${goalId}/generate-paths`, { method: 'POST' });
  }

  async getLearningPaths(goalId: string): Promise<ApiResponse<LearningPath[]>> {
    return this.request<LearningPath[]>(`/growth/goals/${goalId}/paths`);
  }

  async completeMilestone(milestoneId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/growth/milestones/${milestoneId}/complete`, { method: 'POST' });
  }

  async uncompleteMilestone(milestoneId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/growth/milestones/${milestoneId}/uncomplete`, { method: 'POST' });
  }

  async getGrowthSummary(): Promise<ApiResponse<GrowthSummary>> {
    return this.request<GrowthSummary>('/growth/summary');
  }

  // ==================== Community API ====================
  async createPost(request: CreatePostRequest): Promise<ApiResponse<Post>> {
    return this.request<Post>('/community/posts', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getPost(postId: string): Promise<ApiResponse<Post>> {
    return this.request<Post>(`/community/posts/${postId}`);
  }

  async updatePost(postId: string, request: UpdatePostRequest): Promise<ApiResponse<Post>> {
    return this.request<Post>(`/community/posts/${postId}`, {
      method: 'PUT',
      body: JSON.stringify(request),
    });
  }

  async deletePost(postId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/community/posts/${postId}`, { method: 'DELETE' });
  }

  async getFeed(page?: number, size?: number): Promise<ApiResponse<PagedResponse<Post>>> {
    const query = new URLSearchParams();
    if (page) query.append('page', page.toString());
    if (size) query.append('size', size.toString());
    return this.request<PagedResponse<Post>>(`/community/feed?${query.toString()}`);
  }

  async getTrendingPosts(): Promise<ApiResponse<Post[]>> {
    return this.request<Post[]>('/community/posts/trending');
  }

  async searchPosts(keyword: string): Promise<ApiResponse<Post[]>> {
    return this.request<Post[]>(`/community/posts/search?keyword=${encodeURIComponent(keyword)}`);
  }

  async createComment(postId: string, request: CreateCommentRequest): Promise<ApiResponse<Comment>> {
    return this.request<Comment>(`/community/posts/${postId}/comments`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getComments(postId: string): Promise<ApiResponse<Comment[]>> {
    return this.request<Comment[]>(`/community/posts/${postId}/comments`);
  }

  async likePost(postId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/community/posts/${postId}/like`, { method: 'POST' });
  }

  async unlikePost(postId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/community/posts/${postId}/like`, { method: 'DELETE' });
  }

  // ==================== Settings API ====================
  async updatePassword(currentPassword: string, newPassword: string): Promise<ApiResponse<void>> {
    return this.request<void>('/settings/password', {
      method: 'PUT',
      body: JSON.stringify({ currentPassword, newPassword }),
    });
  }

  async updateProfile(request: UpdateProfileRequest): Promise<ApiResponse<User>> {
    return this.request<User>('/settings/profile', {
      method: 'PUT',
      body: JSON.stringify(request),
    });
  }

  async getAiConfig(): Promise<ApiResponse<AiConfig>> {
    return this.request<AiConfig>('/settings/ai-config');
  }

  async updateAiConfig(config: UpdateAiConfigRequest): Promise<ApiResponse<AiConfig>> {
    return this.request<AiConfig>('/settings/ai-config', {
      method: 'PUT',
      body: JSON.stringify(config),
    });
  }

  async deleteAiConfig(): Promise<ApiResponse<void>> {
    return this.request<void>('/settings/ai-config', { method: 'DELETE' });
  }

  // ==================== Notification API ====================
  async getNotifications(page = 0, size = 20): Promise<ApiResponse<NotificationListResponse>> {
    return this.request<NotificationListResponse>(`/v1/notifications?page=${page}&size=${size}`);
  }

  async getRecentNotifications(limit = 5): Promise<ApiResponse<NotificationItem[]>> {
    return this.request<NotificationItem[]>(`/v1/notifications/recent?limit=${limit}`);
  }

  async getUnreadCount(): Promise<ApiResponse<UnreadCountResponse>> {
    return this.request<UnreadCountResponse>('/v1/notifications/unread/count');
  }

  async markNotificationAsRead(notificationId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/v1/notifications/${notificationId}/read`, { method: 'POST' });
  }

  async markAllNotificationsAsRead(): Promise<ApiResponse<void>> {
    return this.request<void>('/v1/notifications/read-all', { method: 'POST' });
  }

  // ==================== Profile API ====================
  async getProfile(): Promise<ApiResponse<Profile>> {
    return this.request<Profile>('/profile');
  }

  async updateUserProfile(request: UpdateUserProfileRequest): Promise<ApiResponse<Profile>> {
    return this.request<Profile>('/profile', {
      method: 'PUT',
      body: JSON.stringify(request),
    });
  }

  async getExperiences(): Promise<ApiResponse<Experience[]>> {
    return this.request<Experience[]>('/profile/experiences');
  }

  async createExperience(request: AddExperienceRequest): Promise<ApiResponse<Experience>> {
    return this.request<Experience>('/profile/experiences', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async addExperience(request: AddExperienceRequest): Promise<ApiResponse<Experience>> {
    return this.request<Experience>('/profile/experiences', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async updateExperience(experienceId: string, request: AddExperienceRequest): Promise<ApiResponse<Experience>> {
    return this.request<Experience>(`/profile/experiences/${experienceId}`, {
      method: 'PUT',
      body: JSON.stringify(request),
    });
  }

  async deleteExperience(experienceId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/profile/experiences/${experienceId}`, { method: 'DELETE' });
  }

  async getSkills(): Promise<ApiResponse<Skill[]>> {
    return this.request<Skill[]>('/profile/skills');
  }

  async createSkill(request: AddSkillRequest): Promise<ApiResponse<Skill>> {
    return this.request<Skill>('/profile/skills', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async addSkill(request: AddSkillRequest): Promise<ApiResponse<Skill>> {
    return this.request<Skill>('/profile/skills', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async deleteSkill(skillId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/profile/skills/${skillId}`, { method: 'DELETE' });
  }
}

export const api = new ApiClient();
export default api;

// ==================== Type Definitions ====================

// Common types
export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// Job types
export interface Job {
  id: string;
  title: string;
  company: string;
  location: string;
  description: string;
  requirements: string[];
  salary?: string;
  source: string;
  sourceUrl?: string;
  isRemote: boolean;
  postedAt: string;
  createdAt: string;
}

export interface JobMatch {
  id: string;
  job: Job;
  matchScore: number;
  matchReasons: string[];
  status: 'NEW' | 'VIEWED' | 'SAVED' | 'APPLIED' | 'REJECTED';
  createdAt: string;
}

export interface JobMatchSummary {
  totalMatches: number;
  newMatches: number;
  savedJobs: number;
  appliedJobs: number;
  averageMatchScore: number;
}

// Resume types
export interface Resume {
  id: string;
  fileName: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  isPrimary: boolean;
  skills: string[];
  parsedData?: object;
  createdAt: string;
  updatedAt: string;
}

export interface ResumeTemplate {
  id: string;
  name: string;
  description: string;
  category: string;
  previewUrl: string;
  isFeatured: boolean;
  createdAt: string;
}

export interface ResumeGeneration {
  id: string;
  templateId: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  latexContent?: string;
  pdfUrl?: string;
  createdAt: string;
}

export interface GenerateResumeRequest {
  templateId: string;
  targetJob?: string;
  targetCompany?: string;
  customInstructions?: string;
}

// Interview types
export interface InterviewSession {
  id: string;
  company: string;
  position: string;
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'active' | 'paused' | 'completed';
  questionsAnswered: number;
  totalQuestions: number;
  score?: number;
  createdAt: string;
}

export interface CreateInterviewRequest {
  company: string;
  position: string;
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD';
}

export interface SubmitAnswerRequest {
  questionId: string;
  answer: string;
}

export interface AnswerFeedback {
  score: number;
  feedback: string;
  improvements: string[];
  nextQuestion?: InterviewQuestion;
}

export interface Answer {
  id: string;
  questionId: string;
  question?: string;
  answer: string;
  score: number;
  feedback: string;
  createdAt: string;
}

// Alias for compatibility
export type InterviewAnswer = Answer;

export interface InterviewQuestion {
  id: string;
  question: string;
  category: string;
  difficulty: string;
}

// Mock Interview types
export interface MockInterview {
  id: string;
  company: string;
  position: string;
  resumeId?: string;
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED';
  currentQuestionNumber: number;
  totalQuestions: number;
  createdAt: string;
}

export interface StartMockInterviewRequest {
  company: string;
  position: string;
  resumeId?: string;
}

export interface MockQuestion {
  id: string;
  questionNumber: number;
  question: string;
  category: string;
  hints?: string[];
}

// Aliases for compatibility
export type MockInterviewQuestion = MockQuestion;

export interface SubmitMockAnswerRequest {
  questionId: string;
  answer: string;
}

export interface MockAnswerFeedback {
  score: number;
  feedback: string;
  sampleAnswer?: string;
  nextQuestion?: MockQuestion;
  nextQuestionId?: string;
  isComplete: boolean;
}

// Alias for compatibility
export type MockInterviewFeedback = MockAnswerFeedback;

export interface MockInterviewSummary {
  totalInterviews: number;
  completedInterviews: number;
  averageScore: number;
  topCompanies: string[];
}

// Growth types
export interface GrowthGoal {
  id: string;
  targetCompany: string;
  targetPosition: string;
  deadline?: string;
  isPrimary: boolean;
  status: 'ACTIVE' | 'ACHIEVED' | 'ABANDONED';
  progress: number;
  createdAt: string;
}

export interface CreateGoalRequest {
  targetCompany: string;
  targetPosition: string;
  deadline?: string;
}

export interface UpdateGoalRequest {
  targetCompany?: string;
  targetPosition?: string;
  deadline?: string;
}

export interface GapAnalysis {
  score: number;
  gaps: SkillGap[];
  strengths: string[];
}

export interface SkillGap {
  id: string;
  skill: string;
  currentLevel: number;
  requiredLevel: number;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
}

export interface LearningPath {
  id: string;
  title: string;
  description: string;
  duration: string;
  milestones: Milestone[];
}

export interface Milestone {
  id: string;
  title: string;
  description: string;
  type: 'LEARN' | 'PROJECT' | 'ASSESSMENT';
  resources: string[];
  isCompleted: boolean;
  order: number;
}

export interface GrowthSummary {
  activeGoals: number;
  completedMilestones: number;
  totalMilestones: number;
  overallProgress: number;
}

// Community types
export interface Post {
  id: string;
  authorId: string;
  authorName: string;
  authorRole?: string;
  content: string;
  category?: string;
  likesCount: number;
  commentsCount: number;
  isLiked: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePostRequest {
  content: string;
  category?: string;
}

export interface UpdatePostRequest {
  content: string;
}

export interface Comment {
  id: string;
  authorId: string;
  authorName: string;
  content: string;
  likesCount: number;
  isLiked: boolean;
  createdAt: string;
}

export interface CreateCommentRequest {
  content: string;
}

// Settings types
export interface UpdateProfileRequest {
  nickname?: string;
  avatar?: string;
}

export interface AiConfig {
  provider: string;
  apiKey?: string;
  model?: string;
  createdAt: string;
}

export interface UpdateAiConfigRequest {
  provider: string;
  apiKey: string;
  model?: string;
}

// Profile types
export interface Profile {
  id: string;
  userId: string;
  headline?: string;
  summary?: string;
  location?: string;
  experiences: Experience[];
  skills: Skill[];
  createdAt: string;
  updatedAt: string;
}

export interface Experience {
  id: number;
  type: 'education' | 'work' | 'award';
  title: string;
  organization?: string;
  startDate?: string;
  endDate?: string;
  description?: string;
}

export interface AddExperienceRequest {
  type: 'education' | 'work' | 'award';
  title: string;
  organization?: string;
  startDate?: string;
  endDate?: string;
  description?: string;
}

export interface Skill {
  id: number;
  name: string;
  level: string;
}

export interface AddSkillRequest {
  name: string;
  level: string;
}

// Notification types
export interface NotificationItem {
  id: string;
  type: string;
  category: string;
  title: string;
  content: string;
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  actionUrl?: string;
  actionText?: string;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationListResponse {
  notifications: NotificationItem[];
  unreadCount: number;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UnreadCountResponse {
  total: number;
  byCategory: Record<string, number>;
  highPriority: number;
}

export interface UpdateUserProfileRequest {
  headline?: string;
  summary?: string;
  location?: string;
}
