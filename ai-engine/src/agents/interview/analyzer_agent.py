"""
AnalyzerAgent - Response Analysis Expert
================================================================================

Core responsibilities:
- Analyze user responses against module schema requirements
- Determine if responses are complete enough for downstream services
- Extract structured information from responses
- Identify missing points that need follow-up

Design principles:
- Focus on quality criteria defined in schemas
- Consider downstream service needs (resume generation, job matching)
- Provide actionable follow-up suggestions
"""

import json
import logging
import re
from typing import Dict, List, Any, Optional

from .models import AnalysisResult, ProfileModule
from .schemas import (
    get_module_schema,
    get_quality_criteria,
    get_follow_up_triggers,
    get_required_fields,
    ModuleSchema,
)

logger = logging.getLogger(__name__)


# ==================== Analyzer Prompts ====================

ANALYZER_PROMPT_ZH = """你是一位专业的信息分析专家，正在评估用户对面试问题的回答质量。

## 当前模块
{module_name}

## 当前问题
{current_question}

## 用户回答
{user_response}

## 已收集的信息
{collected_info}

## 模块质量标准
{quality_criteria}

## 模块必填字段
{required_fields}

## 你的任务
1. **提取信息**：从用户回答中提取所有相关信息
2. **评估完整性**：判断回答是否满足后续服务需求（简历生成、职位匹配）
3. **识别缺失**：找出还需要追问的关键信息点
4. **给出建议**：建议下一步追问方向

## 评判标准（基于后续服务需求）
1. **简历生成需求**：
   - 工作经历必须有量化成就（数字、百分比）
   - 项目必须说明具体贡献和技术亮点
   - 技能必须区分熟练度级别

2. **职位匹配需求**：
   - 技术栈要具体到框架/工具级别
   - 行业经验要明确
   - 团队规模、项目规模要有

3. **信息完整性**：
   - 每段经历的时间线要清晰
   - 职责和成就要分开描述
   - 技术难点和解决方案要有

## 输出格式（严格JSON）
```json
{{
    "extracted_info": {{
        "field_name": "提取的值",
        "achievements": ["成就1", "成就2"]
    }},
    "is_sufficient": false,
    "missing_points": ["缺少量化数据", "技术栈不完整"],
    "follow_up_suggestions": ["追问具体数据", "询问使用的技术"],
    "quality_issues": ["成就描述过于笼统"],
    "confidence_score": 0.7,
    "reasoning": "用户提供了基本信息，但缺少量化成就和具体技术栈"
}}
```

**注意**：
- extracted_info 只包含从本轮回答中新提取的字段
- is_sufficient 为 true 表示该条目信息足够，可以进入下一步
- missing_points 要具体、可操作
- 直接输出 JSON，不要添加其他文字"""

ANALYZER_PROMPT_EN = """You are a professional career advisor, evaluating if the user has shared enough about their background.

## Current Module
{module_name}

## Current Question
{current_question}

## User Response
{user_response}

## Collected Information
{collected_info}

## Required Fields
{required_fields}

## Your Task
1. **Extract Information**: Extract relevant information from the user's response
2. **Evaluate Completeness**: Is there enough context to understand this experience/item?
3. **Identify Gaps**: Only flag truly important missing info (not nice-to-haves)

## Evaluation Guidelines
- Focus on understanding the STORY, not auditing for exact numbers
- Accept approximate descriptions ("several years", "small team", "significant improvement")
- Don't require exact percentages or team sizes - estimates are fine
- If user describes their role and impact clearly, that's sufficient
- Prioritize: What did they do? What was the outcome? What skills were used?

## When to mark as SUFFICIENT (is_sufficient = true):
- User explained their role/responsibility
- User mentioned key technologies or skills used
- User gave some sense of impact or outcome (even without exact numbers)
- User has answered 2+ follow-up questions on this topic

## When to ask follow-up:
- Core role/responsibility is unclear
- No technologies or skills mentioned at all
- User gave very brief response (< 30 words)

## Output Format (Strict JSON)
```json
{{
    "extracted_info": {{
        "field_name": "extracted value"
    }},
    "is_sufficient": true,
    "missing_points": [],
    "follow_up_suggestions": [],
    "quality_issues": [],
    "confidence_score": 0.8,
    "reasoning": "User provided clear description of role and impact"
}}
```

**Important**: Be generous. If the user has shared a reasonable amount of detail, mark as sufficient and move on. Don't interrogate for exact metrics."""


class AnalyzerAgent:
    """
    AnalyzerAgent - 分析专家

    核心职责：
    - 分析用户回答是否满足后续服务的信息需求
    - 判断回答是否足够全面、细致
    - 提取回答中的有效信息
    - 精准指出需要深究的点
    """

    # 最小追问轮次（不同模块不同）- 降低要求
    MIN_FOLLOW_UPS = {
        "basic_info": 0,
        "education": 1,
        "experience": 1,  # 降低：不再强制追问3次
        "project": 1,
        "skill": 0,
        "certification": 0,
        "language": 0,
    }

    # 最大追问轮次 - 防止无限追问
    MAX_FOLLOW_UPS = {
        "basic_info": 2,
        "education": 3,
        "experience": 4,  # 最多追问4次
        "project": 3,
        "skill": 2,
        "certification": 2,
        "language": 2,
    }

    def __init__(self, llm_client):
        """
        初始化 AnalyzerAgent

        Args:
            llm_client: LLM 客户端
        """
        self.llm = llm_client

    def analyze_response(
        self,
        module: ProfileModule,
        current_question: str,
        user_response: str,
        collected_info: Dict[str, Any],
        follow_up_count: int = 0,
        language: str = "zh"
    ) -> AnalysisResult:
        """
        分析用户回答

        Args:
            module: 当前模块
            current_question: 当前问题
            user_response: 用户回答
            collected_info: 已收集的信息
            follow_up_count: 已追问次数
            language: 语言

        Returns:
            AnalysisResult: 分析结果
        """
        module_name = module.value
        schema = get_module_schema(module_name)

        # 构建 prompt
        prompt = self._build_prompt(
            module_name=module_name,
            current_question=current_question,
            user_response=user_response,
            collected_info=collected_info,
            schema=schema,
            language=language
        )

        try:
            messages = [
                {"role": "system", "content": self._get_system_prompt(language)},
                {"role": "user", "content": prompt}
            ]

            # Use streaming for slow models like DeepSeek-R1
            if hasattr(self.llm, 'stream_chat'):
                response_parts = []
                for chunk in self.llm.stream_chat(messages, temperature=0.3):
                    response_parts.append(chunk)
                response = "".join(response_parts)
            elif hasattr(self.llm, 'chat'):
                response = self.llm.chat(messages, temperature=0.3)
            else:
                response = self.llm.complete(prompt)

            # Parse response (handles DeepSeek-R1 <think> tags)
            result = self._parse_response(response)

            # 应用额外的规则检查
            result = self._apply_rule_checks(
                result=result,
                module_name=module_name,
                collected_info=collected_info,
                follow_up_count=follow_up_count,
                user_response=user_response
            )

            logger.debug(f"Analysis result for {module_name}: sufficient={result.is_sufficient}")
            return result

        except Exception as e:
            logger.error(f"Error in analyze_response: {e}", exc_info=True)
            # 返回保守的结果
            return AnalysisResult.insufficient(
                missing=["Unable to analyze response"],
                suggestions=["Please provide more details"],
                reasoning=f"Analysis error: {str(e)}"
            )

    def _build_prompt(
        self,
        module_name: str,
        current_question: str,
        user_response: str,
        collected_info: Dict,
        schema: Optional[ModuleSchema],
        language: str
    ) -> str:
        """构建分析 prompt"""
        template = ANALYZER_PROMPT_ZH if language == "zh" else ANALYZER_PROMPT_EN

        # 格式化质量标准
        quality_criteria = get_quality_criteria(module_name)
        criteria_text = "\n".join(f"- {c}" for c in quality_criteria)

        # 格式化必填字段
        required = get_required_fields(module_name)
        required_text = ", ".join(required) if required else "None"

        # 格式化已收集信息
        collected_text = json.dumps(collected_info, ensure_ascii=False, indent=2) if collected_info else "{}"

        return template.format(
            module_name=module_name,
            current_question=current_question,
            user_response=user_response,
            collected_info=collected_text,
            quality_criteria=criteria_text if criteria_text else "None specified",
            required_fields=required_text
        )

    def _get_system_prompt(self, language: str) -> str:
        """获取系统 prompt"""
        if language == "zh":
            return """你是专业的信息分析专家。你的任务是：
1. 从用户回答中准确提取信息
2. 判断信息是否足够完整
3. 识别需要追问的点
4. 严格按照 JSON 格式输出"""
        else:
            return """You are a professional information analysis expert. Your task is:
1. Accurately extract information from user responses
2. Determine if information is complete enough
3. Identify points that need follow-up
4. Output strictly in JSON format"""

    def _parse_response(self, response: str) -> AnalysisResult:
        """解析 LLM 响应 (handles DeepSeek-R1 thinking tags)"""
        try:
            # First, remove <think>...</think> blocks from DeepSeek-R1
            cleaned_response = re.sub(r'<think>.*?</think>', '', response, flags=re.DOTALL)

            # 提取 JSON
            if "```json" in cleaned_response:
                json_str = cleaned_response.split("```json")[1].split("```")[0]
            elif "```" in cleaned_response:
                json_str = cleaned_response.split("```")[1].split("```")[0]
            else:
                start = cleaned_response.find("{")
                end = cleaned_response.rfind("}") + 1
                if start >= 0 and end > start:
                    json_str = cleaned_response[start:end]
                else:
                    json_str = cleaned_response

            data = json.loads(json_str.strip())

            return AnalysisResult(
                is_sufficient=data.get("is_sufficient", False),
                extracted_info=data.get("extracted_info", {}),
                missing_points=data.get("missing_points", []),
                follow_up_suggestions=data.get("follow_up_suggestions", []),
                reasoning=data.get("reasoning", ""),
                confidence_score=data.get("confidence_score", 0.5),
                quality_issues=data.get("quality_issues", [])
            )

        except json.JSONDecodeError as e:
            logger.warning(f"Failed to parse analyzer response as JSON: {e}")
            return AnalysisResult.insufficient(
                missing=["Parse error"],
                suggestions=["Please provide more details"],
                reasoning=f"Response parsing failed: {response[:200]}"
            )

    def _apply_rule_checks(
        self,
        result: AnalysisResult,
        module_name: str,
        collected_info: Dict,
        follow_up_count: int,
        user_response: str
    ) -> AnalysisResult:
        """
        应用规则检查

        基于硬编码规则调整分析结果
        """
        min_follow_ups = self.MIN_FOLLOW_UPS.get(module_name, 0)
        max_follow_ups = self.MAX_FOLLOW_UPS.get(module_name, 3)

        # 如果已达到最大追问次数，强制标记为足够
        if follow_up_count >= max_follow_ups:
            result.is_sufficient = True
            result.reasoning += f" (Max follow-ups reached: {max_follow_ups})"
            return result

        # 如果追问次数不够，即使 LLM 认为足够也要继续追问
        if follow_up_count < min_follow_ups:
            if result.is_sufficient:
                result.is_sufficient = False
                if not result.missing_points:
                    result.missing_points = ["Need more details for comprehensive profile"]
                if not result.follow_up_suggestions:
                    result.follow_up_suggestions = ["Ask for additional details"]

        # 特定模块的额外检查 - 仅在追问次数少于一半最大值时才严格检查
        merged_info = {**collected_info, **result.extracted_info}

        if follow_up_count < max_follow_ups // 2:
            if module_name == "experience":
                result = self._check_experience_quality(result, merged_info)
            elif module_name == "project":
                result = self._check_project_quality(result, merged_info)
            elif module_name == "education":
                result = self._check_education_quality(result, merged_info)

        # 检查用户是否明确表示没有更多信息
        if self._user_indicates_no_more(user_response):
            # 如果必填字段都有了，可以放宽标准
            required = get_required_fields(module_name)
            has_required = all(merged_info.get(f) for f in required)
            if has_required or follow_up_count >= 1:
                result.is_sufficient = True
                result.reasoning += " (User indicated no more information available)"

        return result

    def _check_experience_quality(self, result: AnalysisResult, info: Dict) -> AnalysisResult:
        """检查工作经历质量 - 宽松版本"""
        # 只检查最基本的必填项：公司名和职位
        if not info.get("company") and not info.get("title"):
            result.is_sufficient = False
            if not info.get("company"):
                result.missing_points.append("Company or organization name")
            if not info.get("title"):
                result.missing_points.append("Job title or role")
            result.follow_up_suggestions.append("Ask about the company and role")

        # 不再强制要求量化数据、团队人数等
        return result

    def _check_project_quality(self, result: AnalysisResult, info: Dict) -> AnalysisResult:
        """检查项目质量 - 宽松版本"""
        # 只检查是否有项目名称
        if not info.get("name") and not info.get("description"):
            result.is_sufficient = False
            result.missing_points.append("Project name or description")
            result.follow_up_suggestions.append("Ask about the project")

        # 不再强制要求技术亮点、具体贡献等
        return result

    def _check_education_quality(self, result: AnalysisResult, info: Dict) -> AnalysisResult:
        """检查教育经历质量"""
        # 教育经历相对简单，主要检查必填字段
        if not info.get("school") or not info.get("degree"):
            result.is_sufficient = False
            if not info.get("school"):
                result.missing_points.append("School name")
            if not info.get("degree"):
                result.missing_points.append("Degree type")

        return result

    def _has_quantified_data(self, text: str) -> bool:
        """检查文本是否包含量化数据"""
        patterns = [
            r'\d+%',           # 百分比
            r'\d+\s*(万|千|百|亿)',  # 中文数量
            r'\d+[kKmMbB]',    # 英文缩写
            r'\d+\s*(users?|用户|customers?|客户)',  # 用户数
            r'(提升|提高|增长|降低|减少|节省|improved|increased|reduced|saved).*\d+',  # 变化量
            r'\d+\s*(times?|倍|x)',  # 倍数
            r'\$\d+',          # 金额
            r'\d+\+',          # 数字+
        ]
        return any(re.search(p, text, re.IGNORECASE) for p in patterns)

    def _user_indicates_no_more(self, response: str) -> bool:
        """判断用户是否表示没有更多信息"""
        no_more_keywords = [
            "没有了", "没了", "就这些", "没有其他", "暂时没有", "不需要", "这就是全部",
            "no more", "that's all", "nothing else", "that's it", "no other",
            "就这样", "差不多了", "basically it", "nothing to add"
        ]
        response_lower = response.lower()
        return any(kw in response_lower for kw in no_more_keywords)

    def quick_extract(self, user_response: str, module: ProfileModule) -> Dict[str, Any]:
        """
        快速提取信息（不调用 LLM）

        用于简单场景的快速信息提取
        """
        extracted = {}
        module_name = module.value

        # 基于正则表达式的快速提取
        if module_name == "experience":
            # 尝试提取公司名
            company_patterns = [
                r'在(.+?)公司',
                r'在(.+?)工作',
                r'at (.+?) company',
                r'worked at (.+)',
            ]
            for pattern in company_patterns:
                match = re.search(pattern, user_response, re.IGNORECASE)
                if match:
                    extracted["company"] = match.group(1).strip()
                    break

        elif module_name == "education":
            # 尝试提取学校名
            school_patterns = [
                r'在(.+?)大学',
                r'毕业于(.+)',
                r'graduated from (.+)',
                r'studied at (.+)',
            ]
            for pattern in school_patterns:
                match = re.search(pattern, user_response, re.IGNORECASE)
                if match:
                    extracted["school"] = match.group(1).strip()
                    break

        return extracted
