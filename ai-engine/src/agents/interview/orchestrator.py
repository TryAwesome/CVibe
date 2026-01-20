"""
ProfileInterviewOrchestrator - Three Agent Coordinator
================================================================================

Core responsibilities:
- Manage interview session state
- Coordinate the three agents (Questioner, Analyzer, Summarizer)
- Handle flow control and module transitions
- Maintain conversation history and collected data

Architecture:
                    +-----------------------------------------------------+
                    |           ProfileInterviewOrchestrator              |
                    |   (Coordinator: state, agent dispatch, flow ctrl)   |
                    +-----------------------------------------------------+
                                            |
              +-----------------------------+-----------------------------+
              |                             |                             |
              v                             v                             v
    +------------------+        +------------------+        +------------------+
    |  QuestionerAgent |<------>|  AnalyzerAgent   |<------>|  SummarizerAgent |
    |   (Questioner)   |        |   (Analyzer)     |        |   (Summarizer)   |
    +------------------+        +------------------+        +------------------+

Parallel Execution:
- When user responds, Analyzer and Questioner start analysis in parallel
- Questioner waits for Analyzer's result to finalize the next question
"""

import json
import logging
import time
from concurrent.futures import ThreadPoolExecutor, as_completed, wait, FIRST_COMPLETED
from typing import Iterator, Optional, Dict, Any, List, Tuple

from .models import (
    ProfileModule,
    OrchestratorState,
    AnalysisResult,
    ModuleSummary,
    QuestionResult,
    merge_extracted_info,
)
from .analyzer_agent import AnalyzerAgent
from .questioner_agent import QuestionerAgent
from .summarizer_agent import SummarizerAgent
from .schemas import get_module_schema, get_required_fields

logger = logging.getLogger(__name__)

# Heartbeat interval for slow models (seconds)
HEARTBEAT_INTERVAL = 5


# ==================== Welcome Messages (English only, no emoji) ====================

WELCOME_MESSAGE = """Hello! I'm your career advisor.

I'll get to know your background thoroughly and in detail through our conversation to help you build a complete professional profile.

I'll collect information in this order:
Basic Info -> Education -> Work Experience -> Projects -> Skills -> Certifications -> Languages

For each section, I'll ask follow-up questions to ensure I get enough detailed information. After completing each section, I'll summarize it before moving on.

Ready? Let's begin!"""


class ProfileInterviewOrchestrator:
    """
    Profile Interview Orchestrator - Three Agent Coordinator

    Core design:
    - Manage session state (OrchestratorState)
    - Coordinate three Agents (Questioner, Analyzer, Summarizer)
    - Handle flow control (opening -> follow-up -> next item -> next module -> complete)
    - Parallel execution: Analyzer and Questioner work simultaneously
    """

    def __init__(self, llm_client, session_store=None):
        """
        Initialize Orchestrator

        Args:
            llm_client: LLM client
            session_store: Session storage
        """
        self.llm = llm_client
        self.session_store = session_store

        # Initialize three Agents
        self.questioner = QuestionerAgent(llm_client)
        self.analyzer = AnalyzerAgent(llm_client)
        self.summarizer = SummarizerAgent(llm_client)

        # Thread pool for parallel agent execution
        self.executor = ThreadPoolExecutor(max_workers=2)

        logger.info("ProfileInterviewOrchestrator initialized with 3 agents (parallel mode)")

    def start_session(
        self,
        user_id: str,
        session_id: str,
        language: str = "en",
        existing_profile: Optional[str] = None
    ) -> Tuple[OrchestratorState, str, str]:
        """
        Start a new interview session

        Args:
            user_id: User ID
            session_id: Session ID
            language: Language (always "en" for English)
            existing_profile: Existing Profile JSON (optional)

        Returns:
            (state, welcome_message, first_question)
        """
        # Force English
        language = "en"

        # Parse existing Profile
        existing_data = {}
        if existing_profile:
            try:
                existing_data = json.loads(existing_profile)
                logger.info(f"Loaded existing profile for user {user_id}")
            except json.JSONDecodeError:
                logger.warning("Failed to parse existing profile")

        # Initialize session state
        state = OrchestratorState(
            session_id=session_id,
            user_id=user_id,
            language=language,
            current_module=ProfileModule.BASIC_INFO,
            existing_profile=existing_data,
        )

        # Welcome message (English only)
        welcome = WELCOME_MESSAGE

        # Generate first question
        first_question_result = self.questioner.generate_opening_question(
            module=state.current_module,
            existing_profile=existing_data,
            language=language
        )
        first_question = first_question_result.question

        # Record opening conversation
        state.add_to_conversation("assistant", f"{welcome}\n\n{first_question}")
        state.current_question = first_question
        state.current_question_type = "opening"

        # Save session
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
        Process user message with parallel agent execution

        Core flow:
        1. Load session state
        2. Add user message to history
        3. [PARALLEL] AnalyzerAgent analyzes response + QuestionerAgent prepares
        4. QuestionerAgent waits for Analyzer result to finalize next question
        5. Based on analysis result:
           - is_sufficient=False -> QuestionerAgent generates follow-up
           - is_sufficient=True -> SummarizerAgent summarizes -> next module/complete

        Args:
            session_id: Session ID
            user_message: User message

        Yields:
            Response content chunks
        """
        # Load session
        logger.info(f"process_message called: session={session_id}, message_len={len(user_message)}")

        if not self.session_store:
            logger.error("Session store not configured")
            yield "Session store not configured."
            return

        state_dict = self.session_store.get(session_id)
        if not state_dict:
            logger.error(f"Session not found: {session_id}")
            yield "Session not found. Please start a new session."
            return

        logger.info(f"Session loaded successfully: {session_id}")
        state = OrchestratorState.from_dict(state_dict)
        language = "en"  # Force English

        # Add user message
        state.add_to_conversation("user", user_message)
        state.update_activity()

        # Check if in summary stage
        if state.current_module == ProfileModule.SUMMARY:
            response = self._handle_summary_confirmation(state, user_message)
            state.add_to_conversation("assistant", response)
            self.session_store.set(session_id, state.to_dict())
            yield response
            return

        # Handle user saying "no more"
        if self._user_says_no_more(user_message):
            response = self._handle_no_more_items(state)
            state.add_to_conversation("assistant", response)
            self.session_store.set(session_id, state.to_dict())
            yield response
            return

        # Handle user saying "have more"
        if self._user_says_has_more(user_message) and state.current_question_type == "confirmation":
            response = self._handle_has_more_items(state)
            state.add_to_conversation("assistant", response)
            self.session_store.set(session_id, state.to_dict())
            yield response
            return

        # Check for off-topic messages and politely decline
        if self._is_off_topic(user_message, state):
            response = self._get_polite_decline(state)
            state.add_to_conversation("assistant", response)
            self.session_store.set(session_id, state.to_dict())
            yield response
            return

        try:
            # ============================================================
            # PARALLEL EXECUTION with HEARTBEAT for slow models (DeepSeek-R1)
            # - Both agents start analyzing simultaneously
            # - Yield heartbeat messages while waiting
            # - Questioner prepares context and possible follow-up directions
            # - Analyzer evaluates response quality
            # ============================================================

            logger.info(f"Starting parallel execution for session {session_id}")

            # Yield initial thinking status
            yield "[THINKING] Analyzing your response..."

            # Prepare common context for both agents
            module = state.current_module
            conversation_history = state.get_current_module_conversation()
            collected_info = state.get_current_collected_info()
            current_question = state.current_question
            follow_up_count = state.follow_up_count

            logger.info(f"Submitting parallel tasks for module {module.value}")

            # Submit both tasks simultaneously
            analyzer_future = self.executor.submit(
                self.analyzer.analyze_response,
                module=module,
                current_question=current_question,
                user_response=user_message,
                collected_info=collected_info,
                follow_up_count=follow_up_count,
                language=language
            )

            questioner_future = self.executor.submit(
                self.questioner.prepare_follow_up_context,
                module=module,
                conversation_history=conversation_history,
                collected_info=collected_info,
                user_response=user_message,
                language=language
            )

            # Wait with heartbeat for slow models
            futures = {analyzer_future, questioner_future}
            heartbeat_count = 0
            while futures:
                done, futures = wait(futures, timeout=HEARTBEAT_INTERVAL, return_when=FIRST_COMPLETED)

                if not done and futures:
                    # Still waiting, send heartbeat
                    heartbeat_count += 1
                    thinking_messages = [
                        "[THINKING] Deep analysis in progress...",
                        "[THINKING] Evaluating your experience...",
                        "[THINKING] Processing information...",
                        "[THINKING] Preparing personalized questions...",
                    ]
                    yield thinking_messages[heartbeat_count % len(thinking_messages)]

            # Get results
            questioner_context = questioner_future.result()
            logger.info(f"Questioner context prepared: {len(questioner_context.get('possible_directions', []))} directions")

            analysis = analyzer_future.result()
            logger.info(f"Analyzer complete: is_sufficient={analysis.is_sufficient}, "
                       f"missing_points={len(analysis.missing_points)}")

            # Update collected info from analysis
            if analysis.extracted_info:
                state.update_collected_info(analysis.extracted_info)

            # ============================================================
            # Questioner generates question based on Analyzer's result
            # ============================================================

            # Yield status before generating follow-up
            yield "[THINKING] Formulating next question..."

            if analysis.is_sufficient:
                # Current item complete, ask if there are more or move to next module
                logger.info("Analysis sufficient, handling item complete")
                response = self._handle_item_complete(state, analysis)
            else:
                # Need follow-up - Questioner uses Analyzer's feedback
                logger.info("Analysis not sufficient, generating follow-up")
                response = self._handle_follow_up(state, analysis)

            logger.info(f"Generated response (length={len(response)}): {response[:100]}...")

            # Record response
            state.add_to_conversation("assistant", response)

            # Save state
            self.session_store.set(session_id, state.to_dict())

            # Yield final response
            yield response

        except Exception as e:
            logger.error(f"Error processing message: {e}", exc_info=True)
            yield "Sorry, there was an error. Please try again."

    def get_session_state(self, session_id: str) -> Optional[Dict[str, Any]]:
        """Get session state"""
        if not self.session_store:
            return None

        state_dict = self.session_store.get(session_id)
        if not state_dict:
            return None

        state = OrchestratorState.from_dict(state_dict)

        # Get progress info
        progress = state.get_progress_info()

        return {
            "session_id": state.session_id,
            "user_id": state.user_id,
            "current_module": state.current_module.value,
            "module_name": self._get_module_display_name(state.current_module.value),
            "turn_count": state.turn_count,
            "status": state.status,
            "progress": f"{progress['completed_modules']}/{progress['total_modules']}",
            "progress_percentage": progress['progress_percentage'],
            "extracted_data": {
                module: summary.structured_data
                for module, summary in state.module_summaries.items()
            },
        }

    def finish_session(self, session_id: str) -> Dict[str, Any]:
        """
        Finish interview and generate final Profile

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

        state = OrchestratorState.from_dict(state_dict)

        # Ensure current module is summarized
        self._ensure_current_module_summarized(state)

        # Call SummarizerAgent to synthesize final Profile
        profile = self.summarizer.synthesize_final_profile(
            module_summaries=state.module_summaries,
            full_conversation=state.conversation_history,
            language="en"
        )

        # Mark complete
        state.status = "COMPLETED"
        self.session_store.set(session_id, state.to_dict())

        logger.info(f"Finished session {session_id}, completeness: {profile.get('completeness_score', 0)}")

        return {
            "success": True,
            "profile": profile,
            "completeness_score": profile.get("completeness_score", 0),
            "missing_sections": profile.get("missing_sections", [])
        }

    # ==================== Private Methods ====================

    def _handle_follow_up(self, state: OrchestratorState, analysis: AnalysisResult) -> str:
        """Handle follow-up question generation"""
        state.follow_up_count += 1

        # QuestionerAgent generates follow-up based on Analyzer's feedback
        question_result = self.questioner.generate_follow_up(
            module=state.current_module,
            conversation_history=state.get_current_module_conversation(),
            collected_info=state.get_current_collected_info(),
            analyzer_feedback=analysis,
            language="en"
        )

        state.current_question = question_result.question
        state.current_question_type = "follow_up"

        return question_result.question

    def _handle_item_complete(self, state: OrchestratorState, analysis: AnalysisResult) -> str:
        """Handle item completion"""
        module_name = state.current_module.value

        # Check if this is a multi-item module
        multi_item_modules = ["education", "experience", "project", "skill", "certification", "language"]

        if module_name in multi_item_modules:
            # Ask if there are more items
            state.current_item_index += 1
            question_result = self.questioner.generate_ask_more_items(
                module=state.current_module,
                item_count=state.current_item_index,
                language="en"
            )

            state.current_question = question_result.question
            state.current_question_type = "confirmation"

            return question_result.question
        else:
            # Single-item module, advance to next
            return self._advance_to_next_module(state)

    def _handle_no_more_items(self, state: OrchestratorState) -> str:
        """Handle user saying no more items"""
        # Summarize current module
        self._summarize_current_module(state)

        # Advance to next module
        return self._advance_to_next_module(state)

    def _handle_has_more_items(self, state: OrchestratorState) -> str:
        """Handle user saying they have more items"""
        # Reset current item collection state
        state.follow_up_count = 0

        # Generate opening question for new item
        question_result = self.questioner.generate_opening_question(
            module=state.current_module,
            existing_profile=state.existing_profile,
            language="en"
        )

        state.current_question = question_result.question
        state.current_question_type = "opening"

        # Save previous info
        current_info = state.get_current_collected_info()
        if current_info:
            self._save_current_item(state)

        return question_result.question

    def _advance_to_next_module(self, state: OrchestratorState) -> str:
        """Advance to next module"""
        completed_module = state.current_module
        item_count = state.current_item_index + 1

        # Ensure current module is summarized
        self._ensure_current_module_summarized(state)

        if state.advance_to_next_module():
            # More modules to go
            next_module = state.current_module

            # Generate transition message
            transition = self._generate_module_transition(
                completed_module=completed_module,
                next_module=next_module,
                item_count=item_count
            )

            # Generate opening question for new module
            question_result = self.questioner.generate_opening_question(
                module=next_module,
                existing_profile=state.existing_profile,
                language="en"
            )

            state.current_question = question_result.question
            state.current_question_type = "opening"
            state.follow_up_count = 0
            state.current_item_index = 0

            return f"{transition}\n\n{question_result.question}"
        else:
            # All modules complete, enter summary
            state.current_module = ProfileModule.SUMMARY
            return self._generate_final_summary(state)

    def _summarize_current_module(self, state: OrchestratorState):
        """Summarize current module"""
        module_name = state.current_module.value

        # Get module conversation and collected info
        module_conversation = state.get_current_module_conversation()
        collected_info = state.get_current_collected_info()

        # Get existing data
        existing_data = self._get_module_existing_data(module_name, state.existing_profile)

        # Call SummarizerAgent
        summary = self.summarizer.summarize_module(
            module=state.current_module,
            module_conversation=module_conversation,
            collected_info=collected_info,
            existing_data=existing_data,
            language="en"
        )

        # Save summary
        state.complete_current_module(summary)

        logger.info(f"Module {module_name} summarized: {summary.item_count} items, score={summary.completeness_score}")

    def _ensure_current_module_summarized(self, state: OrchestratorState):
        """Ensure current module is summarized"""
        module_name = state.current_module.value

        if module_name not in state.module_summaries and module_name != "summary":
            self._summarize_current_module(state)

    def _save_current_item(self, state: OrchestratorState):
        """Save current item info"""
        # Can be extended to save each item separately
        # Current implementation summarizes at module end
        pass

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

    def _get_module_display_name(self, module_name: str) -> str:
        """Get display name for module (English)"""
        names = {
            "basic_info": "Basic Information",
            "education": "Education",
            "experience": "Work Experience",
            "project": "Projects",
            "skill": "Skills",
            "certification": "Certifications",
            "language": "Languages",
            "summary": "Summary",
        }
        return names.get(module_name, module_name.replace("_", " ").title())

    def _generate_module_transition(
        self,
        completed_module: ProfileModule,
        next_module: ProfileModule,
        item_count: int
    ) -> str:
        """Generate transition message between modules"""
        completed_name = self._get_module_display_name(completed_module.value)
        next_name = self._get_module_display_name(next_module.value)

        return (
            f"Great! We've completed the {completed_name} section "
            f"with {item_count} {'entry' if item_count == 1 else 'entries'}. "
            f"Now let's move on to {next_name}."
        )

    def _generate_final_summary(self, state: OrchestratorState) -> str:
        """Generate final summary"""
        all_summaries = state.get_all_summaries()

        summary = "Great! We've completed collecting all the information.\n\n"
        summary += "Here's a summary of what we collected:\n\n"

        for module_name, module_summary in all_summaries.items():
            module_display = self._get_module_display_name(module_name)
            count = module_summary.item_count
            summary += f"- {module_display}: {count} {'entry' if count == 1 else 'entries'}\n"

        summary += "\nIs this accurate? Anything to add or correct?"
        summary += "\n\nIf everything looks good, say 'Confirm' and I'll generate your complete profile."

        return summary

    def _handle_summary_confirmation(self, state: OrchestratorState, user_message: str) -> str:
        """Handle summary confirmation"""
        confirm_keywords = ["confirm", "yes", "ok", "looks good", "correct", "accurate", "done"]

        if any(kw in user_message.lower() for kw in confirm_keywords):
            state.status = "COMPLETED"
            return "Great! Your profile is now ready.\n\nInterview complete, thank you for your cooperation!"
        else:
            return "Sure, please tell me what you'd like to add or modify?"

    def _user_says_no_more(self, message: str) -> bool:
        """Check if user says no more - for confirmation questions"""
        msg_lower = message.lower().strip()

        # For longer messages (over 100 chars), likely not a simple confirmation
        if len(msg_lower) > 100:
            return False

        # Exact or near-exact matches for "no more" confirmation messages
        no_more_patterns = [
            # Explicit no
            "no", "nope", "none", "no more", "that's all", "nothing else",
            "done", "finished", "complete", "that is all", "nothing more",
            "no thanks", "i'm done", "that's it", "nothing", "all done",
            "not really", "no other", "don't have", "do not have",
            # Implicit no - variations of "that's everything"
            "that's everything", "that is everything", "those are all",
            "that covers it", "nothing to add", "no additional",
            # Short negative responses
            "nah", "na", "negative", "not any", "none more",
        ]

        # Check if message matches or starts with any pattern
        for pattern in no_more_patterns:
            if msg_lower == pattern or msg_lower.startswith(pattern + " ") or msg_lower.startswith(pattern + ".") or msg_lower.startswith(pattern + ",") or msg_lower.startswith(pattern + "!"):
                return True

        # Also check if the entire message is just a negative response with punctuation
        clean_msg = msg_lower.rstrip(".,!?")
        if clean_msg in no_more_patterns:
            return True

        return False

    def _user_says_has_more(self, message: str) -> bool:
        """Check if user says they have more - only for short confirmations"""
        # Only check for "yes more" patterns in SHORT messages (under 50 chars)
        msg_lower = message.lower().strip()

        if len(msg_lower) > 50:
            return False

        yes_keywords = [
            "yes", "yeah", "yep", "sure", "have more", "another",
            "one more", "additional", "more to add", "yes please",
            "i have more", "there's more", "got more"
        ]

        return any(msg_lower == kw or msg_lower.startswith(kw + " ") or msg_lower.startswith(kw + ".") or msg_lower.startswith(kw + ",") for kw in yes_keywords)

    def _is_off_topic(self, message: str, state: OrchestratorState) -> bool:
        """
        Check if user message is off-topic (unrelated to profile interview).

        Returns True ONLY for:
        - Very clear off-topic requests (weather, jokes, code help, etc.)
        - Should be conservative - when in doubt, return False
        """
        msg_lower = message.lower().strip()

        # Long messages are almost always content responses, not off-topic
        # Users don't write long off-topic messages
        if len(msg_lower) > 100:
            return False

        # Very short messages might be confirmations, not off-topic
        if len(msg_lower) < 15:
            return False

        # If message contains personal/project-related content, it's on-topic
        on_topic_indicators = [
            # Personal pronouns indicating they're talking about themselves
            "i ", "my ", "i'm", "i've", "i'd", "me ", "myself", "we ", "our ",
            # Project/work related
            "project", "built", "developed", "created", "designed", "implemented",
            "worked", "role", "responsible", "contribution", "team", "company",
            "experience", "skill", "technology", "used", "using",
            # Education related
            "studied", "university", "school", "degree", "major", "graduated",
            # Common response patterns
            "yes", "no", "sure", "absolutely", "here", "this is", "that was",
        ]

        if any(indicator in msg_lower for indicator in on_topic_indicators):
            return False

        # Only flag very clear off-topic patterns
        clear_off_topic_patterns = [
            # Very specific off-topic requests
            "tell me a joke", "sing a song", "play a game",
            "what's the weather", "what time is it",
            "write me a story", "write a poem",
            # Technical help completely unrelated to profile
            "debug this code", "fix this bug", "solve this algorithm",
        ]

        # Check if message matches clear off-topic patterns
        for pattern in clear_off_topic_patterns:
            if pattern in msg_lower:
                return True

        return False

    def _get_module_keywords(self, module_name: str) -> list:
        """Get keywords related to current module for context detection"""
        module_keywords = {
            "basic_info": ["name", "email", "phone", "location", "goal", "career", "profession", "job", "work"],
            "education": ["school", "university", "college", "degree", "major", "study", "graduate", "education", "gpa"],
            "experience": ["company", "job", "work", "role", "position", "responsibility", "team", "project", "employer"],
            "project": ["project", "build", "develop", "create", "application", "system", "software", "tool"],
            "skill": ["skill", "technology", "programming", "language", "framework", "tool", "proficiency", "expert"],
            "certification": ["certificate", "certification", "license", "credential", "exam", "certified"],
            "language": ["language", "speak", "fluent", "native", "proficiency", "english", "chinese", "spanish"],
        }
        return module_keywords.get(module_name, [])

    def _get_polite_decline(self, state: OrchestratorState) -> str:
        """Generate polite decline message for off-topic requests"""
        module_name = self._get_module_display_name(state.current_module.value)

        decline_messages = [
            f"I appreciate the question, but I'm here specifically to help collect your professional profile information. Let's stay focused on the {module_name} section. {state.current_question}",
            f"That's an interesting topic, but my role is to gather your background details for your profile. Let's continue with {module_name}. {state.current_question}",
            f"I'd love to help with that, but right now I'm focused on building your professional profile. Could we get back to discussing your {module_name}? {state.current_question}",
        ]

        # Use turn count to vary the response
        return decline_messages[state.turn_count % len(decline_messages)]
