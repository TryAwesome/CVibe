"""
SummarizerAgent - Module Summarization Expert
================================================================================

Core responsibilities:
- Review complete module conversation history
- Generate structured data matching Profile Schema
- Merge with existing Profile data
- Produce data ready for database insertion

Design principles:
- Look at the ENTIRE module conversation, not just the last message
- Ensure Schema compatibility
- Identify key highlights for quick indexing
- Note data quality issues for review
"""

import json
import logging
from typing import Dict, List, Any, Optional

from .models import ModuleSummary, ProfileModule
from .schemas import (
    get_module_schema,
    get_schema_json_template,
    get_required_fields,
    ModuleSchema,
)

logger = logging.getLogger(__name__)


# ==================== Summarizer Prompts ====================

SUMMARIZER_PROMPT_ZH = """你是一位专业的数据提取专家，负责从面试对话中提取结构化数据。

## 模块
{module_name}

## 完整对话历史
{conversation_history}

## 已收集的原始信息
{collected_info}

## 已有的 Profile 数据（用于合并参考）
{existing_data}

## 目标 Schema 结构
{schema_template}

## 你的任务
1. **纵览完整对话**：仔细阅读所有对话内容
2. **提取所有信息**：不要遗漏任何用户提到的信息
3. **生成结构化数据**：按照 Schema 格式输出
4. **识别亮点**：标注关键成就和亮点
5. **质量评估**：指出数据质量问题

## 输出格式（严格JSON）
```json
{{
    "structured_data": [
        {schema_template}
    ],
    "completeness_score": 80,
    "key_highlights": ["关键亮点1", "关键亮点2"],
    "data_quality_notes": ["数据质量备注"]
}}
```

## 注意事项
1. structured_data 是数组，每个元素是一个完整的条目
2. 日期格式：YYYY-MM
3. 如果某字段未提及，设为 null 或空数组
4. completeness_score: 0-100，基于必填字段和可选字段的填充情况
5. key_highlights: 提取3-5个最突出的成就或亮点
6. data_quality_notes: 指出缺失或质量问题，如"成就缺少量化"

直接输出 JSON，不要添加其他文字。"""

SUMMARIZER_PROMPT_EN = """You are a professional data extraction expert, responsible for extracting structured data from interview conversations.

## Module
{module_name}

## Complete Conversation History
{conversation_history}

## Raw Collected Information
{collected_info}

## Existing Profile Data (for merge reference)
{existing_data}

## Target Schema Structure
{schema_template}

## Your Task
1. **Review Complete Conversation**: Carefully read all conversation content
2. **Extract All Information**: Don't miss any information mentioned by the user
3. **Generate Structured Data**: Output according to Schema format
4. **Identify Highlights**: Note key achievements and highlights
5. **Quality Assessment**: Point out data quality issues

## Output Format (Strict JSON)
```json
{{
    "structured_data": [
        {schema_template}
    ],
    "completeness_score": 80,
    "key_highlights": ["Key highlight 1", "Key highlight 2"],
    "data_quality_notes": ["Data quality note"]
}}
```

## Notes
1. structured_data is an array, each element is a complete entry
2. Date format: YYYY-MM
3. If a field is not mentioned, set to null or empty array
4. completeness_score: 0-100, based on required and optional field completion
5. key_highlights: Extract 3-5 most outstanding achievements or highlights
6. data_quality_notes: Point out missing or quality issues, e.g., "achievements lack quantification"

Output JSON directly, no other text."""


FINAL_SYNTHESIS_PROMPT_ZH = """基于完整的面试采集结果，生成最终的结构化 Profile。

## 已提取的模块数据
{extracted_modules}

## 完整对话记录
{full_conversation}

## 输出要求
整合所有模块数据，生成完整的 Profile JSON：

```json
{{
    "headline": "职业头衔（基于工作经历推断）",
    "summary": "个人简介（2-3句话，基于整体背景）",
    "location": "所在地",
    "years_of_experience": 5,
    "education": [...],
    "experiences": [...],
    "projects": [...],
    "skills": [...],
    "certifications": [...],
    "languages": [...],
    "achievements": ["突出成就1", "突出成就2"],
    "completeness_score": 80,
    "missing_sections": ["缺失的部分"]
}}
```

## 评分标准
- 有详细工作经历: +30
- 有教育背景: +20
- 有项目经历: +20
- 有技能列表: +15
- 有证书: +10
- 有语言: +5

直接输出 JSON，不要添加其他文字。"""

FINAL_SYNTHESIS_PROMPT_EN = """Based on complete interview collection results, generate the final structured Profile.

## Extracted Module Data
{extracted_modules}

## Complete Conversation
{full_conversation}

## Output Requirements
Integrate all module data to generate complete Profile JSON:

```json
{{
    "headline": "Professional title (inferred from work experience)",
    "summary": "Personal summary (2-3 sentences based on overall background)",
    "location": "Location",
    "years_of_experience": 5,
    "education": [...],
    "experiences": [...],
    "projects": [...],
    "skills": [...],
    "certifications": [...],
    "languages": [...],
    "achievements": ["Notable achievement 1", "Notable achievement 2"],
    "completeness_score": 80,
    "missing_sections": ["Missing sections"]
}}
```

## Scoring Criteria
- Has detailed work experience: +30
- Has education: +20
- Has projects: +20
- Has skills list: +15
- Has certifications: +10
- Has languages: +5

Output JSON directly, no other text."""


class SummarizerAgent:
    """
    SummarizerAgent - 总结专家

    核心职责：
    - 纵览该模块的完整对话历史
    - 参照 Profile Schema 结构
    - 生成符合格式要求的结构化数据
    - 该数据直接用于填充数据库
    """

    def __init__(self, llm_client):
        """
        初始化 SummarizerAgent

        Args:
            llm_client: LLM 客户端
        """
        self.llm = llm_client

    def summarize_module(
        self,
        module: ProfileModule,
        module_conversation: List[Dict],
        collected_info: Dict[str, Any],
        existing_data: List[Dict],
        language: str = "zh"
    ) -> ModuleSummary:
        """
        总结模块内容，生成结构化数据

        Args:
            module: 当前模块
            module_conversation: 该模块的完整对话
            collected_info: 已收集的原始信息
            existing_data: 已有的Profile数据（用于合并）
            language: 语言

        Returns:
            ModuleSummary: 模块总结结果
        """
        module_name = module.value

        # 构建 prompt
        prompt = self._build_prompt(
            module_name=module_name,
            conversation=module_conversation,
            collected_info=collected_info,
            existing_data=existing_data,
            language=language
        )

        try:
            messages = [
                {"role": "system", "content": self._get_system_prompt(language)},
                {"role": "user", "content": prompt}
            ]

            if hasattr(self.llm, 'chat'):
                response = self.llm.chat(messages, temperature=0.2, max_tokens=4096)
            else:
                response = self.llm.complete(prompt)

            # 解析响应
            result = self._parse_response(response, module_name)

            logger.info(f"Summarized {module_name}: {result.item_count} items, score={result.completeness_score}")
            return result

        except Exception as e:
            logger.error(f"Error in summarize_module: {e}", exc_info=True)
            # 返回基于已收集信息的基本总结
            return self._create_fallback_summary(module_name, collected_info)

    def synthesize_final_profile(
        self,
        module_summaries: Dict[str, ModuleSummary],
        full_conversation: List[Dict],
        language: str = "zh"
    ) -> Dict[str, Any]:
        """
        合成最终 Profile

        Args:
            module_summaries: 所有模块的总结
            full_conversation: 完整对话历史
            language: 语言

        Returns:
            Dict: 完整的 Profile 数据
        """
        # 整理所有模块数据
        extracted_modules = {}
        for module_name, summary in module_summaries.items():
            extracted_modules[module_name] = summary.structured_data

        # 构建 prompt
        template = FINAL_SYNTHESIS_PROMPT_ZH if language == "zh" else FINAL_SYNTHESIS_PROMPT_EN

        extracted_str = json.dumps(extracted_modules, ensure_ascii=False, indent=2)
        conv_str = self._format_conversation(full_conversation)

        prompt = template.format(
            extracted_modules=extracted_str,
            full_conversation=conv_str[-5000:]  # 限制长度
        )

        try:
            messages = [
                {"role": "system", "content": "You are a profile synthesis expert."},
                {"role": "user", "content": prompt}
            ]

            if hasattr(self.llm, 'chat'):
                response = self.llm.chat(messages, temperature=0.1, max_tokens=4096)
            else:
                response = self.llm.complete(prompt)

            # 解析响应
            profile = self._parse_json_response(response)

            # 确保必要字段
            profile = self._ensure_profile_fields(profile, extracted_modules)

            logger.info(f"Synthesized profile: completeness={profile.get('completeness_score', 0)}")
            return profile

        except Exception as e:
            logger.error(f"Error in synthesize_final_profile: {e}", exc_info=True)
            # 回退：直接使用已提取的数据
            return self._create_fallback_profile(extracted_modules)

    # ==================== Private Methods ====================

    def _build_prompt(
        self,
        module_name: str,
        conversation: List[Dict],
        collected_info: Dict,
        existing_data: List,
        language: str
    ) -> str:
        """构建总结 prompt"""
        template = SUMMARIZER_PROMPT_ZH if language == "zh" else SUMMARIZER_PROMPT_EN

        # 格式化对话历史
        conv_text = self._format_conversation(conversation)

        # 格式化已收集信息
        collected_text = json.dumps(collected_info, ensure_ascii=False, indent=2) if collected_info else "{}"

        # 格式化已有数据
        existing_text = json.dumps(existing_data, ensure_ascii=False, indent=2) if existing_data else "[]"

        # 获取 schema 模板
        schema_template = get_schema_json_template(module_name, language)

        return template.format(
            module_name=module_name,
            conversation_history=conv_text,
            collected_info=collected_text,
            existing_data=existing_text,
            schema_template=schema_template
        )

    def _format_conversation(self, conversation: List[Dict]) -> str:
        """格式化对话历史"""
        lines = []
        for msg in conversation:
            role = "User" if msg.get("role") == "user" else "Advisor"
            content = msg.get("content", "")
            lines.append(f"{role}: {content}")
        return "\n\n".join(lines)

    def _get_system_prompt(self, language: str) -> str:
        """获取系统 prompt"""
        if language == "zh":
            return """你是专业的数据提取专家。你的任务是：
1. 从对话中准确提取结构化数据
2. 确保数据符合 Schema 格式
3. 识别关键亮点和质量问题
4. 严格按照 JSON 格式输出"""
        else:
            return """You are a professional data extraction expert. Your task is:
1. Accurately extract structured data from conversations
2. Ensure data matches Schema format
3. Identify key highlights and quality issues
4. Output strictly in JSON format"""

    def _parse_response(self, response: str, module_name: str) -> ModuleSummary:
        """解析 LLM 响应"""
        data = self._parse_json_response(response)

        structured_data = data.get("structured_data", [])
        if not isinstance(structured_data, list):
            structured_data = [structured_data] if structured_data else []

        return ModuleSummary(
            module=module_name,
            structured_data=structured_data,
            completeness_score=data.get("completeness_score", 50),
            key_highlights=data.get("key_highlights", []),
            data_quality_notes=data.get("data_quality_notes", []),
            item_count=len(structured_data)
        )

    def _parse_json_response(self, response: str) -> Dict:
        """从响应中解析 JSON"""
        try:
            # 提取 JSON
            if "```json" in response:
                json_str = response.split("```json")[1].split("```")[0]
            elif "```" in response:
                json_str = response.split("```")[1].split("```")[0]
            else:
                start = response.find("{")
                end = response.rfind("}") + 1
                if start >= 0 and end > start:
                    json_str = response[start:end]
                else:
                    # 尝试查找数组
                    start = response.find("[")
                    end = response.rfind("]") + 1
                    if start >= 0 and end > start:
                        json_str = response[start:end]
                        return {"structured_data": json.loads(json_str)}
                    else:
                        json_str = response

            return json.loads(json_str.strip())

        except json.JSONDecodeError as e:
            logger.warning(f"Failed to parse summarizer response: {e}")
            return {}

    def _create_fallback_summary(self, module_name: str, collected_info: Dict) -> ModuleSummary:
        """创建备选总结"""
        # 直接使用已收集的信息
        structured_data = [collected_info] if collected_info else []

        return ModuleSummary(
            module=module_name,
            structured_data=structured_data,
            completeness_score=30,
            key_highlights=[],
            data_quality_notes=["Fallback summary - LLM extraction failed"],
            item_count=len(structured_data)
        )

    def _ensure_profile_fields(self, profile: Dict, extracted_modules: Dict) -> Dict:
        """确保 Profile 包含必要字段"""
        # 设置默认值
        profile.setdefault("completeness_score", 50)
        profile.setdefault("missing_sections", [])
        profile.setdefault("headline", "")
        profile.setdefault("summary", "")
        profile.setdefault("location", "")
        profile.setdefault("years_of_experience", 0)

        # 从提取的模块数据填充
        profile.setdefault("education", extracted_modules.get("education", []))
        profile.setdefault("experiences", extracted_modules.get("experience", []))
        profile.setdefault("projects", extracted_modules.get("project", []))
        profile.setdefault("skills", extracted_modules.get("skill", []))
        profile.setdefault("certifications", extracted_modules.get("certification", []))
        profile.setdefault("languages", extracted_modules.get("language", []))

        # 提取成就
        if not profile.get("achievements"):
            achievements = []
            for exp in profile.get("experiences", []):
                if isinstance(exp, dict):
                    achievements.extend(exp.get("achievements", [])[:2])
            profile["achievements"] = achievements[:5]

        return profile

    def _create_fallback_profile(self, extracted_modules: Dict) -> Dict:
        """创建备选 Profile"""
        return {
            "headline": "",
            "summary": "",
            "location": "",
            "years_of_experience": 0,
            "education": extracted_modules.get("education", []),
            "experiences": extracted_modules.get("experience", []),
            "projects": extracted_modules.get("project", []),
            "skills": extracted_modules.get("skill", []),
            "certifications": extracted_modules.get("certification", []),
            "languages": extracted_modules.get("language", []),
            "achievements": [],
            "completeness_score": 40,
            "missing_sections": ["synthesis_failed"]
        }

    def extract_single_item(
        self,
        module: ProfileModule,
        conversation_excerpt: List[Dict],
        collected_fields: Dict[str, Any],
        language: str = "zh"
    ) -> Dict[str, Any]:
        """
        提取单个条目的结构化数据

        用于在模块内处理多个条目（如多段工作经历）

        Args:
            module: 模块
            conversation_excerpt: 相关对话片段
            collected_fields: 已收集的字段
            language: 语言

        Returns:
            Dict: 结构化的条目数据
        """
        module_name = module.value

        # 简单模块直接返回已收集的字段
        if module_name in ["skill", "certification", "language"]:
            return self._normalize_simple_item(module_name, collected_fields)

        # 复杂模块使用 LLM 提取
        schema_template = get_schema_json_template(module_name, language)

        if language == "zh":
            prompt = f"""从以下对话中提取{module_name}的结构化数据。

## 对话
{self._format_conversation(conversation_excerpt)}

## 已收集的字段
{json.dumps(collected_fields, ensure_ascii=False, indent=2)}

## 目标格式
{schema_template}

直接输出 JSON 对象，不要添加其他文字。"""
        else:
            prompt = f"""Extract structured data for {module_name} from the following conversation.

## Conversation
{self._format_conversation(conversation_excerpt)}

## Collected Fields
{json.dumps(collected_fields, ensure_ascii=False, indent=2)}

## Target Format
{schema_template}

Output JSON object directly, no other text."""

        try:
            messages = [
                {"role": "system", "content": "You are a data extraction expert."},
                {"role": "user", "content": prompt}
            ]

            if hasattr(self.llm, 'chat'):
                response = self.llm.chat(messages, temperature=0.1)
            else:
                response = self.llm.complete(prompt)

            # 解析结果
            extracted = self._parse_json_response(response)

            # 合并已收集的字段
            result = collected_fields.copy()
            result.update(extracted)

            return result

        except Exception as e:
            logger.error(f"Error extracting single item: {e}")
            return collected_fields

    def _normalize_simple_item(self, module_name: str, fields: Dict) -> Dict:
        """标准化简单模块的条目数据"""
        if module_name == "skill":
            return {
                "name": fields.get("name", ""),
                "level": fields.get("level", "INTERMEDIATE"),
                "category": fields.get("category", ""),
                "years_of_experience": fields.get("years_of_experience"),
                "context": fields.get("context", ""),
            }
        elif module_name == "certification":
            return {
                "name": fields.get("name", ""),
                "issuer": fields.get("issuer", ""),
                "issue_date": fields.get("issue_date", ""),
                "credential_url": fields.get("credential_url", ""),
            }
        elif module_name == "language":
            return {
                "language": fields.get("language", ""),
                "proficiency": fields.get("proficiency", ""),
                "certification": fields.get("certification", ""),
            }
        else:
            return fields
