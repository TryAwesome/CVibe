"""
QuestionerAgent - Question Generation Expert
================================================================================

Core responsibilities:
- Read and understand existing Profile data
- Generate opening questions for each module
- Create targeted follow-up questions based on AnalyzerAgent feedback
- Reference existing data and previous answers in questions

Design principles:
- Questions should be conversational and natural
- Reference existing data to show awareness
- Follow-up questions should be specific and actionable
- English only, no emojis
"""

import json
import logging
from typing import Dict, List, Any, Optional

from .models import AnalysisResult, ProfileModule, QuestionResult
from .schemas import (
    get_module_schema,
    get_all_fields,
    get_required_fields,
    ModuleSchema,
)

logger = logging.getLogger(__name__)


# ==================== Questioner Prompts (English Only) ====================

OPENING_QUESTION_PROMPT = """You are a professional career advisor conducting a deep background collection interview.

## Current Module
{module_name}

## Information to Collect for This Module
{module_fields}

## User's Existing Profile Data (Reference)
{existing_profile_info}

## Your Task
Generate a natural, friendly opening question to start collecting information for this module.

## Requirements
1. If there's existing data for this module, acknowledge it: "I see you have some information, let's supplement/update it"
2. Questions should be specific, guiding users to provide detailed information
3. Maintain a professional, friendly tone
4. Ask one main question at a time, with optional prompts
5. Do NOT use any emojis

## Output Format
Output the question text directly, no other content."""


FOLLOW_UP_PROMPT = """You are a professional career advisor conducting a deep background collection interview.

## Current Module
{module_name}

## Recent Conversation
{recent_conversation}

## Collected Information
{collected_info}

## Analysis Feedback
- Missing Information: {missing_points}
- Follow-up Suggestions: {follow_up_suggestions}
- Quality Issues: {quality_issues}

## Your Task
Generate a targeted follow-up question based on the analysis feedback.

## Follow-up Techniques
1. Reference user's previous answers to show you're listening
2. Ask about specific missing points, not general questions
3. Use encouraging language like "Could you tell me more about...", "For example..."
4. Give examples to guide user responses

## Common Follow-up Patterns
- Quantified data: "Do you have specific numbers for this achievement? Like percentage improvement?"
- Specific contribution: "What was your specific responsibility in this project?"
- Technical details: "What technical challenges did you face? How did you solve them?"
- Tech stack: "What technologies and tools did you mainly use?"
- Timeline: "What were the start and end dates for this experience?"

## Important
- Do NOT use any emojis
- Keep the question professional and concise

## Output Format
Output the follow-up question text directly, no other content."""


ASK_MORE_ITEMS_PROMPT = """You are a professional career advisor. The user just finished describing a {module_name} entry.

## Number of Items Collected
{item_count}

## Your Task
Generate a natural question asking if the user has more items of this type to share.

## Requirements
1. Natural tone, not verbose
2. Can briefly mention the number of items collected
3. Give user clear choices: have more / no more
4. Do NOT use any emojis

## Output Format
Output the question text directly."""


class QuestionerAgent:
    """
    QuestionerAgent - Question Generation Expert

    Core responsibilities:
    - Read and understand existing Profile data
    - Generate opening questions for each module
    - Create targeted follow-up questions based on AnalyzerAgent feedback
    - English only, no emojis
    """

    # Module name mapping (English only)
    MODULE_NAMES = {
        "basic_info": "Basic Info",
        "education": "Education",
        "experience": "Work Experience",
        "project": "Projects",
        "skill": "Skills",
        "certification": "Certifications",
        "language": "Languages",
        "summary": "Summary",
    }

    # Default opening questions (English only, no emoji)
    DEFAULT_OPENERS = {
        "basic_info": "First, tell me about yourself - what's your current profession? What do you hope to achieve through this interview?",
        "education": "Let's talk about your education. Tell me about your highest degree - where did you study? What was your major? When did you graduate?",
        "experience": "Now let's discuss your work experience in detail. Starting with your most recent job - what company? What position? What were your main responsibilities?",
        "project": "Let's talk about your projects. Share a project you're most proud of - what was it called? What was your role?",
        "skill": "Let's organize your skills. What's your strongest tech stack? How many years have you been using it?",
        "certification": "Do you have any professional certifications? Like tech certifications, industry qualifications, etc.",
        "language": "Finally, what languages do you speak? What's your proficiency level in each?",
    }

    # Default "ask more" questions (English only, no emoji)
    # Added hint to make it clear user can say "no" if they have no more items
    DEFAULT_ASK_MORE = {
        "education": "Do you have other education experiences? Such as bachelor's, master's, or other training? (If no, just say 'no')",
        "experience": "Do you have other work experiences? Including internships or part-time jobs. (If no, just say 'no')",
        "project": "Any other projects you'd like to share? (If no, just say 'no')",
        "skill": "Any other skills to add? (If no, just say 'no')",
        "certification": "Any other certifications? (If no, just say 'no')",
        "language": "Do you speak any other languages? (If no, just say 'no')",
    }

    def __init__(self, llm_client):
        """
        Initialize QuestionerAgent

        Args:
            llm_client: LLM client
        """
        self.llm = llm_client

    def generate_opening_question(
        self,
        module: ProfileModule,
        existing_profile: Dict[str, Any],
        language: str = "en"
    ) -> QuestionResult:
        """
        Generate module opening question

        Args:
            module: Current module
            existing_profile: Existing Profile data
            language: Language (always "en")

        Returns:
            QuestionResult: Contains question and metadata
        """
        module_name = module.value

        # Get existing data for this module
        existing_data = self._get_module_existing_data(module_name, existing_profile)

        # If there's existing data, use LLM to generate smarter question
        if existing_data:
            question = self._generate_smart_opening(
                module_name=module_name,
                existing_data=existing_data
            )
        else:
            # Use default opening question
            question = self.DEFAULT_OPENERS.get(
                module_name,
                self._get_fallback_opener(module_name)
            )

        return QuestionResult(
            question=question,
            question_type="opening",
            target_fields=get_required_fields(module_name)
        )

    def generate_follow_up(
        self,
        module: ProfileModule,
        conversation_history: List[Dict],
        collected_info: Dict[str, Any],
        analyzer_feedback: AnalysisResult,
        language: str = "en"
    ) -> QuestionResult:
        """
        Generate follow-up question based on analyzer feedback

        Args:
            module: Current module
            conversation_history: Conversation history
            collected_info: Collected information
            analyzer_feedback: AnalyzerAgent analysis result
            language: Language (always "en")

        Returns:
            QuestionResult: Contains follow-up question and metadata
        """
        module_name = module.value

        # Build prompt
        prompt = self._build_follow_up_prompt(
            module_name=module_name,
            conversation_history=conversation_history,
            collected_info=collected_info,
            analyzer_feedback=analyzer_feedback
        )

        try:
            messages = [
                {"role": "system", "content": self._get_system_prompt()},
                {"role": "user", "content": prompt}
            ]

            # Use streaming for slow models like DeepSeek-R1
            if hasattr(self.llm, 'stream_chat'):
                question_parts = []
                for chunk in self.llm.stream_chat(messages, temperature=0.7):
                    question_parts.append(chunk)
                question = "".join(question_parts)
            elif hasattr(self.llm, 'chat'):
                question = self.llm.chat(messages, temperature=0.7)
            else:
                question = self.llm.complete(prompt)

            # Clean response - extract only the question part
            question = self._extract_question_from_response(question.strip())

            return QuestionResult(
                question=question,
                question_type="follow_up",
                context_reference=analyzer_feedback.reasoning,
                target_fields=self._infer_target_fields(analyzer_feedback)
            )

        except Exception as e:
            logger.error(f"Error generating follow-up: {e}", exc_info=True)
            # Use fallback question
            return self._get_fallback_follow_up(analyzer_feedback)

    def _extract_question_from_response(self, response: str) -> str:
        """Extract the actual question from LLM response, removing thinking tags."""
        import re

        # Remove <think>...</think> blocks (DeepSeek-R1 reasoning)
        response = re.sub(r'<think>.*?</think>', '', response, flags=re.DOTALL)

        # Remove other common reasoning markers
        response = re.sub(r'\[thinking\].*?\[/thinking\]', '', response, flags=re.DOTALL | re.IGNORECASE)

        # Clean up whitespace
        response = response.strip()

        # If empty after cleaning, return a fallback
        if not response:
            return "Could you tell me more about that?"

        return response

    def generate_ask_more_items(
        self,
        module: ProfileModule,
        item_count: int,
        language: str = "en"
    ) -> QuestionResult:
        """
        Generate question asking if user has more items

        Args:
            module: Current module
            item_count: Number of items collected
            language: Language (always "en")

        Returns:
            QuestionResult: Confirmation question
        """
        module_name = module.value

        # Use default question
        question = self.DEFAULT_ASK_MORE.get(
            module_name,
            self._get_fallback_ask_more(module_name)
        )

        return QuestionResult(
            question=question,
            question_type="confirmation",
        )

    def prepare_follow_up_context(
        self,
        module: ProfileModule,
        conversation_history: List[Dict],
        collected_info: Dict[str, Any],
        user_response: str,
        language: str = "en"
    ) -> Dict[str, Any]:
        """
        Prepare follow-up context in parallel with Analyzer.
        This runs simultaneously with AnalyzerAgent.analyze_response() for speed optimization.

        Args:
            module: Current module
            conversation_history: Conversation history
            collected_info: Collected information
            user_response: User's latest response
            language: Language (always "en")

        Returns:
            Dict containing context for follow-up question generation:
            - possible_directions: List of possible follow-up directions
            - user_mentions: Key topics user mentioned
            - conversation_summary: Brief summary of conversation
        """
        module_name = module.value

        # Extract key topics from user response
        user_mentions = self._extract_user_mentions(user_response, module_name)

        # Identify possible follow-up directions based on schema
        required_fields = get_required_fields(module_name)
        all_fields = get_all_fields(module_name)

        # Determine which fields are still missing
        collected_keys = set(collected_info.keys()) if collected_info else set()
        missing_required = [f for f in required_fields if f not in collected_keys]
        missing_optional = [f for f in all_fields if f not in collected_keys and f not in required_fields]

        # Build possible follow-up directions
        possible_directions = []

        if missing_required:
            possible_directions.append(f"Ask about required fields: {', '.join(missing_required[:3])}")

        if missing_optional:
            possible_directions.append(f"Explore optional details: {', '.join(missing_optional[:3])}")

        # Add module-specific directions
        if module_name == "experience":
            if "achievements" not in collected_keys:
                possible_directions.append("Ask for quantified achievements and impact")
            if "technologies" not in collected_keys:
                possible_directions.append("Ask about technology stack used")
        elif module_name == "project":
            if "your_contribution" not in collected_keys:
                possible_directions.append("Ask about specific personal contribution")
            if "challenges" not in collected_keys:
                possible_directions.append("Ask about technical challenges faced")
        elif module_name == "education":
            if "activities" not in collected_keys:
                possible_directions.append("Ask about notable activities or achievements")

        # Generate conversation summary
        conversation_summary = self._summarize_conversation(conversation_history[-4:]) if conversation_history else ""

        return {
            "possible_directions": possible_directions,
            "user_mentions": user_mentions,
            "conversation_summary": conversation_summary,
            "missing_required": missing_required,
            "missing_optional": missing_optional[:5],  # Limit to top 5
        }

    def _extract_user_mentions(self, user_response: str, module_name: str) -> List[str]:
        """Extract key topics mentioned by user"""
        mentions = []
        response_lower = user_response.lower()

        # Module-specific keyword extraction
        if module_name == "experience":
            keywords = ["team", "lead", "manager", "develop", "build", "design", "optimize", "improve"]
            for kw in keywords:
                if kw in response_lower:
                    mentions.append(kw)
        elif module_name == "project":
            keywords = ["built", "created", "implemented", "designed", "led", "contributed"]
            for kw in keywords:
                if kw in response_lower:
                    mentions.append(kw)
        elif module_name == "skill":
            keywords = ["python", "java", "javascript", "react", "node", "sql", "aws", "docker"]
            for kw in keywords:
                if kw in response_lower:
                    mentions.append(kw)

        return mentions

    def _summarize_conversation(self, recent_messages: List[Dict]) -> str:
        """Generate brief summary of recent conversation"""
        if not recent_messages:
            return ""

        summary_parts = []
        for msg in recent_messages:
            role = "User" if msg.get("role") == "user" else "Advisor"
            content = msg.get("content", "")[:100]  # First 100 chars
            if content:
                summary_parts.append(f"{role}: {content}...")

        return " | ".join(summary_parts[-2:])  # Last 2 exchanges

    # ==================== Private Methods ====================

    def _get_module_existing_data(self, module_name: str, existing_profile: Dict) -> List:
        """Get existing data for module"""
        module_key_map = {
            "education": "educations",
            "experience": "experiences",
            "project": "projects",
            "skill": "skills",
            "certification": "certifications",
            "language": "languages",
        }
        key = module_key_map.get(module_name)
        if key and key in existing_profile:
            return existing_profile[key]
        return []

    def _generate_smart_opening(
        self,
        module_name: str,
        existing_data: List
    ) -> str:
        """Generate smart opening question using existing data"""
        # Format existing data summary
        existing_summary = self._summarize_existing_data(existing_data, module_name)

        # Get module fields
        fields = get_all_fields(module_name)
        fields_text = ", ".join(fields) if fields else "General information"

        prompt = OPENING_QUESTION_PROMPT.format(
            module_name=self._get_module_display_name(module_name),
            module_fields=fields_text,
            existing_profile_info=existing_summary
        )

        try:
            messages = [
                {"role": "system", "content": self._get_system_prompt()},
                {"role": "user", "content": prompt}
            ]

            if hasattr(self.llm, 'chat'):
                return self.llm.chat(messages, temperature=0.7).strip()
            else:
                return self.llm.complete(prompt).strip()

        except Exception as e:
            logger.error(f"Error generating smart opening: {e}")
            # Fall back to default question
            return self.DEFAULT_OPENERS.get(
                module_name,
                self._get_fallback_opener(module_name)
            )

    def _summarize_existing_data(self, data: List, module_name: str) -> str:
        """Generate summary of existing data"""
        if not data:
            return "No existing data"

        summaries = []
        for i, item in enumerate(data[:3]):  # Show max 3 items
            if module_name == "experience":
                company = item.get("company", "Unknown")
                title = item.get("title", "Unknown")
                summaries.append(f"- {company}: {title}")
            elif module_name == "education":
                school = item.get("school", "Unknown")
                degree = item.get("degree", "Unknown")
                summaries.append(f"- {school}: {degree}")
            elif module_name == "project":
                name = item.get("name", "Unknown")
                summaries.append(f"- {name}")
            else:
                # Generic format
                if isinstance(item, dict):
                    name = item.get("name", str(item))
                    summaries.append(f"- {name}")
                else:
                    summaries.append(f"- {item}")

        return "\n".join(summaries)

    def _build_follow_up_prompt(
        self,
        module_name: str,
        conversation_history: List[Dict],
        collected_info: Dict,
        analyzer_feedback: AnalysisResult
    ) -> str:
        """Build follow-up prompt"""
        # Format recent conversation
        recent = conversation_history[-4:]  # Last 2 turns
        recent_text = "\n".join([
            f"{'User' if m['role'] == 'user' else 'Advisor'}: {m['content']}"
            for m in recent
        ])

        # Format collected info
        collected_text = json.dumps(collected_info, ensure_ascii=False, indent=2) if collected_info else "{}"

        return FOLLOW_UP_PROMPT.format(
            module_name=self._get_module_display_name(module_name),
            recent_conversation=recent_text,
            collected_info=collected_text,
            missing_points=", ".join(analyzer_feedback.missing_points) or "None",
            follow_up_suggestions=", ".join(analyzer_feedback.follow_up_suggestions) or "None",
            quality_issues=", ".join(analyzer_feedback.quality_issues) or "None"
        )

    def _get_system_prompt(self) -> str:
        """Get system prompt (English only)"""
        return """You are a professional career advisor conducting a deep background collection interview.
Your questions should:
1. Be natural and friendly, like a conversation between friends
2. Be specific and targeted, not vague
3. Guide users to provide detailed, quantified information
4. Reference user's previous answers when appropriate
5. NEVER use any emojis - keep everything text-only"""

    def _get_module_display_name(self, module_name: str) -> str:
        """Get module display name"""
        return self.MODULE_NAMES.get(module_name, module_name.replace("_", " ").title())

    def _get_fallback_opener(self, module_name: str) -> str:
        """Get fallback opening question"""
        return f"Please tell me about your {self._get_module_display_name(module_name)}."

    def _get_fallback_ask_more(self, module_name: str) -> str:
        """Get fallback ask more question"""
        return "Anything else to add?"

    def _get_fallback_follow_up(self, analyzer_feedback: AnalysisResult) -> QuestionResult:
        """Get fallback follow-up question"""
        # Generate simple follow-up based on analysis feedback
        if analyzer_feedback.missing_points:
            missing = analyzer_feedback.missing_points[0]
            question = f"Could you tell me more about {missing}?"
        elif analyzer_feedback.follow_up_suggestions:
            suggestion = analyzer_feedback.follow_up_suggestions[0]
            question = suggestion
        else:
            question = "Could you elaborate more on that?"

        return QuestionResult(
            question=question,
            question_type="follow_up",
            target_fields=[]
        )

    def _infer_target_fields(self, analyzer_feedback: AnalysisResult) -> List[str]:
        """Infer target fields from analysis feedback"""
        target_fields = []

        # Infer from missing points
        field_keywords = {
            "achievement": ["achievements"],
            "tech": ["technologies"],
            "description": ["description"],
            "quantif": ["achievements"],
            "time": ["start_date", "end_date"],
            "date": ["start_date", "end_date"],
            "responsibility": ["description"],
            "role": ["title", "description"],
        }

        for missing in analyzer_feedback.missing_points:
            missing_lower = missing.lower()
            for keyword, fields in field_keywords.items():
                if keyword in missing_lower:
                    target_fields.extend(fields)

        return list(set(target_fields))
