"""
Interview Agent Data Models (v2 - Multi-Agent Architecture)
================================================================================

重新设计的数据模型，支持：
1. 按 Schema 顺序逐字段采集
2. 深度追问直到获取足够信息
3. 模块级别的结构化数据提取
4. 结合已有 Profile 智能提问
5. 三 Agent 协作架构 (Questioner, Analyzer, Summarizer)
"""

from dataclasses import dataclass, field, asdict
from enum import Enum
from typing import List, Dict, Optional, Any, Tuple
from datetime import datetime
import json


class ProfileModule(Enum):
    """Profile 模块，按采集顺序排列"""
    BASIC_INFO = "basic_info"       # 基本信息（姓名、headline、location）
    EDUCATION = "education"          # 教育背景
    EXPERIENCE = "experience"        # 工作经历
    PROJECT = "project"              # 项目经历
    SKILL = "skill"                  # 技能
    CERTIFICATION = "certification"  # 证书
    LANGUAGE = "language"            # 语言能力
    SUMMARY = "summary"              # 总结确认


# 每个模块需要采集的字段定义
MODULE_FIELDS = {
    ProfileModule.BASIC_INFO: ["headline", "location"],
    ProfileModule.EDUCATION: ["school", "degree", "field_of_study", "start_date", "end_date", "gpa", "activities"],
    ProfileModule.EXPERIENCE: ["company", "title", "location", "employment_type", "start_date", "end_date",
                               "is_current", "description", "achievements", "technologies"],
    ProfileModule.PROJECT: ["name", "description", "technologies", "start_date", "end_date", "highlights", "url"],
    ProfileModule.SKILL: ["name", "level", "years_of_experience"],
    ProfileModule.CERTIFICATION: ["name", "issuer", "issue_date", "credential_url"],
    ProfileModule.LANGUAGE: ["language", "proficiency"],
}

# 每个模块的必填字段（必须获取才能认为该条目完成）
REQUIRED_FIELDS = {
    ProfileModule.BASIC_INFO: ["headline"],
    ProfileModule.EDUCATION: ["school", "degree"],
    ProfileModule.EXPERIENCE: ["company", "title", "description"],
    ProfileModule.PROJECT: ["name", "description"],
    ProfileModule.SKILL: ["name", "level"],
    ProfileModule.CERTIFICATION: ["name", "issuer"],
    ProfileModule.LANGUAGE: ["language", "proficiency"],
}


@dataclass
class ModuleItem:
    """单个模块条目（如一段工作经历、一个项目）"""
    module: str                          # 所属模块
    fields: Dict[str, Any] = field(default_factory=dict)  # 已收集的字段
    is_complete: bool = False            # 是否完成
    follow_up_count: int = 0             # 追问次数

    def get_missing_required_fields(self) -> List[str]:
        """获取缺失的必填字段"""
        module_enum = ProfileModule(self.module)
        required = REQUIRED_FIELDS.get(module_enum, [])
        return [f for f in required if not self.fields.get(f)]

    def get_missing_optional_fields(self) -> List[str]:
        """获取缺失的可选字段"""
        module_enum = ProfileModule(self.module)
        all_fields = MODULE_FIELDS.get(module_enum, [])
        required = REQUIRED_FIELDS.get(module_enum, [])
        optional = [f for f in all_fields if f not in required]
        return [f for f in optional if not self.fields.get(f)]

    def is_sufficiently_detailed(self) -> bool:
        """判断是否有足够细节（用于决定是否继续追问）"""
        # 必填字段都有
        if self.get_missing_required_fields():
            return False

        # 工作经历需要有成就描述
        if self.module == "experience":
            achievements = self.fields.get("achievements", [])
            if not achievements or len(achievements) == 0:
                return False
            # 成就需要有量化数据
            if not any(self._has_quantified_data(a) for a in achievements):
                return False

        # 项目需要有亮点
        if self.module == "project":
            highlights = self.fields.get("highlights", [])
            if not highlights or len(highlights) == 0:
                return False

        return True

    def _has_quantified_data(self, text: str) -> bool:
        """检查文本是否包含量化数据"""
        import re
        # 检查是否有数字、百分比等
        patterns = [
            r'\d+%',           # 百分比
            r'\d+\s*(万|千|百|亿)',  # 中文数量
            r'\d+[kKmMbB]',    # 英文缩写
            r'\d+\s*(users?|用户|customers?|客户)',  # 用户数
            r'(提升|提高|增长|降低|减少|节省).*\d+',  # 变化量
            r'\d+\s*(times?|倍)',  # 倍数
        ]
        return any(re.search(p, text) for p in patterns)


@dataclass
class ModuleProgress:
    """模块进度跟踪"""
    module: str
    items: List[ModuleItem] = field(default_factory=list)  # 该模块的所有条目
    current_item_index: int = 0       # 当前正在采集的条目索引
    is_module_complete: bool = False  # 整个模块是否完成
    extracted_data: List[Dict] = field(default_factory=list)  # 已提取的结构化数据

    def get_current_item(self) -> Optional[ModuleItem]:
        """获取当前正在采集的条目"""
        if 0 <= self.current_item_index < len(self.items):
            return self.items[self.current_item_index]
        return None

    def start_new_item(self) -> ModuleItem:
        """开始采集新条目"""
        item = ModuleItem(module=self.module)
        self.items.append(item)
        self.current_item_index = len(self.items) - 1
        return item

    def complete_current_item(self, extracted: Dict):
        """完成当前条目并保存提取的数据"""
        if self.items:
            self.items[self.current_item_index].is_complete = True
            self.extracted_data.append(extracted)


@dataclass
class InterviewSessionState:
    """面试会话状态"""
    session_id: str
    user_id: str
    language: str = "zh"

    # 模块进度
    current_module: ProfileModule = ProfileModule.BASIC_INFO
    module_progress: Dict[str, ModuleProgress] = field(default_factory=dict)

    # 已有 Profile 数据（用于智能提问）
    existing_profile: Dict[str, Any] = field(default_factory=dict)

    # 对话历史
    conversation_history: List[Dict] = field(default_factory=list)

    # 当前采集的临时数据
    current_collection: Dict[str, Any] = field(default_factory=dict)

    # 状态
    status: str = "IN_PROGRESS"
    turn_count: int = 0
    current_item_turn_count: int = 0  # 当前条目的追问次数

    # 时间戳
    started_at: str = field(default_factory=lambda: datetime.now().isoformat())
    last_activity_at: str = field(default_factory=lambda: datetime.now().isoformat())

    def __post_init__(self):
        """初始化模块进度"""
        if not self.module_progress:
            for module in ProfileModule:
                self.module_progress[module.value] = ModuleProgress(module=module.value)

    def get_current_module_progress(self) -> ModuleProgress:
        """获取当前模块的进度"""
        return self.module_progress[self.current_module.value]

    def advance_to_next_module(self) -> bool:
        """进入下一个模块，返回是否还有更多模块"""
        modules = list(ProfileModule)
        current_idx = modules.index(self.current_module)

        # 标记当前模块完成
        self.module_progress[self.current_module.value].is_module_complete = True

        if current_idx < len(modules) - 1:
            self.current_module = modules[current_idx + 1]
            self.current_item_turn_count = 0
            return True
        return False

    def update_activity(self):
        """更新活动时间戳"""
        self.last_activity_at = datetime.now().isoformat()
        self.turn_count += 1
        self.current_item_turn_count += 1

    def get_all_extracted_data(self) -> Dict[str, List[Dict]]:
        """获取所有已提取的结构化数据"""
        result = {}
        for module_name, progress in self.module_progress.items():
            if progress.extracted_data:
                result[module_name] = progress.extracted_data
        return result

    def to_dict(self) -> dict:
        """转换为字典用于存储"""
        module_progress_dict = {}
        for k, v in self.module_progress.items():
            module_progress_dict[k] = {
                "module": v.module,
                "items": [asdict(item) for item in v.items],
                "current_item_index": v.current_item_index,
                "is_module_complete": v.is_module_complete,
                "extracted_data": v.extracted_data,
            }

        return {
            "session_id": self.session_id,
            "user_id": self.user_id,
            "language": self.language,
            "current_module": self.current_module.value,
            "module_progress": module_progress_dict,
            "existing_profile": self.existing_profile,
            "conversation_history": self.conversation_history,
            "current_collection": self.current_collection,
            "status": self.status,
            "turn_count": self.turn_count,
            "current_item_turn_count": self.current_item_turn_count,
            "started_at": self.started_at,
            "last_activity_at": self.last_activity_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "InterviewSessionState":
        """从字典创建"""
        state = cls(
            session_id=data["session_id"],
            user_id=data["user_id"],
            language=data.get("language", "zh"),
            current_module=ProfileModule(data.get("current_module", "basic_info")),
            existing_profile=data.get("existing_profile", {}),
            conversation_history=data.get("conversation_history", []),
            current_collection=data.get("current_collection", {}),
            status=data.get("status", "IN_PROGRESS"),
            turn_count=data.get("turn_count", 0),
            current_item_turn_count=data.get("current_item_turn_count", 0),
            started_at=data.get("started_at", ""),
            last_activity_at=data.get("last_activity_at", ""),
        )

        # 恢复模块进度
        module_progress_data = data.get("module_progress", {})
        for module_name, mp_data in module_progress_data.items():
            items = []
            for item_data in mp_data.get("items", []):
                items.append(ModuleItem(
                    module=item_data["module"],
                    fields=item_data.get("fields", {}),
                    is_complete=item_data.get("is_complete", False),
                    follow_up_count=item_data.get("follow_up_count", 0),
                ))

            state.module_progress[module_name] = ModuleProgress(
                module=mp_data["module"],
                items=items,
                current_item_index=mp_data.get("current_item_index", 0),
                is_module_complete=mp_data.get("is_module_complete", False),
                extracted_data=mp_data.get("extracted_data", []),
            )

        return state


# ==================== 提取的 Profile 类型 ====================

@dataclass
class EducationItem:
    """教育条目"""
    school: str = ""
    degree: str = ""
    field_of_study: str = ""
    start_date: str = ""
    end_date: str = ""
    gpa: Optional[str] = None
    description: str = ""
    activities: List[str] = field(default_factory=list)
    honors: List[str] = field(default_factory=list)


@dataclass
class WorkExperienceItem:
    """工作经历条目"""
    company: str = ""
    title: str = ""
    location: str = ""
    employment_type: str = "FULL_TIME"
    start_date: str = ""
    end_date: str = ""
    is_current: bool = False
    description: str = ""
    achievements: List[str] = field(default_factory=list)
    technologies: List[str] = field(default_factory=list)


@dataclass
class ProjectItem:
    """项目条目"""
    name: str = ""
    description: str = ""
    url: str = ""
    repo_url: str = ""
    technologies: List[str] = field(default_factory=list)
    start_date: str = ""
    end_date: str = ""
    highlights: List[str] = field(default_factory=list)


@dataclass
class SkillItem:
    """技能条目"""
    name: str = ""
    level: str = "INTERMEDIATE"
    category: str = ""


@dataclass
class CertificationItem:
    """证书条目"""
    name: str = ""
    issuer: str = ""
    issue_date: str = ""
    credential_url: str = ""


@dataclass
class LanguageItem:
    """语言条目"""
    language: str = ""
    proficiency: str = ""


@dataclass
class ExtractedProfile:
    """完整的提取 Profile"""
    headline: Optional[str] = None
    summary: Optional[str] = None
    location: Optional[str] = None
    education: List[Dict] = field(default_factory=list)
    experiences: List[Dict] = field(default_factory=list)
    projects: List[Dict] = field(default_factory=list)
    skills: List[Dict] = field(default_factory=list)
    certifications: List[Dict] = field(default_factory=list)
    languages: List[Dict] = field(default_factory=list)
    achievements: List[str] = field(default_factory=list)
    completeness_score: int = 0
    missing_sections: List[str] = field(default_factory=list)

    def to_dict(self) -> dict:
        return asdict(self)


# ==================== Multi-Agent Architecture Models ====================

@dataclass
class AnalysisResult:
    """
    AnalyzerAgent 的分析结果

    用于判断用户回答是否足够完整，以及需要追问的方向
    """
    is_sufficient: bool                    # 回答是否足够完整（可以进入下一模块）
    extracted_info: Dict[str, Any]         # 从回答中提取的信息
    missing_points: List[str]              # 还缺少的信息点（具体、可操作）
    follow_up_suggestions: List[str]       # 建议的追问方向
    reasoning: str                         # LLM的分析推理过程
    confidence_score: float = 0.0          # 置信度 0-1
    quality_issues: List[str] = field(default_factory=list)  # 质量问题

    def to_dict(self) -> dict:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict) -> "AnalysisResult":
        return cls(
            is_sufficient=data.get("is_sufficient", False),
            extracted_info=data.get("extracted_info", {}),
            missing_points=data.get("missing_points", []),
            follow_up_suggestions=data.get("follow_up_suggestions", []),
            reasoning=data.get("reasoning", ""),
            confidence_score=data.get("confidence_score", 0.0),
            quality_issues=data.get("quality_issues", []),
        )

    @classmethod
    def insufficient(cls, missing: List[str], suggestions: List[str], reasoning: str = "") -> "AnalysisResult":
        """创建一个表示回答不足的结果"""
        return cls(
            is_sufficient=False,
            extracted_info={},
            missing_points=missing,
            follow_up_suggestions=suggestions,
            reasoning=reasoning,
        )

    @classmethod
    def sufficient(cls, extracted: Dict[str, Any], reasoning: str = "") -> "AnalysisResult":
        """创建一个表示回答足够的结果"""
        return cls(
            is_sufficient=True,
            extracted_info=extracted,
            missing_points=[],
            follow_up_suggestions=[],
            reasoning=reasoning,
        )


@dataclass
class ModuleSummary:
    """
    SummarizerAgent 的模块总结结果

    包含该模块的完整结构化数据，可直接用于填充数据库
    """
    module: str                            # 模块名称
    structured_data: List[Dict]            # 符合Schema的结构化数据列表（可直接入库）
    completeness_score: int                # 完整度评分 0-100
    key_highlights: List[str]              # 关键亮点（供后续服务快速索引）
    data_quality_notes: List[str]          # 数据质量备注（如"成就缺少量化"）
    item_count: int = 0                    # 条目数量

    def to_dict(self) -> dict:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict) -> "ModuleSummary":
        return cls(
            module=data.get("module", ""),
            structured_data=data.get("structured_data", []),
            completeness_score=data.get("completeness_score", 0),
            key_highlights=data.get("key_highlights", []),
            data_quality_notes=data.get("data_quality_notes", []),
            item_count=data.get("item_count", 0),
        )

    @classmethod
    def empty(cls, module: str) -> "ModuleSummary":
        """创建一个空的模块总结"""
        return cls(
            module=module,
            structured_data=[],
            completeness_score=0,
            key_highlights=[],
            data_quality_notes=["No data collected"],
            item_count=0,
        )


@dataclass
class QuestionResult:
    """
    QuestionerAgent 的提问结果
    """
    question: str                          # 生成的问题
    question_type: str = "opening"         # 问题类型: opening / follow_up / confirmation
    context_reference: str = ""            # 引用的上下文（如用户之前的回答）
    target_fields: List[str] = field(default_factory=list)  # 目标字段

    def to_dict(self) -> dict:
        return asdict(self)


@dataclass
class OrchestratorState:
    """
    Orchestrator 的状态管理

    扩展的会话状态，用于协调三个Agent
    """
    session_id: str
    user_id: str
    language: str = "zh"

    # 模块进度
    current_module: ProfileModule = ProfileModule.BASIC_INFO
    module_order: List[str] = field(default_factory=lambda: [
        "basic_info", "education", "experience", "project",
        "skill", "certification", "language"
    ])

    # 已有数据
    existing_profile: Dict[str, Any] = field(default_factory=dict)

    # 各模块收集的信息
    module_collected_info: Dict[str, Dict] = field(default_factory=dict)  # 临时收集的信息
    module_summaries: Dict[str, ModuleSummary] = field(default_factory=dict)  # 总结后的结构化数据

    # 对话历史（按模块分组）
    conversation_history: List[Dict] = field(default_factory=list)
    module_conversations: Dict[str, List[Dict]] = field(default_factory=dict)

    # 当前问题追踪
    current_question: str = ""
    current_question_type: str = "opening"
    follow_up_count: int = 0              # 当前问题追问次数
    current_item_index: int = 0           # 当前条目索引（一个模块可能有多个条目）

    # 状态
    status: str = "IN_PROGRESS"
    turn_count: int = 0

    # 时间戳
    started_at: str = field(default_factory=lambda: datetime.now().isoformat())
    last_activity_at: str = field(default_factory=lambda: datetime.now().isoformat())

    def __post_init__(self):
        """初始化模块数据结构"""
        for module in self.module_order:
            if module not in self.module_collected_info:
                self.module_collected_info[module] = {}
            if module not in self.module_conversations:
                self.module_conversations[module] = []

    def get_current_module_name(self) -> str:
        """获取当前模块名称"""
        return self.current_module.value

    def get_current_collected_info(self) -> Dict:
        """获取当前模块已收集的信息"""
        return self.module_collected_info.get(self.current_module.value, {})

    def update_collected_info(self, new_info: Dict):
        """更新当前模块收集的信息"""
        current = self.module_collected_info.get(self.current_module.value, {})

        for key, value in new_info.items():
            if value:  # 只更新非空值
                if isinstance(value, list):
                    # 对于列表，合并而不是覆盖
                    existing = current.get(key, [])
                    if isinstance(existing, list):
                        current[key] = existing + value
                    else:
                        current[key] = value
                else:
                    current[key] = value

        self.module_collected_info[self.current_module.value] = current

    def add_to_conversation(self, role: str, content: str):
        """添加消息到对话历史"""
        message = {"role": role, "content": content}
        self.conversation_history.append(message)

        # 同时添加到当前模块的对话
        module_key = self.current_module.value
        if module_key not in self.module_conversations:
            self.module_conversations[module_key] = []
        self.module_conversations[module_key].append(message)

    def get_current_module_conversation(self) -> List[Dict]:
        """获取当前模块的对话历史"""
        return self.module_conversations.get(self.current_module.value, [])

    def get_recent_conversation(self, n: int = 6) -> List[Dict]:
        """获取最近n轮对话"""
        return self.conversation_history[-n:]

    def advance_to_next_module(self) -> bool:
        """
        进入下一个模块

        Returns:
            bool: 是否还有更多模块
        """
        modules = list(ProfileModule)
        current_idx = modules.index(self.current_module)

        if current_idx < len(modules) - 1:
            self.current_module = modules[current_idx + 1]
            self.follow_up_count = 0
            self.current_item_index = 0

            # 初始化新模块的数据结构
            module_key = self.current_module.value
            if module_key not in self.module_collected_info:
                self.module_collected_info[module_key] = {}
            if module_key not in self.module_conversations:
                self.module_conversations[module_key] = []

            return True
        return False

    def complete_current_module(self, summary: ModuleSummary):
        """完成当前模块"""
        self.module_summaries[self.current_module.value] = summary

    def update_activity(self):
        """更新活动时间戳"""
        self.last_activity_at = datetime.now().isoformat()
        self.turn_count += 1

    def is_all_modules_complete(self) -> bool:
        """检查是否所有模块都已完成"""
        return self.current_module == ProfileModule.SUMMARY

    def get_all_summaries(self) -> Dict[str, ModuleSummary]:
        """获取所有模块的总结"""
        return self.module_summaries

    def get_progress_info(self) -> Dict:
        """获取进度信息"""
        completed = len(self.module_summaries)
        total = len(self.module_order)
        return {
            "completed_modules": completed,
            "total_modules": total,
            "progress_percentage": int(completed / total * 100) if total > 0 else 0,
            "current_module": self.current_module.value,
        }

    def to_dict(self) -> dict:
        """转换为字典用于存储"""
        return {
            "session_id": self.session_id,
            "user_id": self.user_id,
            "language": self.language,
            "current_module": self.current_module.value,
            "module_order": self.module_order,
            "existing_profile": self.existing_profile,
            "module_collected_info": self.module_collected_info,
            "module_summaries": {k: v.to_dict() for k, v in self.module_summaries.items()},
            "conversation_history": self.conversation_history,
            "module_conversations": self.module_conversations,
            "current_question": self.current_question,
            "current_question_type": self.current_question_type,
            "follow_up_count": self.follow_up_count,
            "current_item_index": self.current_item_index,
            "status": self.status,
            "turn_count": self.turn_count,
            "started_at": self.started_at,
            "last_activity_at": self.last_activity_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "OrchestratorState":
        """从字典创建"""
        state = cls(
            session_id=data["session_id"],
            user_id=data["user_id"],
            language=data.get("language", "zh"),
            current_module=ProfileModule(data.get("current_module", "basic_info")),
            module_order=data.get("module_order", []),
            existing_profile=data.get("existing_profile", {}),
            module_collected_info=data.get("module_collected_info", {}),
            conversation_history=data.get("conversation_history", []),
            module_conversations=data.get("module_conversations", {}),
            current_question=data.get("current_question", ""),
            current_question_type=data.get("current_question_type", "opening"),
            follow_up_count=data.get("follow_up_count", 0),
            current_item_index=data.get("current_item_index", 0),
            status=data.get("status", "IN_PROGRESS"),
            turn_count=data.get("turn_count", 0),
            started_at=data.get("started_at", ""),
            last_activity_at=data.get("last_activity_at", ""),
        )

        # 恢复模块总结
        summaries_data = data.get("module_summaries", {})
        for module_name, summary_dict in summaries_data.items():
            state.module_summaries[module_name] = ModuleSummary.from_dict(summary_dict)

        return state


# ==================== Utility Functions ====================

def merge_extracted_info(existing: Dict, new: Dict) -> Dict:
    """
    合并提取的信息

    对于列表类型的字段，合并而不是覆盖
    对于其他类型，新值覆盖旧值
    """
    result = existing.copy()

    for key, value in new.items():
        if value is None:
            continue

        if isinstance(value, list):
            existing_list = result.get(key, [])
            if isinstance(existing_list, list):
                # 合并列表，去重
                combined = existing_list + [v for v in value if v not in existing_list]
                result[key] = combined
            else:
                result[key] = value
        else:
            result[key] = value

    return result


def calculate_module_completeness(collected: Dict, module_name: str) -> int:
    """
    计算模块完整度

    基于必填字段和可选字段的填充情况
    """
    from .schemas import get_module_schema

    schema = get_module_schema(module_name)
    if not schema:
        return 0

    required_fields = schema.get_required_fields()
    optional_fields = schema.get_optional_fields()

    # 必填字段权重更高
    required_filled = sum(1 for f in required_fields if collected.get(f))
    optional_filled = sum(1 for f in optional_fields if collected.get(f))

    required_weight = 0.7
    optional_weight = 0.3

    required_score = (required_filled / len(required_fields) * 100) if required_fields else 100
    optional_score = (optional_filled / len(optional_fields) * 100) if optional_fields else 100

    return int(required_score * required_weight + optional_score * optional_weight)
