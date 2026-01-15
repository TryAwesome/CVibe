"""
LLM Client Configuration - 支持用户自定义 API Key、Model、BaseURL
"""

from typing import Optional
from pydantic import BaseModel
from openai import OpenAI
from anthropic import Anthropic


class LLMConfig(BaseModel):
    """用户自定义的 LLM 配置"""
    provider: str = "openai"  # openai, anthropic, custom
    api_key: str
    model: str = "gpt-4o"
    base_url: Optional[str] = None  # 自定义 base_url
    temperature: float = 0.7
    max_tokens: int = 4096


class LLMClient:
    """
    统一的 LLM 客户端，支持：
    - OpenAI (GPT-4o, GPT-4, GPT-3.5)
    - Anthropic (Claude)
    - 自定义 API (兼容 OpenAI 格式)
    """

    def __init__(self, config: LLMConfig):
        self.config = config
        self._client = None
        self._init_client()

    def _init_client(self):
        if self.config.provider == "anthropic":
            self._client = Anthropic(api_key=self.config.api_key)
        else:
            # OpenAI 或自定义 (兼容 OpenAI API 格式)
            self._client = OpenAI(
                api_key=self.config.api_key,
                base_url=self.config.base_url,
            )

    def chat(self, messages: list[dict], **kwargs) -> str:
        """
        发送聊天请求
        
        Args:
            messages: [{"role": "user/assistant/system", "content": "..."}]
        
        Returns:
            AI 响应文本
        """
        temperature = kwargs.get("temperature", self.config.temperature)
        max_tokens = kwargs.get("max_tokens", self.config.max_tokens)

        if self.config.provider == "anthropic":
            # Anthropic Claude
            system_msg = next((m["content"] for m in messages if m["role"] == "system"), None)
            chat_messages = [m for m in messages if m["role"] != "system"]
            
            response = self._client.messages.create(
                model=self.config.model,
                max_tokens=max_tokens,
                system=system_msg or "",
                messages=chat_messages,
            )
            return response.content[0].text
        else:
            # OpenAI 或兼容 API
            response = self._client.chat.completions.create(
                model=self.config.model,
                messages=messages,
                temperature=temperature,
                max_tokens=max_tokens,
            )
            return response.choices[0].message.content

    def stream_chat(self, messages: list[dict], **kwargs):
        """流式聊天响应"""
        # TODO: 实现流式响应
        pass


def create_llm_client(
    api_key: str,
    model: str = "gpt-4o",
    provider: str = "openai",
    base_url: Optional[str] = None,
) -> LLMClient:
    """
    工厂方法：创建 LLM 客户端
    
    用户可以在 Settings 中配置自己的:
    - API Key
    - Model (gpt-4o, claude-3-opus, etc.)
    - Provider (openai, anthropic, custom)
    - Base URL (自定义 API 端点)
    """
    config = LLMConfig(
        provider=provider,
        api_key=api_key,
        model=model,
        base_url=base_url,
    )
    return LLMClient(config)
