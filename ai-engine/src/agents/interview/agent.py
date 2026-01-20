"""
Profile Interview Agent (v2)
================================================================================

重新设计的面试 Agent，支持：
1. 按 Profile Schema 顺序逐模块采集
2. 深度追问直到获取足够信息
3. 每完成一个模块立即提取结构化数据
4. 结合已有 Profile 数据智能提问
"""

import json
import logging
from typing import Iterator, Optional, Dict, Any, List
from datetime import datetime

from .models import (
    ProfileModule,
    InterviewSessionState,
    ModuleProgress,
    ModuleItem,
    MODULE_FIELDS,
    REQUIRED_FIELDS,
    ExtractedProfile,
)
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

logger = logging.getLogger(__name__)


class ProfileInterviewAgent:
    """
    Profile Interview Agent v2

    核心设计：
    - 按 Schema 顺序采集：basic_info → education → experience → project → skill → certification → language
    - 深度追问：每个条目必须收集完整信息才进入下一个
    - 模块总结：每完成一个模块，立即提取该模块的结构化数据
    - 智能提问：结合已有 Profile 数据进行针对性提问
    """

    # 最小追问轮次（不同模块不同）
    MIN_FOLLOW_UPS = {
        "basic_info": 1,
        "education": 2,
        "experience": 3,  # 工作经历需要更深入
        "project": 2,
        "skill": 1,
        "certification": 1,
        "language": 1,
    }

    def __init__(self, llm_client, session_store=None):
        """
        初始化 Agent

        Args:
            llm_client: LLM 客户端
            session_store: 会话存储（默认内存存储）
        """
        self.llm = llm_client
        self.session_store = session_store

    def start_session(
        self,
        user_id: str,
        session_id: str,
        language: str = "zh",
        existing_profile: Optional[str] = None
    ) -> tuple[InterviewSessionState, str, str]:
        """
        开始新的面试会话

        Args:
            user_id: 用户 ID
            session_id: 会话 ID
            language: 语言（zh/en）
            existing_profile: 已有 Profile JSON（可选）

        Returns:
            (state, welcome_message, first_question)
        """
        # 解析已有 Profile
        existing_data = {}
        if existing_profile:
            try:
                existing_data = json.loads(existing_profile)
                logger.info(f"Loaded existing profile for user {user_id}")
            except json.JSONDecodeError:
                logger.warning("Failed to parse existing profile")

        # 初始化会话状态
        state = InterviewSessionState(
            session_id=session_id,
            user_id=user_id,
            language=language,
            current_module=ProfileModule.BASIC_INFO,
            existing_profile=existing_data,
        )

        # 获取欢迎消息和第一个问题
        welcome = get_welcome_message(language)
        first_question = get_first_question(language)

        # 记录开场对话
        state.conversation_history.append({
            "role": "assistant",
            "content": f"{welcome}\n\n{first_question}"
        })

        # 保存会话
        if self.session_store:
            self.session_store.set(session_id, state.to_dict())

        logger.info(f"Started interview session: {session_id} for user: {user_id}")

        return state, welcome, first_question

    def process_message(
        self,
        session_id: str,
        user_message: str
    ) -> Iterator[str]:
        """
        处理用户消息，流式返回响应

        核心逻辑：
        1. 加载会话状态
        2. 添加用户消息到历史
        3. 调用 LLM 分析回答并决定下一步
        4. 根据决策：追问 / 下一条目 / 下一模块 / 完成
        5. 如果切换模块，先提取当前模块的结构化数据

        Args:
            session_id: 会话 ID
            user_message: 用户消息

        Yields:
            响应内容块
        """
        # 加载会话
        if not self.session_store:
            yield "会话存储未配置。"
            return

        state_dict = self.session_store.get(session_id)
        if not state_dict:
            yield "会话不存在，请重新开始。"
            return

        state = InterviewSessionState.from_dict(state_dict)
        language = state.language

        # 添加用户消息
        state.conversation_history.append({
            "role": "user",
            "content": user_message
        })
        state.update_activity()

        # 检查是否在总结阶段
        if state.current_module == ProfileModule.SUMMARY:
            response = self._handle_summary_confirmation(state, user_message)
            state.conversation_history.append({
                "role": "assistant",
                "content": response
            })
            self.session_store.set(session_id, state.to_dict())
            yield response
            return

        # 获取当前模块进度
        module_progress = state.get_current_module_progress()

        # 如果当前模块还没有条目，创建一个
        if not module_progress.items:
            module_progress.start_new_item()

        # 构建 LLM prompt
        prompt = self._build_controller_prompt(state)

        # 调用 LLM
        messages = [
            {"role": "system", "content": self._get_system_prompt(language)},
            {"role": "user", "content": prompt}
        ]

        try:
            if hasattr(self.llm, 'chat'):
                response = self.llm.chat(messages, temperature=0.7)
            else:
                response = self.llm.complete(prompt)

            result = self._parse_controller_response(response)
            logger.debug(f"Controller result: {result}")

            # 更新已收集的字段
            extracted = result.get("extracted_fields", {})
            current_item = module_progress.get_current_item()
            if current_item and extracted:
                for key, value in extracted.items():
                    if value:  # 只更新非空值
                        current_item.fields[key] = value
                current_item.follow_up_count += 1

            # 根据决策处理
            decision = result.get("decision", "FOLLOW_UP")
            next_question = result.get("next_question", "")

            if decision == "FOLLOW_UP":
                # 继续追问当前条目
                pass

            elif decision == "NEXT_ITEM":
                # 完成当前条目，询问是否有更多
                if current_item:
                    # 提取当前条目的结构化数据
                    extracted_data = self._extract_item_data(state, current_item)
                    module_progress.complete_current_item(extracted_data)

                # 检查用户是否表示还有更多
                if result.get("is_asking_for_more_items", False):
                    # 等待用户回答
                    pass
                else:
                    # 准备询问是否有更多
                    next_question = get_ask_more_items(state.current_module.value, language)

            elif decision == "NEXT_MODULE":
                # 完成当前模块
                if current_item and not current_item.is_complete:
                    extracted_data = self._extract_item_data(state, current_item)
                    module_progress.complete_current_item(extracted_data)

                module_progress.is_module_complete = True

                # 生成模块总结（可选显示给用户）
                summary = self._generate_module_summary(state)
                logger.info(f"Module {state.current_module.value} completed with {len(module_progress.extracted_data)} items")

                # 进入下一模块
                if state.advance_to_next_module():
                    next_question = get_module_opener(state.current_module.value, language)

                    # 结合已有数据智能调整问题
                    next_question = self._enhance_question_with_existing_data(
                        state, next_question
                    )
                else:
                    # 所有模块完成，进入总结
                    state.current_module = ProfileModule.SUMMARY
                    next_question = self._generate_final_summary(state)

            elif decision == "COMPLETE":
                # 全部完成
                state.status = "COMPLETED"
                next_question = self._generate_completion_message(state)

            # 处理用户表示"没有更多"的情况
            if self._user_says_no_more(user_message):
                # 完成当前模块，进入下一个
                module_progress.is_module_complete = True

                if state.advance_to_next_module():
                    next_question = get_module_opener(state.current_module.value, language)
                    next_question = self._enhance_question_with_existing_data(
                        state, next_question
                    )
                else:
                    state.current_module = ProfileModule.SUMMARY
                    next_question = self._generate_final_summary(state)

            # 处理用户表示"有更多"的情况
            elif self._user_says_has_more(user_message) and decision == "NEXT_ITEM":
                # 开始新条目
                module_progress.start_new_item()
                state.current_item_turn_count = 0
                next_question = get_module_opener(state.current_module.value, language)

            # 记录 AI 响应
            if next_question:
                state.conversation_history.append({
                    "role": "assistant",
                    "content": next_question
                })

            # 保存状态
            self.session_store.set(session_id, state.to_dict())

            # 返回响应
            yield next_question if next_question else result.get("next_question", "请继续。")

        except Exception as e:
            logger.error(f"Error processing message: {e}", exc_info=True)
            error_msg = "抱歉，处理出错了。请再说一遍。" if language == "zh" else "Sorry, there was an error. Please try again."
            yield error_msg

    def get_session_state(self, session_id: str) -> Optional[Dict[str, Any]]:
        """获取会话状态"""
        if not self.session_store:
            return None

        state_dict = self.session_store.get(session_id)
        if not state_dict:
            return None

        state = InterviewSessionState.from_dict(state_dict)

        # 计算完成进度
        completed_modules = sum(
            1 for p in state.module_progress.values()
            if p.is_module_complete
        )
        total_modules = len(ProfileModule) - 1  # 不包括 SUMMARY

        return {
            "session_id": state.session_id,
            "user_id": state.user_id,
            "current_module": state.current_module.value,
            "module_name": get_module_name(state.current_module.value, state.language),
            "turn_count": state.turn_count,
            "status": state.status,
            "progress": f"{completed_modules}/{total_modules}",
            "extracted_data": state.get_all_extracted_data(),
        }

    def finish_session(self, session_id: str) -> Dict[str, Any]:
        """
        完成面试，生成最终 Profile

        Returns:
            {
                "success": bool,
                "profile": dict,
                "completeness_score": int,
                "missing_sections": list
            }
        """
        if not self.session_store:
            return {"success": False, "error": "Session store not configured"}

        state_dict = self.session_store.get(session_id)
        if not state_dict:
            return {"success": False, "error": "Session not found"}

        state = InterviewSessionState.from_dict(state_dict)

        # 收集所有已提取的模块数据
        all_extracted = state.get_all_extracted_data()

        # 调用 LLM 合成最终 Profile
        profile = self._synthesize_final_profile(state, all_extracted)

        # 标记完成
        state.status = "COMPLETED"
        self.session_store.set(session_id, state.to_dict())

        logger.info(f"Finished session {session_id}, completeness: {profile.get('completeness_score', 0)}")

        return {
            "success": True,
            "profile": profile,
            "completeness_score": profile.get("completeness_score", 0),
            "missing_sections": profile.get("missing_sections", [])
        }

    # ==================== 私有方法 ====================

    def _get_system_prompt(self, language: str) -> str:
        """获取系统 prompt"""
        if language == "zh":
            return """你是一位专业的职业顾问，正在进行深度背景采集面试。
你的目标是通过对话深入了解用户的职业背景，收集简历所需的所有详细信息。
关键要求：
1. 每个问题要追问到足够深入，获取量化数据
2. 工作经历和项目经历需要特别详细
3. 用中文回复，保持专业、友好"""
        else:
            return """You are a professional career advisor conducting a deep background collection interview.
Your goal is to thoroughly understand the user's career background and collect all detailed information needed for their resume.
Key requirements:
1. Follow up on each question until you get enough depth and quantified data
2. Work experience and projects need particularly detailed information
3. Respond in English, maintaining a professional and friendly tone"""

    def _build_controller_prompt(self, state: InterviewSessionState) -> str:
        """构建对话控制器的 prompt"""
        module = state.current_module.value
        module_name = get_module_name(module, state.language)

        # 获取当前模块进度
        progress = state.get_current_module_progress()
        current_item = progress.get_current_item()

        # 已收集的信息
        collected_info = "无" if not current_item else json.dumps(
            current_item.fields, ensure_ascii=False, indent=2
        )

        # 已有 Profile 数据
        existing_info = self._format_existing_profile(state, module)

        # 最近对话
        recent = state.conversation_history[-6:]
        recent_conv = "\n".join([
            f"{'用户' if m['role'] == 'user' else '顾问'}: {m['content']}"
            for m in recent
        ])

        # 模块字段
        module_fields = MODULE_FIELDS.get(ProfileModule(module), [])
        required_fields = REQUIRED_FIELDS.get(ProfileModule(module), [])

        # 缺失的必填字段
        missing_required = []
        if current_item:
            missing_required = current_item.get_missing_required_fields()

        prompt_template = get_controller_prompt(state.language)

        return prompt_template.format(
            module=module,
            module_name=module_name,
            item_turn_count=state.current_item_turn_count,
            collected_info=collected_info,
            existing_profile_info=existing_info,
            recent_conversation=recent_conv,
            module_fields=", ".join(module_fields),
            required_fields=", ".join(required_fields),
            missing_required=", ".join(missing_required) if missing_required else "无"
        )

    def _format_existing_profile(self, state: InterviewSessionState, module: str) -> str:
        """格式化已有 Profile 数据供参考"""
        existing = state.existing_profile
        if not existing:
            return "无已有数据"

        # 根据模块提取相关数据
        module_data_map = {
            "education": existing.get("educations", []),
            "experience": existing.get("experiences", []),
            "project": existing.get("projects", []),
            "skill": existing.get("skills", []),
            "certification": existing.get("certifications", []),
            "language": existing.get("languages", []),
        }

        data = module_data_map.get(module, [])
        if not data:
            return "该模块无已有数据"

        return json.dumps(data, ensure_ascii=False, indent=2)[:500]  # 限制长度

    def _parse_controller_response(self, response: str) -> Dict[str, Any]:
        """解析控制器 LLM 响应"""
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
                    json_str = response

            result = json.loads(json_str.strip())

            # 确保必要字段存在
            result.setdefault("extracted_fields", {})
            result.setdefault("decision", "FOLLOW_UP")
            result.setdefault("next_question", "")

            return result

        except json.JSONDecodeError:
            logger.warning("Failed to parse controller response as JSON")
            return {
                "extracted_fields": {},
                "decision": "FOLLOW_UP",
                "next_question": response[:200]
            }

    def _extract_item_data(self, state: InterviewSessionState, item: ModuleItem) -> Dict:
        """提取单个条目的结构化数据"""
        module = item.module

        # 简单情况：直接使用已收集的字段
        if module in ["skill", "certification", "language"]:
            return item.fields.copy()

        # 复杂情况：调用 LLM 提取
        prompt_template = get_module_summary_prompt(state.language)
        schema = get_schema_template(module)

        # 获取相关对话片段
        conversation_excerpt = self._get_module_conversation(state, module)

        prompt = prompt_template.format(
            module_name=get_module_name(module, state.language),
            collected_info=json.dumps(item.fields, ensure_ascii=False, indent=2),
            conversation_excerpt=conversation_excerpt,
            schema_template=schema
        )

        try:
            messages = [
                {"role": "system", "content": "You are a data extraction expert. Extract structured data from interview conversations."},
                {"role": "user", "content": prompt}
            ]

            if hasattr(self.llm, 'chat'):
                response = self.llm.chat(messages, temperature=0.1)
            else:
                response = self.llm.complete(prompt)

            # 解析 JSON
            if "```json" in response:
                json_str = response.split("```json")[1].split("```")[0]
            elif "```" in response:
                json_str = response.split("```")[1].split("```")[0]
            else:
                start = response.find("{")
                end = response.rfind("}") + 1
                json_str = response[start:end] if start >= 0 else "{}"

            extracted = json.loads(json_str.strip())

            # 合并已有字段
            result = item.fields.copy()
            result.update(extracted)

            return result

        except Exception as e:
            logger.error(f"Error extracting item data: {e}")
            return item.fields.copy()

    def _get_module_conversation(self, state: InterviewSessionState, module: str) -> str:
        """获取特定模块的对话片段"""
        # 简化实现：返回最近的对话
        recent = state.conversation_history[-10:]
        return "\n".join([
            f"{'User' if m['role'] == 'user' else 'Advisor'}: {m['content']}"
            for m in recent
        ])

    def _generate_module_summary(self, state: InterviewSessionState) -> str:
        """生成模块总结"""
        module = state.current_module.value
        progress = state.get_current_module_progress()

        if state.language == "zh":
            summary = f"✅ **{get_module_name(module, 'zh')}**模块已完成！\n\n"
            summary += f"共收集了 {len(progress.extracted_data)} 条记录。\n"
        else:
            summary = f"✅ **{get_module_name(module, 'en')}** module completed!\n\n"
            summary += f"Collected {len(progress.extracted_data)} records.\n"

        return summary

    def _generate_final_summary(self, state: InterviewSessionState) -> str:
        """生成最终总结"""
        all_data = state.get_all_extracted_data()

        if state.language == "zh":
            summary = "🎉 **太好了！我们已经完成了所有信息的采集。**\n\n"
            summary += "让我为你总结一下收集到的内容：\n\n"

            if "education" in all_data:
                summary += f"📚 **教育背景**: {len(all_data['education'])} 条\n"
            if "experience" in all_data:
                summary += f"💼 **工作经历**: {len(all_data['experience'])} 条\n"
            if "project" in all_data:
                summary += f"🚀 **项目经历**: {len(all_data['project'])} 条\n"
            if "skill" in all_data:
                summary += f"🛠 **技能**: {len(all_data['skill'])} 项\n"
            if "certification" in all_data:
                summary += f"📜 **证书**: {len(all_data['certification'])} 个\n"
            if "language" in all_data:
                summary += f"🌍 **语言**: {len(all_data['language'])} 种\n"

            summary += "\n这些信息准确吗？有什么需要补充或修改的吗？"
            summary += "\n\n如果没问题，请说「确认」，我会为你生成完整的个人资料。"
        else:
            summary = "🎉 **Great! We've completed collecting all the information.**\n\n"
            summary += "Let me summarize what we collected:\n\n"

            if "education" in all_data:
                summary += f"📚 **Education**: {len(all_data['education'])} entries\n"
            if "experience" in all_data:
                summary += f"💼 **Work Experience**: {len(all_data['experience'])} entries\n"
            if "project" in all_data:
                summary += f"🚀 **Projects**: {len(all_data['project'])} entries\n"
            if "skill" in all_data:
                summary += f"🛠 **Skills**: {len(all_data['skill'])} items\n"
            if "certification" in all_data:
                summary += f"📜 **Certifications**: {len(all_data['certification'])} items\n"
            if "language" in all_data:
                summary += f"🌍 **Languages**: {len(all_data['language'])} items\n"

            summary += "\nIs this accurate? Anything to add or correct?"
            summary += "\n\nIf everything looks good, say 'Confirm' and I'll generate your complete profile."

        return summary

    def _handle_summary_confirmation(self, state: InterviewSessionState, user_message: str) -> str:
        """处理总结确认"""
        # 检查用户是否确认
        confirm_keywords = ["确认", "没问题", "可以", "好的", "confirm", "yes", "ok", "looks good"]
        if any(kw in user_message.lower() for kw in confirm_keywords):
            state.status = "COMPLETED"
            if state.language == "zh":
                return "✨ 太好了！你的个人资料已经准备就绪。\n\n面试已完成，感谢你的配合！"
            else:
                return "✨ Great! Your profile is now ready.\n\nInterview complete, thank you for your cooperation!"
        else:
            if state.language == "zh":
                return "好的，请告诉我需要补充或修改什么内容？"
            else:
                return "Sure, please tell me what you'd like to add or modify?"

    def _generate_completion_message(self, state: InterviewSessionState) -> str:
        """生成完成消息"""
        if state.language == "zh":
            return "✨ 面试已完成！感谢你的分享。你的个人资料已经准备就绪。"
        else:
            return "✨ Interview complete! Thank you for sharing. Your profile is now ready."

    def _enhance_question_with_existing_data(self, state: InterviewSessionState, question: str) -> str:
        """结合已有数据增强问题"""
        existing = state.existing_profile
        if not existing:
            return question

        module = state.current_module.value

        # 检查是否已有该模块的数据
        module_key_map = {
            "education": "educations",
            "experience": "experiences",
            "project": "projects",
            "skill": "skills",
            "certification": "certifications",
            "language": "languages",
        }

        key = module_key_map.get(module)
        if key and key in existing and existing[key]:
            items = existing[key]
            if state.language == "zh":
                question = f"我看到你已有的资料中有一些{get_module_name(module, 'zh')}记录。让我们来补充或更新这些信息。\n\n{question}"
            else:
                question = f"I see you have some {get_module_name(module, 'en')} records in your existing profile. Let's supplement or update this information.\n\n{question}"

        return question

    def _user_says_no_more(self, message: str) -> bool:
        """判断用户是否表示没有更多"""
        no_keywords = [
            "没有了", "没了", "就这些", "没有其他", "暂时没有", "不需要",
            "no more", "that's all", "nothing else", "no", "nope"
        ]
        return any(kw in message.lower() for kw in no_keywords)

    def _user_says_has_more(self, message: str) -> bool:
        """判断用户是否表示还有更多"""
        yes_keywords = [
            "还有", "有的", "是的", "对", "yes", "yeah", "yep", "sure", "have more"
        ]
        return any(kw in message.lower() for kw in yes_keywords)

    def _synthesize_final_profile(self, state: InterviewSessionState, all_extracted: Dict) -> Dict:
        """合成最终 Profile"""
        prompt_template = get_final_synthesis_prompt(state.language)

        # 格式化已提取的模块数据
        extracted_str = json.dumps(all_extracted, ensure_ascii=False, indent=2)

        # 完整对话
        full_conv = "\n".join([
            f"{'User' if m['role'] == 'user' else 'Advisor'}: {m['content']}"
            for m in state.conversation_history
        ])

        prompt = prompt_template.format(
            extracted_modules=extracted_str,
            full_conversation=full_conv[-5000:]  # 限制长度
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

            # 解析 JSON
            if "```json" in response:
                json_str = response.split("```json")[1].split("```")[0]
            elif "```" in response:
                json_str = response.split("```")[1].split("```")[0]
            else:
                start = response.find("{")
                end = response.rfind("}") + 1
                json_str = response[start:end] if start >= 0 else "{}"

            profile = json.loads(json_str.strip())

            # 确保必要字段
            profile.setdefault("completeness_score", 50)
            profile.setdefault("missing_sections", [])
            profile.setdefault("education", all_extracted.get("education", []))
            profile.setdefault("experiences", all_extracted.get("experience", []))
            profile.setdefault("projects", all_extracted.get("project", []))
            profile.setdefault("skills", all_extracted.get("skill", []))
            profile.setdefault("certifications", all_extracted.get("certification", []))
            profile.setdefault("languages", all_extracted.get("language", []))

            return profile

        except Exception as e:
            logger.error(f"Error synthesizing profile: {e}")

            # 回退：直接使用已提取的数据
            return {
                "education": all_extracted.get("education", []),
                "experiences": all_extracted.get("experience", []),
                "projects": all_extracted.get("project", []),
                "skills": all_extracted.get("skill", []),
                "certifications": all_extracted.get("certification", []),
                "languages": all_extracted.get("language", []),
                "completeness_score": 50,
                "missing_sections": ["synthesis_failed"]
            }
