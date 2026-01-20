"""Interview Agent Module (v2 - Multi-Agent Architecture)

Profile Interview Agent for deep background collection interviews.

Key features:
1. Schema-ordered collection: basic_info → education → experience → project → skill → certification → language
2. Deep follow-up: Each item must be fully collected before moving on
3. Module-level summarization: Extract structured data after completing each module
4. Smart questioning: Leverage existing profile data for targeted questions
5. Multi-Agent Architecture: Questioner, Analyzer, Summarizer with Orchestrator

Architecture:
                    ┌─────────────────────────────────────────────────────┐
                    │                 ProfileInterviewOrchestrator         │
                    │   (协调器：管理状态、调度Agent、处理流程控制)          │
                    └─────────────────────────────────────────────────────┘
                                            │
              ┌─────────────────────────────┼─────────────────────────────┐
              │                             │                             │
              ▼                             ▼                             ▼
    ┌──────────────────┐        ┌──────────────────┐        ┌──────────────────┐
    │  QuestionerAgent │        │  AnalyzerAgent   │        │  SummarizerAgent │
    │   (提问专家)      │◄──────►│   (分析专家)      │◄──────►│   (总结专家)     │
    └──────────────────┘        └──────────────────┘        └──────────────────┘
"""

# ==================== New Multi-Agent Implementation ====================

# Orchestrator (main entry point for new architecture)
from .orchestrator import ProfileInterviewOrchestrator

# Individual Agents
from .questioner_agent import QuestionerAgent
from .analyzer_agent import AnalyzerAgent
from .summarizer_agent import SummarizerAgent

# Schemas
from .schemas import (
    ModuleSchema,
    FieldDefinition,
    FieldType,
    MODULE_SCHEMAS,
    EXPERIENCE_SCHEMA,
    PROJECT_SCHEMA,
    EDUCATION_SCHEMA,
    SKILL_SCHEMA,
    CERTIFICATION_SCHEMA,
    LANGUAGE_SCHEMA,
    BASIC_INFO_SCHEMA,
    get_module_schema,
    get_quality_criteria,
    get_follow_up_triggers,
    get_required_fields,
    get_all_fields,
    get_schema_json_template,
)

# Models
from .models import (
    # Core enums and state
    ProfileModule,
    InterviewSessionState,
    ModuleProgress,
    ModuleItem,
    MODULE_FIELDS,
    REQUIRED_FIELDS,

    # Multi-Agent models
    AnalysisResult,
    ModuleSummary,
    QuestionResult,
    OrchestratorState,

    # Profile data types
    ExtractedProfile,
    EducationItem,
    WorkExperienceItem,
    ProjectItem,
    SkillItem,
    CertificationItem,
    LanguageItem,

    # Utility functions
    merge_extracted_info,
    calculate_module_completeness,
)

# Prompts
from .prompts import (
    get_welcome_message,
    get_first_question,
    get_controller_prompt,
    get_module_summary_prompt,
    get_module_name,
    get_module_opener,
    get_ask_more_items,
    get_schema_template,
    get_final_synthesis_prompt,
)

# ==================== Legacy Implementation (for backwards compatibility) ====================

# Legacy single-agent implementation
from .agent import ProfileInterviewAgent

# Legacy workflow
from .workflow import InterviewAgentWorkflow, InterviewContext


__all__ = [
    # ==================== New Multi-Agent Implementation ====================

    # Orchestrator (preferred entry point)
    "ProfileInterviewOrchestrator",

    # Individual Agents
    "QuestionerAgent",
    "AnalyzerAgent",
    "SummarizerAgent",

    # Schemas
    "ModuleSchema",
    "FieldDefinition",
    "FieldType",
    "MODULE_SCHEMAS",
    "EXPERIENCE_SCHEMA",
    "PROJECT_SCHEMA",
    "EDUCATION_SCHEMA",
    "SKILL_SCHEMA",
    "CERTIFICATION_SCHEMA",
    "LANGUAGE_SCHEMA",
    "BASIC_INFO_SCHEMA",
    "get_module_schema",
    "get_quality_criteria",
    "get_follow_up_triggers",
    "get_required_fields",
    "get_all_fields",
    "get_schema_json_template",

    # Models - Core
    "ProfileModule",
    "InterviewSessionState",
    "ModuleProgress",
    "ModuleItem",
    "MODULE_FIELDS",
    "REQUIRED_FIELDS",

    # Models - Multi-Agent
    "AnalysisResult",
    "ModuleSummary",
    "QuestionResult",
    "OrchestratorState",

    # Models - Profile data
    "ExtractedProfile",
    "EducationItem",
    "WorkExperienceItem",
    "ProjectItem",
    "SkillItem",
    "CertificationItem",
    "LanguageItem",

    # Utility functions
    "merge_extracted_info",
    "calculate_module_completeness",

    # Prompts
    "get_welcome_message",
    "get_first_question",
    "get_controller_prompt",
    "get_module_summary_prompt",
    "get_module_name",
    "get_module_opener",
    "get_ask_more_items",
    "get_schema_template",
    "get_final_synthesis_prompt",

    # ==================== Legacy Implementation ====================

    # Legacy agent (for backwards compatibility)
    "ProfileInterviewAgent",

    # Legacy workflow
    "InterviewAgentWorkflow",
    "InterviewContext",
]
