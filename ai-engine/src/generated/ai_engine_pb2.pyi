from google.protobuf.internal import containers as _containers
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class ParseResumeRequest(_message.Message):
    __slots__ = ("file_content", "file_name", "file_type")
    FILE_CONTENT_FIELD_NUMBER: _ClassVar[int]
    FILE_NAME_FIELD_NUMBER: _ClassVar[int]
    FILE_TYPE_FIELD_NUMBER: _ClassVar[int]
    file_content: bytes
    file_name: str
    file_type: str
    def __init__(self, file_content: _Optional[bytes] = ..., file_name: _Optional[str] = ..., file_type: _Optional[str] = ...) -> None: ...

class ParseResumeResponse(_message.Message):
    __slots__ = ("success", "data", "error_message")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    DATA_FIELD_NUMBER: _ClassVar[int]
    ERROR_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    success: bool
    data: ResumeData
    error_message: str
    def __init__(self, success: bool = ..., data: _Optional[_Union[ResumeData, _Mapping]] = ..., error_message: _Optional[str] = ...) -> None: ...

class ResumeData(_message.Message):
    __slots__ = ("name", "email", "phone", "summary", "experiences", "educations", "skills", "raw_text")
    NAME_FIELD_NUMBER: _ClassVar[int]
    EMAIL_FIELD_NUMBER: _ClassVar[int]
    PHONE_FIELD_NUMBER: _ClassVar[int]
    SUMMARY_FIELD_NUMBER: _ClassVar[int]
    EXPERIENCES_FIELD_NUMBER: _ClassVar[int]
    EDUCATIONS_FIELD_NUMBER: _ClassVar[int]
    SKILLS_FIELD_NUMBER: _ClassVar[int]
    RAW_TEXT_FIELD_NUMBER: _ClassVar[int]
    name: str
    email: str
    phone: str
    summary: str
    experiences: _containers.RepeatedCompositeFieldContainer[ExperienceData]
    educations: _containers.RepeatedCompositeFieldContainer[EducationData]
    skills: _containers.RepeatedScalarFieldContainer[str]
    raw_text: str
    def __init__(self, name: _Optional[str] = ..., email: _Optional[str] = ..., phone: _Optional[str] = ..., summary: _Optional[str] = ..., experiences: _Optional[_Iterable[_Union[ExperienceData, _Mapping]]] = ..., educations: _Optional[_Iterable[_Union[EducationData, _Mapping]]] = ..., skills: _Optional[_Iterable[str]] = ..., raw_text: _Optional[str] = ...) -> None: ...

class ExperienceData(_message.Message):
    __slots__ = ("company", "title", "start_date", "end_date", "description", "is_current")
    COMPANY_FIELD_NUMBER: _ClassVar[int]
    TITLE_FIELD_NUMBER: _ClassVar[int]
    START_DATE_FIELD_NUMBER: _ClassVar[int]
    END_DATE_FIELD_NUMBER: _ClassVar[int]
    DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    IS_CURRENT_FIELD_NUMBER: _ClassVar[int]
    company: str
    title: str
    start_date: str
    end_date: str
    description: str
    is_current: bool
    def __init__(self, company: _Optional[str] = ..., title: _Optional[str] = ..., start_date: _Optional[str] = ..., end_date: _Optional[str] = ..., description: _Optional[str] = ..., is_current: bool = ...) -> None: ...

class EducationData(_message.Message):
    __slots__ = ("school", "degree", "field", "start_date", "end_date")
    SCHOOL_FIELD_NUMBER: _ClassVar[int]
    DEGREE_FIELD_NUMBER: _ClassVar[int]
    FIELD_FIELD_NUMBER: _ClassVar[int]
    START_DATE_FIELD_NUMBER: _ClassVar[int]
    END_DATE_FIELD_NUMBER: _ClassVar[int]
    school: str
    degree: str
    field: str
    start_date: str
    end_date: str
    def __init__(self, school: _Optional[str] = ..., degree: _Optional[str] = ..., field: _Optional[str] = ..., start_date: _Optional[str] = ..., end_date: _Optional[str] = ...) -> None: ...

class BuildResumeRequest(_message.Message):
    __slots__ = ("user_id", "job_title", "job_description", "profile", "language")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    JOB_TITLE_FIELD_NUMBER: _ClassVar[int]
    JOB_DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    PROFILE_FIELD_NUMBER: _ClassVar[int]
    LANGUAGE_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    job_title: str
    job_description: str
    profile: ProfileData
    language: str
    def __init__(self, user_id: _Optional[str] = ..., job_title: _Optional[str] = ..., job_description: _Optional[str] = ..., profile: _Optional[_Union[ProfileData, _Mapping]] = ..., language: _Optional[str] = ...) -> None: ...

class ProfileData(_message.Message):
    __slots__ = ("name", "title", "summary", "experiences", "educations", "skills")
    NAME_FIELD_NUMBER: _ClassVar[int]
    TITLE_FIELD_NUMBER: _ClassVar[int]
    SUMMARY_FIELD_NUMBER: _ClassVar[int]
    EXPERIENCES_FIELD_NUMBER: _ClassVar[int]
    EDUCATIONS_FIELD_NUMBER: _ClassVar[int]
    SKILLS_FIELD_NUMBER: _ClassVar[int]
    name: str
    title: str
    summary: str
    experiences: _containers.RepeatedCompositeFieldContainer[ExperienceData]
    educations: _containers.RepeatedCompositeFieldContainer[EducationData]
    skills: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, name: _Optional[str] = ..., title: _Optional[str] = ..., summary: _Optional[str] = ..., experiences: _Optional[_Iterable[_Union[ExperienceData, _Mapping]]] = ..., educations: _Optional[_Iterable[_Union[EducationData, _Mapping]]] = ..., skills: _Optional[_Iterable[str]] = ...) -> None: ...

class BuildResumeChunk(_message.Message):
    __slots__ = ("section", "content", "is_final")
    SECTION_FIELD_NUMBER: _ClassVar[int]
    CONTENT_FIELD_NUMBER: _ClassVar[int]
    IS_FINAL_FIELD_NUMBER: _ClassVar[int]
    section: str
    content: str
    is_final: bool
    def __init__(self, section: _Optional[str] = ..., content: _Optional[str] = ..., is_final: bool = ...) -> None: ...

class StartInterviewRequest(_message.Message):
    __slots__ = ("user_id", "session_id", "job_title", "job_description", "resume_content", "config")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    SESSION_ID_FIELD_NUMBER: _ClassVar[int]
    JOB_TITLE_FIELD_NUMBER: _ClassVar[int]
    JOB_DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    RESUME_CONTENT_FIELD_NUMBER: _ClassVar[int]
    CONFIG_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    session_id: str
    job_title: str
    job_description: str
    resume_content: str
    config: InterviewConfig
    def __init__(self, user_id: _Optional[str] = ..., session_id: _Optional[str] = ..., job_title: _Optional[str] = ..., job_description: _Optional[str] = ..., resume_content: _Optional[str] = ..., config: _Optional[_Union[InterviewConfig, _Mapping]] = ...) -> None: ...

class InterviewConfig(_message.Message):
    __slots__ = ("language", "difficulty", "focus_areas")
    LANGUAGE_FIELD_NUMBER: _ClassVar[int]
    DIFFICULTY_FIELD_NUMBER: _ClassVar[int]
    FOCUS_AREAS_FIELD_NUMBER: _ClassVar[int]
    language: str
    difficulty: str
    focus_areas: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, language: _Optional[str] = ..., difficulty: _Optional[str] = ..., focus_areas: _Optional[_Iterable[str]] = ...) -> None: ...

class StartInterviewResponse(_message.Message):
    __slots__ = ("success", "welcome_message", "first_question")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    WELCOME_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    FIRST_QUESTION_FIELD_NUMBER: _ClassVar[int]
    success: bool
    welcome_message: str
    first_question: str
    def __init__(self, success: bool = ..., welcome_message: _Optional[str] = ..., first_question: _Optional[str] = ...) -> None: ...

class SendMessageRequest(_message.Message):
    __slots__ = ("session_id", "user_message", "history")
    SESSION_ID_FIELD_NUMBER: _ClassVar[int]
    USER_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    HISTORY_FIELD_NUMBER: _ClassVar[int]
    session_id: str
    user_message: str
    history: _containers.RepeatedCompositeFieldContainer[ChatMessage]
    def __init__(self, session_id: _Optional[str] = ..., user_message: _Optional[str] = ..., history: _Optional[_Iterable[_Union[ChatMessage, _Mapping]]] = ...) -> None: ...

class ChatMessage(_message.Message):
    __slots__ = ("role", "content")
    ROLE_FIELD_NUMBER: _ClassVar[int]
    CONTENT_FIELD_NUMBER: _ClassVar[int]
    role: str
    content: str
    def __init__(self, role: _Optional[str] = ..., content: _Optional[str] = ...) -> None: ...

class MessageChunk(_message.Message):
    __slots__ = ("content", "is_final", "next_question")
    CONTENT_FIELD_NUMBER: _ClassVar[int]
    IS_FINAL_FIELD_NUMBER: _ClassVar[int]
    NEXT_QUESTION_FIELD_NUMBER: _ClassVar[int]
    content: str
    is_final: bool
    next_question: str
    def __init__(self, content: _Optional[str] = ..., is_final: bool = ..., next_question: _Optional[str] = ...) -> None: ...

class StartMockRequest(_message.Message):
    __slots__ = ("user_id", "session_id", "job_title", "interview_type", "question_count", "language")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    SESSION_ID_FIELD_NUMBER: _ClassVar[int]
    JOB_TITLE_FIELD_NUMBER: _ClassVar[int]
    INTERVIEW_TYPE_FIELD_NUMBER: _ClassVar[int]
    QUESTION_COUNT_FIELD_NUMBER: _ClassVar[int]
    LANGUAGE_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    session_id: str
    job_title: str
    interview_type: str
    question_count: int
    language: str
    def __init__(self, user_id: _Optional[str] = ..., session_id: _Optional[str] = ..., job_title: _Optional[str] = ..., interview_type: _Optional[str] = ..., question_count: _Optional[int] = ..., language: _Optional[str] = ...) -> None: ...

class StartMockResponse(_message.Message):
    __slots__ = ("success", "session_id", "total_questions")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    SESSION_ID_FIELD_NUMBER: _ClassVar[int]
    TOTAL_QUESTIONS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    session_id: str
    total_questions: int
    def __init__(self, success: bool = ..., session_id: _Optional[str] = ..., total_questions: _Optional[int] = ...) -> None: ...

class GetQuestionRequest(_message.Message):
    __slots__ = ("session_id", "question_index")
    SESSION_ID_FIELD_NUMBER: _ClassVar[int]
    QUESTION_INDEX_FIELD_NUMBER: _ClassVar[int]
    session_id: str
    question_index: int
    def __init__(self, session_id: _Optional[str] = ..., question_index: _Optional[int] = ...) -> None: ...

class QuestionResponse(_message.Message):
    __slots__ = ("question_index", "question", "category", "time_limit_seconds")
    QUESTION_INDEX_FIELD_NUMBER: _ClassVar[int]
    QUESTION_FIELD_NUMBER: _ClassVar[int]
    CATEGORY_FIELD_NUMBER: _ClassVar[int]
    TIME_LIMIT_SECONDS_FIELD_NUMBER: _ClassVar[int]
    question_index: int
    question: str
    category: str
    time_limit_seconds: int
    def __init__(self, question_index: _Optional[int] = ..., question: _Optional[str] = ..., category: _Optional[str] = ..., time_limit_seconds: _Optional[int] = ...) -> None: ...

class EvaluateAnswerRequest(_message.Message):
    __slots__ = ("session_id", "question_index", "question", "answer_text", "answer_audio_url")
    SESSION_ID_FIELD_NUMBER: _ClassVar[int]
    QUESTION_INDEX_FIELD_NUMBER: _ClassVar[int]
    QUESTION_FIELD_NUMBER: _ClassVar[int]
    ANSWER_TEXT_FIELD_NUMBER: _ClassVar[int]
    ANSWER_AUDIO_URL_FIELD_NUMBER: _ClassVar[int]
    session_id: str
    question_index: int
    question: str
    answer_text: str
    answer_audio_url: str
    def __init__(self, session_id: _Optional[str] = ..., question_index: _Optional[int] = ..., question: _Optional[str] = ..., answer_text: _Optional[str] = ..., answer_audio_url: _Optional[str] = ...) -> None: ...

class EvaluationResponse(_message.Message):
    __slots__ = ("score", "feedback", "strengths", "improvements")
    SCORE_FIELD_NUMBER: _ClassVar[int]
    FEEDBACK_FIELD_NUMBER: _ClassVar[int]
    STRENGTHS_FIELD_NUMBER: _ClassVar[int]
    IMPROVEMENTS_FIELD_NUMBER: _ClassVar[int]
    score: int
    feedback: str
    strengths: _containers.RepeatedScalarFieldContainer[str]
    improvements: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, score: _Optional[int] = ..., feedback: _Optional[str] = ..., strengths: _Optional[_Iterable[str]] = ..., improvements: _Optional[_Iterable[str]] = ...) -> None: ...

class FinishMockRequest(_message.Message):
    __slots__ = ("session_id",)
    SESSION_ID_FIELD_NUMBER: _ClassVar[int]
    session_id: str
    def __init__(self, session_id: _Optional[str] = ...) -> None: ...

class MockReportResponse(_message.Message):
    __slots__ = ("overall_score", "overall_feedback", "results", "recommendations")
    OVERALL_SCORE_FIELD_NUMBER: _ClassVar[int]
    OVERALL_FEEDBACK_FIELD_NUMBER: _ClassVar[int]
    RESULTS_FIELD_NUMBER: _ClassVar[int]
    RECOMMENDATIONS_FIELD_NUMBER: _ClassVar[int]
    overall_score: int
    overall_feedback: str
    results: _containers.RepeatedCompositeFieldContainer[QuestionResult]
    recommendations: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, overall_score: _Optional[int] = ..., overall_feedback: _Optional[str] = ..., results: _Optional[_Iterable[_Union[QuestionResult, _Mapping]]] = ..., recommendations: _Optional[_Iterable[str]] = ...) -> None: ...

class QuestionResult(_message.Message):
    __slots__ = ("index", "question", "answer", "score", "feedback")
    INDEX_FIELD_NUMBER: _ClassVar[int]
    QUESTION_FIELD_NUMBER: _ClassVar[int]
    ANSWER_FIELD_NUMBER: _ClassVar[int]
    SCORE_FIELD_NUMBER: _ClassVar[int]
    FEEDBACK_FIELD_NUMBER: _ClassVar[int]
    index: int
    question: str
    answer: str
    score: int
    feedback: str
    def __init__(self, index: _Optional[int] = ..., question: _Optional[str] = ..., answer: _Optional[str] = ..., score: _Optional[int] = ..., feedback: _Optional[str] = ...) -> None: ...

class GapAnalysisRequest(_message.Message):
    __slots__ = ("user_id", "goal_title", "target_date", "current_profile")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    GOAL_TITLE_FIELD_NUMBER: _ClassVar[int]
    TARGET_DATE_FIELD_NUMBER: _ClassVar[int]
    CURRENT_PROFILE_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    goal_title: str
    target_date: str
    current_profile: ProfileData
    def __init__(self, user_id: _Optional[str] = ..., goal_title: _Optional[str] = ..., target_date: _Optional[str] = ..., current_profile: _Optional[_Union[ProfileData, _Mapping]] = ...) -> None: ...

class GapAnalysisResponse(_message.Message):
    __slots__ = ("gaps", "recommendations", "readiness_score")
    GAPS_FIELD_NUMBER: _ClassVar[int]
    RECOMMENDATIONS_FIELD_NUMBER: _ClassVar[int]
    READINESS_SCORE_FIELD_NUMBER: _ClassVar[int]
    gaps: _containers.RepeatedCompositeFieldContainer[GapItem]
    recommendations: _containers.RepeatedScalarFieldContainer[str]
    readiness_score: int
    def __init__(self, gaps: _Optional[_Iterable[_Union[GapItem, _Mapping]]] = ..., recommendations: _Optional[_Iterable[str]] = ..., readiness_score: _Optional[int] = ...) -> None: ...

class GapItem(_message.Message):
    __slots__ = ("skill", "current_level", "required_level", "priority")
    SKILL_FIELD_NUMBER: _ClassVar[int]
    CURRENT_LEVEL_FIELD_NUMBER: _ClassVar[int]
    REQUIRED_LEVEL_FIELD_NUMBER: _ClassVar[int]
    PRIORITY_FIELD_NUMBER: _ClassVar[int]
    skill: str
    current_level: str
    required_level: str
    priority: int
    def __init__(self, skill: _Optional[str] = ..., current_level: _Optional[str] = ..., required_level: _Optional[str] = ..., priority: _Optional[int] = ...) -> None: ...

class LearningPathRequest(_message.Message):
    __slots__ = ("user_id", "goal_id", "gaps", "preferred_style")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    GOAL_ID_FIELD_NUMBER: _ClassVar[int]
    GAPS_FIELD_NUMBER: _ClassVar[int]
    PREFERRED_STYLE_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    goal_id: str
    gaps: _containers.RepeatedCompositeFieldContainer[GapItem]
    preferred_style: str
    def __init__(self, user_id: _Optional[str] = ..., goal_id: _Optional[str] = ..., gaps: _Optional[_Iterable[_Union[GapItem, _Mapping]]] = ..., preferred_style: _Optional[str] = ...) -> None: ...

class LearningPathChunk(_message.Message):
    __slots__ = ("phase", "content", "is_final")
    PHASE_FIELD_NUMBER: _ClassVar[int]
    CONTENT_FIELD_NUMBER: _ClassVar[int]
    IS_FINAL_FIELD_NUMBER: _ClassVar[int]
    phase: str
    content: str
    is_final: bool
    def __init__(self, phase: _Optional[str] = ..., content: _Optional[str] = ..., is_final: bool = ...) -> None: ...

class AnalyzeJobRequest(_message.Message):
    __slots__ = ("job_id", "job_title", "job_description", "company")
    JOB_ID_FIELD_NUMBER: _ClassVar[int]
    JOB_TITLE_FIELD_NUMBER: _ClassVar[int]
    JOB_DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    COMPANY_FIELD_NUMBER: _ClassVar[int]
    job_id: str
    job_title: str
    job_description: str
    company: str
    def __init__(self, job_id: _Optional[str] = ..., job_title: _Optional[str] = ..., job_description: _Optional[str] = ..., company: _Optional[str] = ...) -> None: ...

class JobAnalysisResponse(_message.Message):
    __slots__ = ("required_skills", "nice_to_have_skills", "experience_level", "salary_estimate", "interview_tips")
    REQUIRED_SKILLS_FIELD_NUMBER: _ClassVar[int]
    NICE_TO_HAVE_SKILLS_FIELD_NUMBER: _ClassVar[int]
    EXPERIENCE_LEVEL_FIELD_NUMBER: _ClassVar[int]
    SALARY_ESTIMATE_FIELD_NUMBER: _ClassVar[int]
    INTERVIEW_TIPS_FIELD_NUMBER: _ClassVar[int]
    required_skills: _containers.RepeatedScalarFieldContainer[str]
    nice_to_have_skills: _containers.RepeatedScalarFieldContainer[str]
    experience_level: str
    salary_estimate: str
    interview_tips: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, required_skills: _Optional[_Iterable[str]] = ..., nice_to_have_skills: _Optional[_Iterable[str]] = ..., experience_level: _Optional[str] = ..., salary_estimate: _Optional[str] = ..., interview_tips: _Optional[_Iterable[str]] = ...) -> None: ...
