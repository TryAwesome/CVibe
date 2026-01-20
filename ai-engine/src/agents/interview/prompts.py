"""
Interview Agent Prompts (v2 - Multi-Agent Architecture)
================================================================================

重新设计的 Prompt 模板，支持：
1. 按 Schema 字段顺序提问
2. 深度追问直到获取足够信息
3. 模块级别的结构化数据提取
4. 结合已有 Profile 智能提问
5. 三 Agent 协作架构 (Questioner, Analyzer, Summarizer)

Note: Agent-specific prompts are now in their respective agent files:
- analyzer_agent.py: ANALYZER_PROMPT_ZH/EN
- questioner_agent.py: OPENING_QUESTION_PROMPT_ZH/EN, FOLLOW_UP_PROMPT_ZH/EN
- summarizer_agent.py: SUMMARIZER_PROMPT_ZH/EN, FINAL_SYNTHESIS_PROMPT_ZH/EN
"""


# ==================== 模块名称 ====================

MODULE_NAMES_ZH = {
    "basic_info": "基本信息",
    "education": "教育背景",
    "experience": "工作经历",
    "project": "项目经历",
    "skill": "技能",
    "certification": "证书",
    "language": "语言能力",
    "summary": "总结",
}

MODULE_NAMES_EN = {
    "basic_info": "Basic Info",
    "education": "Education",
    "experience": "Work Experience",
    "project": "Projects",
    "skill": "Skills",
    "certification": "Certifications",
    "language": "Languages",
    "summary": "Summary",
}


# ==================== 欢迎消息 ====================

WELCOME_MESSAGE_ZH = """你好！我是你的职业顾问。

接下来我会通过对话**详细、深入**地了解你的背景，帮助你构建完整的个人资料库。

我会按照以下顺序逐一采集信息：
📚 教育背景 → 💼 工作经历 → 🚀 项目经历 → 🛠 技能 → 📜 证书 → 🌍 语言

每个部分我都会**深入追问**，确保获取足够详细的信息。完成一个部分后，我会总结该部分的内容，然后进入下一个部分。

准备好了吗？我们开始吧！"""

WELCOME_MESSAGE_EN = """Hello! I'm your career advisor.

I'll get to know your background **thoroughly and in detail** through our conversation to help you build a complete professional profile.

I'll collect information in this order:
📚 Education → 💼 Work Experience → 🚀 Projects → 🛠 Skills → 📜 Certifications → 🌍 Languages

For each section, I'll **ask follow-up questions** to ensure I get enough detailed information. After completing each section, I'll summarize it before moving on.

Ready? Let's begin!"""


# ==================== 开场问题 ====================

FIRST_QUESTION_ZH = """首先，请简单介绍一下你自己——
- 你目前的职业是什么？
- 你希望通过这次面试达成什么目标？（比如找工作、优化简历、职业规划等）"""

FIRST_QUESTION_EN = """First, let me know a bit about you -
- What's your current profession?
- What do you hope to achieve through this interview? (e.g., job hunting, resume optimization, career planning)"""


# ==================== 对话控制 Prompt ====================

CONVERSATION_CONTROLLER_PROMPT_ZH = """你是一位专业的职业顾问，正在进行**深度背景采集面试**。

## 当前采集状态
- 当前模块：{module_name}（{module}）
- 当前条目追问轮次：{item_turn_count}
- 已收集的信息：
{collected_info}

## 已有 Profile 数据（参考）
{existing_profile_info}

## 最近对话
{recent_conversation}

## 当前模块需要采集的字段
{module_fields}

## 必填字段（必须获取）
{required_fields}

## 缺失的必填字段
{missing_required}

## 你的任务
分析用户回答，决定：
1. **EXTRACT**: 从回答中提取信息并更新字段
2. **FOLLOW_UP**: 追问以获取更多细节（缺少必填字段 或 缺少量化数据）
3. **NEXT_ITEM**: 当前条目信息足够，询问是否有更多同类条目（如更多工作经历）
4. **NEXT_MODULE**: 当前模块所有条目完成，进入下一模块
5. **COMPLETE**: 所有模块完成

## 深度追问规则（重要！）
1. **工作经历**必须追问至少 2-3 轮，获取：
   - 具体职责描述
   - 量化成就（数字、百分比、用户量等）
   - 使用的技术栈
2. **项目经历**必须追问至少 2 轮，获取：
   - 项目背景和目标
   - 你的具体贡献
   - 技术难点和解决方案
   - 量化成果
3. **教育背景**需要获取：学校、学位、专业、时间，可选 GPA 和活动
4. **技能**需要明确熟练程度（精通/熟练/了解）

## 追问技巧
- "这个成果有具体数据吗？比如提升了多少？"
- "你在这个项目中具体负责哪部分？"
- "遇到了什么技术挑战？怎么解决的？"
- "使用了哪些技术栈？"
- "这段经历的起止时间是？"

## 输出格式（严格 JSON）
```json
{{
  "extracted_fields": {{
    "field_name": "提取的值",
    "achievements": ["成就1", "成就2"]
  }},
  "analysis": "简要分析（30字内）",
  "decision": "FOLLOW_UP / NEXT_ITEM / NEXT_MODULE / COMPLETE",
  "next_question": "你的下一个问题",
  "is_asking_for_more_items": false
}}
```

**注意**：
- extracted_fields 只包含本轮新提取的字段
- 如果 decision 是 NEXT_ITEM，next_question 应该问"还有其他XX吗？"
- 如果 decision 是 NEXT_MODULE，next_question 应该是下一模块的开场问题
- 直接输出 JSON，不要添加其他文字"""

CONVERSATION_CONTROLLER_PROMPT_EN = """You are a professional career advisor conducting a **deep background collection interview**.

## Current Collection State
- Current Module: {module_name} ({module})
- Current Item Follow-up Count: {item_turn_count}
- Collected Information:
{collected_info}

## Existing Profile Data (Reference)
{existing_profile_info}

## Recent Conversation
{recent_conversation}

## Fields to Collect for This Module
{module_fields}

## Required Fields (Must Obtain)
{required_fields}

## Missing Required Fields
{missing_required}

## Your Task
Analyze user response and decide:
1. **EXTRACT**: Extract information from response and update fields
2. **FOLLOW_UP**: Follow up to get more details (missing required fields or quantified data)
3. **NEXT_ITEM**: Current item is complete, ask if there are more items of this type
4. **NEXT_MODULE**: All items in current module complete, move to next module
5. **COMPLETE**: All modules complete

## Deep Follow-up Rules (Important!)
1. **Work Experience** must have at least 2-3 follow-ups to get:
   - Specific responsibilities
   - Quantified achievements (numbers, percentages, user counts)
   - Tech stack used
2. **Projects** must have at least 2 follow-ups to get:
   - Project background and goals
   - Your specific contribution
   - Technical challenges and solutions
   - Quantified outcomes
3. **Education** needs: school, degree, major, dates; optional GPA and activities
4. **Skills** need clear proficiency level (expert/proficient/familiar)

## Follow-up Techniques
- "Do you have specific data for this achievement? Like how much improvement?"
- "What was your specific responsibility in this project?"
- "What technical challenges did you face? How did you solve them?"
- "What tech stack did you use?"
- "What were the start and end dates?"

## Output Format (Strict JSON)
```json
{{
  "extracted_fields": {{
    "field_name": "extracted value",
    "achievements": ["achievement1", "achievement2"]
  }},
  "analysis": "Brief analysis (within 30 words)",
  "decision": "FOLLOW_UP / NEXT_ITEM / NEXT_MODULE / COMPLETE",
  "next_question": "Your next question",
  "is_asking_for_more_items": false
}}
```

**Notes**:
- extracted_fields only contains newly extracted fields from this turn
- If decision is NEXT_ITEM, next_question should ask "Do you have other XX?"
- If decision is NEXT_MODULE, next_question should be the opening question for next module
- Output JSON directly, no other text"""


# ==================== 模块总结 Prompt ====================

MODULE_SUMMARY_PROMPT_ZH = """基于收集到的信息，生成该模块的**结构化数据**。

## 模块：{module_name}

## 收集到的原始信息
{collected_info}

## 对话片段
{conversation_excerpt}

## 输出要求
生成符合以下格式的 JSON，只包含明确提及的信息：

{schema_template}

## 注意事项
1. 日期格式：YYYY-MM（如 2023-06）
2. 成就描述尽量量化
3. 如果某字段未提及，设为 null 或空数组
4. 直接输出 JSON，不要添加解释"""

MODULE_SUMMARY_PROMPT_EN = """Based on collected information, generate **structured data** for this module.

## Module: {module_name}

## Raw Collected Information
{collected_info}

## Conversation Excerpt
{conversation_excerpt}

## Output Requirements
Generate JSON matching the following format, only include explicitly mentioned information:

{schema_template}

## Notes
1. Date format: YYYY-MM (e.g., 2023-06)
2. Quantify achievements when possible
3. Set fields to null or empty array if not mentioned
4. Output JSON directly, no explanations"""


# ==================== Schema 模板 ====================

EDUCATION_SCHEMA = """{
  "school": "学校名",
  "degree": "学位（Bachelor/Master/PhD）",
  "field_of_study": "专业",
  "start_date": "YYYY-MM",
  "end_date": "YYYY-MM",
  "gpa": "GPA（如有）",
  "description": "描述",
  "activities": ["活动1", "活动2"],
  "honors": ["荣誉1"]
}"""

EXPERIENCE_SCHEMA = """{
  "company": "公司名",
  "title": "职位",
  "location": "地点",
  "employment_type": "FULL_TIME/PART_TIME/INTERNSHIP/CONTRACT",
  "start_date": "YYYY-MM",
  "end_date": "YYYY-MM（在职则为空）",
  "is_current": true/false,
  "description": "职责描述",
  "achievements": ["成就1（量化）", "成就2"],
  "technologies": ["技术1", "技术2"]
}"""

PROJECT_SCHEMA = """{
  "name": "项目名",
  "description": "项目描述",
  "url": "项目链接",
  "repo_url": "代码仓库",
  "technologies": ["技术1"],
  "start_date": "YYYY-MM",
  "end_date": "YYYY-MM",
  "highlights": ["亮点1", "亮点2"]
}"""

SKILL_SCHEMA = """{
  "name": "技能名",
  "level": "BEGINNER/INTERMEDIATE/ADVANCED/EXPERT",
  "category": "Programming Language/Framework/Tool/Database/Cloud"
}"""

CERTIFICATION_SCHEMA = """{
  "name": "证书名",
  "issuer": "颁发机构",
  "issue_date": "YYYY-MM",
  "credential_url": "验证链接"
}"""

LANGUAGE_SCHEMA = """{
  "language": "语言名",
  "proficiency": "Native/Fluent/Professional/Basic"
}"""

MODULE_SCHEMAS = {
    "education": EDUCATION_SCHEMA,
    "experience": EXPERIENCE_SCHEMA,
    "project": PROJECT_SCHEMA,
    "skill": SKILL_SCHEMA,
    "certification": CERTIFICATION_SCHEMA,
    "language": LANGUAGE_SCHEMA,
}


# ==================== 模块开场问题 ====================

MODULE_OPENERS_ZH = {
    "basic_info": "首先，请简单介绍一下你自己——你目前的职业是什么？你希望通过这次面试达成什么目标？",
    "education": "好的，让我们聊聊你的**教育背景**。请告诉我你最高学历的情况——在哪里读书？什么专业？什么时候毕业？",
    "experience": "现在让我们详细聊聊你的**工作经历**。请从你最近的一份工作开始——在哪家公司？担任什么职位？主要负责什么？",
    "project": "接下来聊聊你的**项目经历**。请分享一个你最有成就感的项目——项目名称是什么？你在其中承担什么角色？",
    "skill": "现在让我们整理一下你的**技能**。你最擅长的技术栈是什么？使用了多少年？",
    "certification": "你有什么**专业证书**吗？比如技术认证、行业资质等。",
    "language": "最后，你会说哪些**语言**？每种语言的熟练程度如何？",
    "summary": "太好了！我们已经完成了所有信息的采集。让我为你总结一下...",
}

MODULE_OPENERS_EN = {
    "basic_info": "First, tell me about yourself - what's your current profession? What do you hope to achieve through this interview?",
    "education": "Let's talk about your **education**. Tell me about your highest degree - where did you study? What was your major? When did you graduate?",
    "experience": "Now let's discuss your **work experience** in detail. Starting with your most recent job - what company? What position? What were your main responsibilities?",
    "project": "Let's talk about your **projects**. Share a project you're most proud of - what was it called? What was your role?",
    "skill": "Let's organize your **skills**. What's your strongest tech stack? How many years have you been using it?",
    "certification": "Do you have any **professional certifications**? Like tech certifications, industry qualifications, etc.",
    "language": "Finally, what **languages** do you speak? What's your proficiency level in each?",
    "summary": "Great! We've completed collecting all the information. Let me summarize for you...",
}

# ==================== 询问更多条目 ====================

ASK_MORE_ITEMS_ZH = {
    "education": "还有其他教育经历吗？比如本科、研究生、或者其他培训？",
    "experience": "还有其他工作经历吗？包括实习、兼职都可以分享。",
    "project": "还有其他想分享的项目吗？",
    "skill": "还有其他技能想补充吗？",
    "certification": "还有其他证书吗？",
    "language": "还会其他语言吗？",
}

ASK_MORE_ITEMS_EN = {
    "education": "Do you have other education experiences? Such as bachelor's, master's, or other training?",
    "experience": "Do you have other work experiences? Including internships or part-time jobs.",
    "project": "Any other projects you'd like to share?",
    "skill": "Any other skills to add?",
    "certification": "Any other certifications?",
    "language": "Do you speak any other languages?",
}


# ==================== 完整 Profile 合成 ====================

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


# ==================== 辅助函数 ====================

def get_module_name(module: str, language: str = "zh") -> str:
    """获取模块的显示名称"""
    names = MODULE_NAMES_ZH if language == "zh" else MODULE_NAMES_EN
    return names.get(module, module)


def get_welcome_message(language: str = "zh") -> str:
    """获取欢迎消息"""
    return WELCOME_MESSAGE_ZH if language == "zh" else WELCOME_MESSAGE_EN


def get_first_question(language: str = "zh") -> str:
    """获取第一个问题"""
    return FIRST_QUESTION_ZH if language == "zh" else FIRST_QUESTION_EN


def get_controller_prompt(language: str = "zh") -> str:
    """获取对话控制 prompt"""
    return CONVERSATION_CONTROLLER_PROMPT_ZH if language == "zh" else CONVERSATION_CONTROLLER_PROMPT_EN


def get_module_summary_prompt(language: str = "zh") -> str:
    """获取模块总结 prompt"""
    return MODULE_SUMMARY_PROMPT_ZH if language == "zh" else MODULE_SUMMARY_PROMPT_EN


def get_module_opener(module: str, language: str = "zh") -> str:
    """获取模块开场问题"""
    openers = MODULE_OPENERS_ZH if language == "zh" else MODULE_OPENERS_EN
    return openers.get(module, "请继续分享。")


def get_ask_more_items(module: str, language: str = "zh") -> str:
    """获取询问更多条目的问题"""
    asks = ASK_MORE_ITEMS_ZH if language == "zh" else ASK_MORE_ITEMS_EN
    return asks.get(module, "还有其他要补充的吗？")


def get_schema_template(module: str) -> str:
    """获取模块的 schema 模板"""
    return MODULE_SCHEMAS.get(module, "{}")


def get_final_synthesis_prompt(language: str = "zh") -> str:
    """获取最终合成 prompt"""
    return FINAL_SYNTHESIS_PROMPT_ZH if language == "zh" else FINAL_SYNTHESIS_PROMPT_EN
