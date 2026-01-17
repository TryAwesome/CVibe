"""
AI Engine Configuration
"""

import os
from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional


class Settings(BaseSettings):
    """AI Engine Settings - loaded from environment variables"""
    
    # gRPC Server
    grpc_port: int = 50051
    grpc_max_workers: int = 10
    
    # Redis (for session management)
    redis_url: str = "redis://localhost:6379"
    session_ttl_hours: int = 24
    
    # Feature Flags
    enable_streaming: bool = True
    enable_session_persistence: bool = True
    
    # Logging
    log_level: str = "INFO"
    
    model_config = SettingsConfigDict(
        env_file=".env",
        env_prefix="AI_ENGINE_",
        extra="ignore",  # 忽略额外的环境变量
    )


settings = Settings()
