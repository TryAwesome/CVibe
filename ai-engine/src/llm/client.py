"""
LLM Client Configuration - 支持用户自定义 API Key、Model、BaseURL
支持语言模型和视觉模型分离配置
"""

import os
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

    def stream_chat(self, messages: list[dict], **kwargs):
        """流式聊天响应"""
        # TODO: 实现流式响应
        pass


class VisionClient:
    """
    视觉模型客户端，用于图像分析任务
    支持: OpenAI GPT-4V, Claude 3 Vision, Gemini Pro Vision
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
    
    def analyze_image(self, image_url: str, prompt: str, **kwargs) -> str:
        """
        分析图像
        
        Args:
            image_url: 图像URL或base64数据
            prompt: 分析提示词
        
        Returns:
            分析结果文本
        """
        max_tokens = kwargs.get("max_tokens", self.config.max_tokens)
        
        if self.config.provider == "anthropic":
            # Anthropic Claude Vision
            import base64
            import httpx
            
            # 处理图像数据
            if image_url.startswith("data:"):
                # Base64 数据
                media_type = image_url.split(";")[0].split(":")[1]
                image_data = image_url.split(",")[1]
            else:
                # URL - 需要下载
                response = httpx.get(image_url)
                media_type = response.headers.get("content-type", "image/jpeg")
                image_data = base64.b64encode(response.content).decode()
            
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
                                "data": image_data,
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


def create_vision_client(
    api_key: str,
    model: str = "gpt-4o",
    provider: str = "openai",
    base_url: Optional[str] = None,
) -> VisionClient:
    """
    工厂方法：创建视觉模型客户端
    
    用于图像分析任务，如简历图片解析
    """
    config = VisionConfig(
        provider=provider,
        api_key=api_key,
        model=model,
        base_url=base_url,
    )
    return VisionClient(config)


def get_default_llm_config() -> Optional[LLMConfig]:
    """从环境变量获取默认 LLM 配置"""
    api_key = os.getenv("AI_LLM_API_KEY")
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
        api_key = os.getenv("AI_LLM_API_KEY")
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
