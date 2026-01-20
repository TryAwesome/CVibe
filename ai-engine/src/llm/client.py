"""
LLM Client Configuration - 支持用户自定义 API Key、Model、BaseURL
支持语言模型和视觉模型分离配置，支持流式输出
"""

import os
import json
import base64
from typing import Optional, Iterator, Union, Any
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


class VisionConfig(BaseModel):
    """视觉模型配置 (可以独立于语言模型)"""
    provider: str = "openai"
    api_key: str
    model: str = "gpt-4o"
    base_url: Optional[str] = None
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

    def stream_chat(self, messages: list[dict], **kwargs) -> Iterator[str]:
        """流式聊天响应"""
        temperature = kwargs.get("temperature", self.config.temperature)
        max_tokens = kwargs.get("max_tokens", self.config.max_tokens)

        if self.config.provider == "anthropic":
            system_msg = next((m["content"] for m in messages if m["role"] == "system"), None)
            chat_messages = [m for m in messages if m["role"] != "system"]
            
            with self._client.messages.stream(
                model=self.config.model,
                max_tokens=max_tokens,
                system=system_msg or "",
                messages=chat_messages,
            ) as stream:
                for text in stream.text_stream:
                    yield text
        else:
            response = self._client.chat.completions.create(
                model=self.config.model,
                messages=messages,
                temperature=temperature,
                max_tokens=max_tokens,
                stream=True,
            )
            for chunk in response:
                if chunk.choices[0].delta.content:
                    yield chunk.choices[0].delta.content

    def parse_json(self, messages: list[dict], **kwargs) -> dict:
        """解析 JSON 响应"""
        response = self.chat(messages, **kwargs)
        # Try to extract JSON from response
        try:
            # Handle markdown code blocks
            if "```json" in response:
                json_str = response.split("```json")[1].split("```")[0].strip()
            elif "```" in response:
                json_str = response.split("```")[1].split("```")[0].strip()
            else:
                json_str = response.strip()
            return json.loads(json_str)
        except json.JSONDecodeError:
            return {"raw_response": response}


class VisionClient:
    """
    视觉模型客户端，用于图像分析任务
    支持: OpenAI GPT-4V, Claude 3 Vision
    """
    
    def __init__(self, config: VisionConfig):
        self.config = config
        self._client = None
        self._init_client()
    
    def _init_client(self):
        if self.config.provider == "anthropic":
            self._client = Anthropic(api_key=self.config.api_key)
        else:
            self._client = OpenAI(
                api_key=self.config.api_key,
                base_url=self.config.base_url,
            )
    
    def analyze_image(self, image_data: Union[str, bytes], prompt: str, **kwargs) -> str:
        """
        分析图像
        
        Args:
            image_data: 图像URL、base64字符串或bytes数据
            prompt: 分析提示词
        
        Returns:
            分析结果文本
        """
        max_tokens = kwargs.get("max_tokens", self.config.max_tokens)
        
        # 处理不同格式的图像数据
        if isinstance(image_data, bytes):
            image_b64 = base64.b64encode(image_data).decode()
            image_url = f"data:image/png;base64,{image_b64}"
        elif image_data.startswith("data:"):
            image_url = image_data
        else:
            image_url = image_data
        
        if self.config.provider == "anthropic":
            import httpx
            
            # Anthropic requires base64 data
            if image_url.startswith("data:"):
                media_type = image_url.split(";")[0].split(":")[1]
                image_b64 = image_url.split(",")[1]
            else:
                # Download image
                response = httpx.get(image_url)
                media_type = response.headers.get("content-type", "image/jpeg")
                image_b64 = base64.b64encode(response.content).decode()
            
            response = self._client.messages.create(
                model=self.config.model,
                max_tokens=max_tokens,
                messages=[{
                    "role": "user",
                    "content": [
                        {
                            "type": "image",
                            "source": {
                                "type": "base64",
                                "media_type": media_type,
                                "data": image_b64,
                            }
                        },
                        {
                            "type": "text",
                            "text": prompt,
                        }
                    ]
                }]
            )
            return response.content[0].text
        else:
            # OpenAI Vision API
            response = self._client.chat.completions.create(
                model=self.config.model,
                messages=[{
                    "role": "user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {"type": "image_url", "image_url": {"url": image_url}},
                    ]
                }],
                max_tokens=max_tokens,
            )
            return response.choices[0].message.content


def create_llm_client(
    api_key: str,
    model: str = "gpt-4o",
    provider: str = "openai",
    base_url: Optional[str] = None,
) -> LLMClient:
    """工厂方法：创建 LLM 客户端"""
    config = LLMConfig(
        provider=provider,
        api_key=api_key,
        model=model,
        base_url=base_url,
    )
    return LLMClient(config)


def create_vision_client(
    api_key: str,
    model: str = "gpt-4o",
    provider: str = "openai",
    base_url: Optional[str] = None,
) -> VisionClient:
    """工厂方法：创建视觉模型客户端"""
    config = VisionConfig(
        provider=provider,
        api_key=api_key,
        model=model,
        base_url=base_url,
    )
    return VisionClient(config)


def get_default_llm_config() -> Optional[LLMConfig]:
    """从环境变量获取默认 LLM 配置"""
    api_key = os.getenv("AI_LLM_API_KEY") or os.getenv("OPENAI_API_KEY")
    if not api_key:
        return None
    
    return LLMConfig(
        provider=os.getenv("AI_LLM_PROVIDER", "openai"),
        api_key=api_key,
        model=os.getenv("AI_LLM_MODEL", "gpt-4o"),
        base_url=os.getenv("AI_LLM_BASE_URL") or None,
    )


def get_default_vision_config() -> Optional[VisionConfig]:
    """从环境变量获取默认视觉模型配置"""
    api_key = os.getenv("AI_VISION_API_KEY")
    if not api_key:
        # 如果没有单独配置视觉模型，则使用 LLM 配置
        api_key = os.getenv("AI_LLM_API_KEY") or os.getenv("OPENAI_API_KEY")
        if not api_key:
            return None

        return VisionConfig(
            provider=os.getenv("AI_LLM_PROVIDER", "openai"),
            api_key=api_key,
            model=os.getenv("AI_LLM_MODEL", "gpt-4o"),
            base_url=os.getenv("AI_LLM_BASE_URL") or None,
        )

    return VisionConfig(
        provider=os.getenv("AI_VISION_PROVIDER", "openai"),
        api_key=api_key,
        model=os.getenv("AI_VISION_MODEL", "gpt-4o"),
        base_url=os.getenv("AI_VISION_BASE_URL") or None,
    )


def get_reasoning_llm_config() -> Optional[LLMConfig]:
    """从环境变量获取推理模型配置 (DeepSeek-R1 for resume parsing)"""
    api_key = os.getenv("AI_REASONING_API_KEY")
    if not api_key:
        # Fallback to default LLM if reasoning model not configured
        return get_default_llm_config()

    return LLMConfig(
        provider=os.getenv("AI_REASONING_PROVIDER", "custom"),
        api_key=api_key,
        model=os.getenv("AI_REASONING_MODEL", "deepseek-ai/DeepSeek-R1"),
        base_url=os.getenv("AI_REASONING_BASE_URL") or None,
    )
