"""
Profile Interview Schemas
================================================================================

Detailed Schema definitions for each Profile module, designed to support:
1. Resume generation service - rich details for HC matching
2. Job matching service - comprehensive skill and experience data
3. Mock interview service - deep context for follow-up questions
4. Growth advice service - skill gap analysis

Design Principles:
- As rich as possible: Many fields for downstream services to find matching experiences
- Not mandatory to fill: AnalyzerAgent intelligently determines "enough or not"
- Match-oriented: Every field considers "how useful for job matching/resume generation"
"""

from dataclasses import dataclass, field
from typing import Dict, List, Any, Optional
from enum import Enum


class FieldType(Enum):
    """Field data types"""
    STRING = "string"
    TEXT = "text"
    DATE = "date"
    INT = "int"
    BOOLEAN = "boolean"
    ARRAY_STRING = "array[string]"
    ENUM = "enum"


@dataclass
class FieldDefinition:
    """Definition of a single field"""
    name: str
    field_type: FieldType
    required: bool = False
    description: str = ""
    enum_values: List[str] = field(default_factory=list)
    format: str = ""  # e.g., "YYYY-MM" for dates


@dataclass
class ModuleSchema:
    """Complete schema definition for a module"""
    module_name: str
    fields: Dict[str, FieldDefinition]
    quality_criteria: List[str]
    follow_up_triggers: List[Dict[str, str]]
    collection_strategy: str = ""

    def get_required_fields(self) -> List[str]:
        """Get list of required field names"""
        return [name for name, f in self.fields.items() if f.required]

    def get_optional_fields(self) -> List[str]:
        """Get list of optional field names"""
        return [name for name, f in self.fields.items() if not f.required]

    def to_prompt_description(self, language: str = "zh") -> str:
        """Generate prompt-friendly description of the schema"""
        lines = []
        for name, f in self.fields.items():
            required_mark = "*" if f.required else ""
            lines.append(f"- {name}{required_mark}: {f.description}")
        return "\n".join(lines)


# ==================== Experience Schema (Most Important) ====================

EXPERIENCE_SCHEMA = ModuleSchema(
    module_name="experience",
    fields={
        # Basic Information
        "company": FieldDefinition(
            name="company",
            field_type=FieldType.STRING,
            required=True,
            description="Company name"
        ),
        "company_type": FieldDefinition(
            name="company_type",
            field_type=FieldType.STRING,
            description="Company type: Big Tech / Unicorn / Startup / MNC / State-owned"
        ),
        "company_size": FieldDefinition(
            name="company_size",
            field_type=FieldType.STRING,
            description="Company size: e.g., 1000+ employees"
        ),
        "industry": FieldDefinition(
            name="industry",
            field_type=FieldType.STRING,
            description="Industry: Finance / E-commerce / Social / Gaming / Enterprise, etc."
        ),

        # Position Information
        "title": FieldDefinition(
            name="title",
            field_type=FieldType.STRING,
            required=True,
            description="Job title"
        ),
        "level": FieldDefinition(
            name="level",
            field_type=FieldType.STRING,
            description="Level: e.g., P6 / Senior Engineer / Tech Lead"
        ),
        "employment_type": FieldDefinition(
            name="employment_type",
            field_type=FieldType.ENUM,
            enum_values=["FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP"],
            description="Employment type"
        ),
        "location": FieldDefinition(
            name="location",
            field_type=FieldType.STRING,
            description="Work location"
        ),

        # Time
        "start_date": FieldDefinition(
            name="start_date",
            field_type=FieldType.DATE,
            required=True,
            format="YYYY-MM",
            description="Start date"
        ),
        "end_date": FieldDefinition(
            name="end_date",
            field_type=FieldType.DATE,
            format="YYYY-MM",
            description="End date"
        ),
        "is_current": FieldDefinition(
            name="is_current",
            field_type=FieldType.BOOLEAN,
            description="Is current position"
        ),
        "duration_months": FieldDefinition(
            name="duration_months",
            field_type=FieldType.INT,
            description="Duration in months"
        ),

        # Core Content - Most Important!
        "description": FieldDefinition(
            name="description",
            field_type=FieldType.TEXT,
            required=True,
            description="Job responsibilities, must be specific"
        ),
        "achievements": FieldDefinition(
            name="achievements",
            field_type=FieldType.ARRAY_STRING,
            description="List of achievements, must be quantified"
        ),
        "technologies": FieldDefinition(
            name="technologies",
            field_type=FieldType.ARRAY_STRING,
            description="Tech stack used"
        ),

        # Extended Information - For HC Matching
        "team_size": FieldDefinition(
            name="team_size",
            field_type=FieldType.STRING,
            description="Team size: e.g., led a team of 5"
        ),
        "report_to": FieldDefinition(
            name="report_to",
            field_type=FieldType.STRING,
            description="Reporting to: e.g., reported to CTO"
        ),
        "key_projects": FieldDefinition(
            name="key_projects",
            field_type=FieldType.ARRAY_STRING,
            description="Key projects owned"
        ),
        "business_impact": FieldDefinition(
            name="business_impact",
            field_type=FieldType.TEXT,
            description="Business impact: e.g., supported XX business line"
        ),
    },
    quality_criteria=[
        "Achievements must include quantified data (numbers, percentages, amounts)",
        "Tech stack must be specific to framework/tool level, not just 'backend development'",
        "Must describe YOUR specific responsibilities, not the team's responsibilities",
        "Must have business context: what does this project/system do",
        "If management role, must describe team size and management scope",
    ],
    follow_up_triggers=[
        {"condition": "achievements is empty or no quantified data", "action": "Ask for specific results and data"},
        {"condition": "technologies has fewer than 3 items", "action": "Ask for complete tech stack"},
        {"condition": "description is less than 50 characters", "action": "Ask for specific responsibilities"},
        {"condition": "no team_size and title contains 'lead/manager'", "action": "Ask for team size"},
    ],
    collection_strategy="Start with most recent job, collect comprehensive details before moving to next"
)


# ==================== Project Schema ====================

PROJECT_SCHEMA = ModuleSchema(
    module_name="project",
    fields={
        # Basic Information
        "name": FieldDefinition(
            name="name",
            field_type=FieldType.STRING,
            required=True,
            description="Project name"
        ),
        "project_type": FieldDefinition(
            name="project_type",
            field_type=FieldType.STRING,
            description="Project type: Work project / Open source / Personal / Competition"
        ),
        "role": FieldDefinition(
            name="role",
            field_type=FieldType.STRING,
            description="Your role: Core developer / Lead / Contributor"
        ),

        # Time
        "start_date": FieldDefinition(
            name="start_date",
            field_type=FieldType.DATE,
            format="YYYY-MM",
            description="Start date"
        ),
        "end_date": FieldDefinition(
            name="end_date",
            field_type=FieldType.DATE,
            format="YYYY-MM",
            description="End date"
        ),
        "is_current": FieldDefinition(
            name="is_current",
            field_type=FieldType.BOOLEAN,
            description="Is ongoing project"
        ),

        # Core Content
        "description": FieldDefinition(
            name="description",
            field_type=FieldType.TEXT,
            required=True,
            description="Project description: background + goal + your contribution"
        ),
        "your_contribution": FieldDefinition(
            name="your_contribution",
            field_type=FieldType.TEXT,
            description="Your specific contribution (distinct from team contribution)"
        ),
        "technologies": FieldDefinition(
            name="technologies",
            field_type=FieldType.ARRAY_STRING,
            description="Tech stack used"
        ),
        "highlights": FieldDefinition(
            name="highlights",
            field_type=FieldType.ARRAY_STRING,
            description="Technical highlights / innovations"
        ),

        # Extended Information
        "challenges": FieldDefinition(
            name="challenges",
            field_type=FieldType.TEXT,
            description="Technical challenges encountered"
        ),
        "solutions": FieldDefinition(
            name="solutions",
            field_type=FieldType.TEXT,
            description="How they were solved"
        ),
        "results": FieldDefinition(
            name="results",
            field_type=FieldType.TEXT,
            description="Project outcomes / business value"
        ),
        "team_size": FieldDefinition(
            name="team_size",
            field_type=FieldType.STRING,
            description="Project team size"
        ),

        # Links
        "url": FieldDefinition(
            name="url",
            field_type=FieldType.STRING,
            description="Project URL"
        ),
        "repo_url": FieldDefinition(
            name="repo_url",
            field_type=FieldType.STRING,
            description="Code repository URL"
        ),
    },
    quality_criteria=[
        "Must explain project background: why this project was done",
        "Must explain your specific contribution, not what the whole team did",
        "Technical highlights should be specific: what technology solved what problem",
        "Best to have quantified results: performance improved X%, supported X users",
    ],
    follow_up_triggers=[
        {"condition": "your_contribution is empty", "action": "Ask what you specifically worked on"},
        {"condition": "no technologies", "action": "Ask what technologies were used"},
        {"condition": "no challenges or solutions", "action": "Ask about technical challenges encountered"},
    ],
    collection_strategy="Ask about most proud/impactful project first, then others"
)


# ==================== Education Schema ====================

EDUCATION_SCHEMA = ModuleSchema(
    module_name="education",
    fields={
        "school": FieldDefinition(
            name="school",
            field_type=FieldType.STRING,
            required=True,
            description="School name"
        ),
        "degree": FieldDefinition(
            name="degree",
            field_type=FieldType.STRING,
            required=True,
            description="Degree: Bachelor / Master / PhD / Other"
        ),
        "field_of_study": FieldDefinition(
            name="field_of_study",
            field_type=FieldType.STRING,
            description="Major / Field of study"
        ),
        "location": FieldDefinition(
            name="location",
            field_type=FieldType.STRING,
            description="School location"
        ),

        "start_date": FieldDefinition(
            name="start_date",
            field_type=FieldType.DATE,
            format="YYYY-MM",
            description="Start date"
        ),
        "end_date": FieldDefinition(
            name="end_date",
            field_type=FieldType.DATE,
            format="YYYY-MM",
            description="End date / Expected graduation"
        ),
        "is_current": FieldDefinition(
            name="is_current",
            field_type=FieldType.BOOLEAN,
            description="Currently studying"
        ),

        "gpa": FieldDefinition(
            name="gpa",
            field_type=FieldType.STRING,
            description="GPA or ranking"
        ),
        "description": FieldDefinition(
            name="description",
            field_type=FieldType.TEXT,
            description="Additional description"
        ),
        "activities": FieldDefinition(
            name="activities",
            field_type=FieldType.ARRAY_STRING,
            description="Campus activities, honors, scholarships"
        ),

        # Extended
        "thesis_topic": FieldDefinition(
            name="thesis_topic",
            field_type=FieldType.STRING,
            description="Thesis topic / Research direction (for graduate students)"
        ),
        "relevant_courses": FieldDefinition(
            name="relevant_courses",
            field_type=FieldType.ARRAY_STRING,
            description="Relevant coursework"
        ),
        "honors": FieldDefinition(
            name="honors",
            field_type=FieldType.ARRAY_STRING,
            description="Honors and awards"
        ),
    },
    quality_criteria=[
        "School and degree are required",
        "If outstanding GPA or honors, should be recorded",
        "Graduate students should record research direction",
    ],
    follow_up_triggers=[
        {"condition": "degree is Master/PhD and no thesis_topic", "action": "Ask about research direction"},
        {"condition": "high GPA mentioned but not recorded", "action": "Ask for specific GPA"},
    ],
    collection_strategy="Start with highest degree, then work backwards"
)


# ==================== Skill Schema ====================

SKILL_SCHEMA = ModuleSchema(
    module_name="skill",
    fields={
        "name": FieldDefinition(
            name="name",
            field_type=FieldType.STRING,
            required=True,
            description="Skill name"
        ),
        "category": FieldDefinition(
            name="category",
            field_type=FieldType.STRING,
            description="Category: Programming Language / Framework / Database / Tool / Soft Skill"
        ),
        "level": FieldDefinition(
            name="level",
            field_type=FieldType.ENUM,
            required=True,
            enum_values=["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"],
            description="Proficiency level"
        ),
        "years_of_experience": FieldDefinition(
            name="years_of_experience",
            field_type=FieldType.INT,
            description="Years of experience"
        ),
        "last_used": FieldDefinition(
            name="last_used",
            field_type=FieldType.STRING,
            description="When last used"
        ),
        "context": FieldDefinition(
            name="context",
            field_type=FieldType.STRING,
            description="Context: which projects used this skill"
        ),
    },
    quality_criteria=[
        "Skills should be specific: Java, not just 'programming'",
        "Must differentiate proficiency levels",
        "Core skills should have years of experience and context",
    ],
    follow_up_triggers=[
        {"condition": "level not specified", "action": "Ask for proficiency level"},
        {"condition": "core skill without years_of_experience", "action": "Ask how long used"},
    ],
    collection_strategy="Ask about core tech stack first, then other tools/skills"
)


# ==================== Certification Schema ====================

CERTIFICATION_SCHEMA = ModuleSchema(
    module_name="certification",
    fields={
        "name": FieldDefinition(
            name="name",
            field_type=FieldType.STRING,
            required=True,
            description="Certification name"
        ),
        "issuer": FieldDefinition(
            name="issuer",
            field_type=FieldType.STRING,
            required=True,
            description="Issuing organization"
        ),
        "issue_date": FieldDefinition(
            name="issue_date",
            field_type=FieldType.DATE,
            format="YYYY-MM",
            description="Issue date"
        ),
        "expiration_date": FieldDefinition(
            name="expiration_date",
            field_type=FieldType.DATE,
            format="YYYY-MM",
            description="Expiration date"
        ),
        "credential_id": FieldDefinition(
            name="credential_id",
            field_type=FieldType.STRING,
            description="Credential ID"
        ),
        "credential_url": FieldDefinition(
            name="credential_url",
            field_type=FieldType.STRING,
            description="Verification URL"
        ),
    },
    quality_criteria=[
        "Certification name and issuer are required",
        "Include issue date when possible",
    ],
    follow_up_triggers=[],
    collection_strategy="Ask about most relevant/impressive certifications first"
)


# ==================== Language Schema ====================

LANGUAGE_SCHEMA = ModuleSchema(
    module_name="language",
    fields={
        "language": FieldDefinition(
            name="language",
            field_type=FieldType.STRING,
            required=True,
            description="Language name"
        ),
        "proficiency": FieldDefinition(
            name="proficiency",
            field_type=FieldType.ENUM,
            required=True,
            enum_values=["Native", "Fluent", "Professional", "Basic"],
            description="Proficiency level"
        ),
        "certification": FieldDefinition(
            name="certification",
            field_type=FieldType.STRING,
            description="Language certification e.g., TOEFL / IELTS"
        ),
    },
    quality_criteria=[
        "Language and proficiency are required",
        "Include certification if applicable",
    ],
    follow_up_triggers=[],
    collection_strategy="Ask about all languages spoken"
)


# ==================== Basic Info Schema ====================

BASIC_INFO_SCHEMA = ModuleSchema(
    module_name="basic_info",
    fields={
        "headline": FieldDefinition(
            name="headline",
            field_type=FieldType.STRING,
            description="Professional title: e.g., 'Senior Backend Engineer'"
        ),
        "summary": FieldDefinition(
            name="summary",
            field_type=FieldType.TEXT,
            description="Personal summary: 3-5 sentences summarizing background and strengths"
        ),
        "location": FieldDefinition(
            name="location",
            field_type=FieldType.STRING,
            description="Current location"
        ),
        "years_of_experience": FieldDefinition(
            name="years_of_experience",
            field_type=FieldType.INT,
            description="Total years of work experience"
        ),
        "current_status": FieldDefinition(
            name="current_status",
            field_type=FieldType.STRING,
            description="Current status: Employed / Unemployed / Student"
        ),
        "job_seeking_status": FieldDefinition(
            name="job_seeking_status",
            field_type=FieldType.STRING,
            description="Job seeking intent: Actively looking / Open to opportunities / Not looking"
        ),
    },
    quality_criteria=[
        "Headline should accurately describe professional identity",
        "Summary should be concise but comprehensive",
    ],
    follow_up_triggers=[],
    collection_strategy="Collect during initial conversation"
)


# ==================== Schema Registry ====================

MODULE_SCHEMAS: Dict[str, ModuleSchema] = {
    "basic_info": BASIC_INFO_SCHEMA,
    "education": EDUCATION_SCHEMA,
    "experience": EXPERIENCE_SCHEMA,
    "project": PROJECT_SCHEMA,
    "skill": SKILL_SCHEMA,
    "certification": CERTIFICATION_SCHEMA,
    "language": LANGUAGE_SCHEMA,
}


def get_module_schema(module_name: str) -> Optional[ModuleSchema]:
    """Get schema for a module"""
    return MODULE_SCHEMAS.get(module_name)


def get_quality_criteria(module_name: str) -> List[str]:
    """Get quality criteria for a module"""
    schema = MODULE_SCHEMAS.get(module_name)
    return schema.quality_criteria if schema else []


def get_follow_up_triggers(module_name: str) -> List[Dict[str, str]]:
    """Get follow-up triggers for a module"""
    schema = MODULE_SCHEMAS.get(module_name)
    return schema.follow_up_triggers if schema else []


def get_required_fields(module_name: str) -> List[str]:
    """Get required fields for a module"""
    schema = MODULE_SCHEMAS.get(module_name)
    return schema.get_required_fields() if schema else []


def get_all_fields(module_name: str) -> List[str]:
    """Get all fields for a module"""
    schema = MODULE_SCHEMAS.get(module_name)
    return list(schema.fields.keys()) if schema else []


# ==================== Schema Templates for LLM ====================

def get_schema_json_template(module_name: str, language: str = "zh") -> str:
    """Get JSON template for a module schema (for LLM extraction)"""
    templates = {
        "experience": """{
  "company": "Company name",
  "company_type": "Big Tech/Unicorn/Startup/MNC/State-owned",
  "company_size": "e.g., 1000+ employees",
  "industry": "Industry sector",
  "title": "Job title",
  "level": "Level/Grade",
  "employment_type": "FULL_TIME/PART_TIME/CONTRACT/INTERNSHIP",
  "location": "Location",
  "start_date": "YYYY-MM",
  "end_date": "YYYY-MM or null if current",
  "is_current": true/false,
  "description": "Detailed responsibilities",
  "achievements": ["Quantified achievement 1", "Quantified achievement 2"],
  "technologies": ["Tech 1", "Tech 2"],
  "team_size": "e.g., led team of 5",
  "key_projects": ["Project 1", "Project 2"],
  "business_impact": "Business impact description"
}""",
        "project": """{
  "name": "Project name",
  "project_type": "Work/Open Source/Personal/Competition",
  "role": "Your role",
  "start_date": "YYYY-MM",
  "end_date": "YYYY-MM",
  "is_current": true/false,
  "description": "Project description",
  "your_contribution": "Your specific contribution",
  "technologies": ["Tech 1", "Tech 2"],
  "highlights": ["Technical highlight 1", "Technical highlight 2"],
  "challenges": "Technical challenges",
  "solutions": "How solved",
  "results": "Outcomes/Business value",
  "team_size": "Team size",
  "url": "Project URL",
  "repo_url": "Repository URL"
}""",
        "education": """{
  "school": "School name",
  "degree": "Bachelor/Master/PhD",
  "field_of_study": "Major",
  "location": "Location",
  "start_date": "YYYY-MM",
  "end_date": "YYYY-MM",
  "is_current": true/false,
  "gpa": "GPA or ranking",
  "description": "Description",
  "activities": ["Activity 1", "Activity 2"],
  "thesis_topic": "Research direction (if applicable)",
  "relevant_courses": ["Course 1", "Course 2"],
  "honors": ["Honor 1", "Honor 2"]
}""",
        "skill": """{
  "name": "Skill name",
  "category": "Programming Language/Framework/Database/Tool/Soft Skill",
  "level": "BEGINNER/INTERMEDIATE/ADVANCED/EXPERT",
  "years_of_experience": number,
  "last_used": "When last used",
  "context": "Usage context"
}""",
        "certification": """{
  "name": "Certification name",
  "issuer": "Issuing organization",
  "issue_date": "YYYY-MM",
  "expiration_date": "YYYY-MM or null",
  "credential_id": "ID",
  "credential_url": "URL"
}""",
        "language": """{
  "language": "Language name",
  "proficiency": "Native/Fluent/Professional/Basic",
  "certification": "e.g., TOEFL 110"
}""",
        "basic_info": """{
  "headline": "Professional title",
  "summary": "Personal summary",
  "location": "Location",
  "years_of_experience": number,
  "current_status": "Employed/Unemployed/Student",
  "job_seeking_status": "Actively looking/Open/Not looking"
}""",
    }
    return templates.get(module_name, "{}")
