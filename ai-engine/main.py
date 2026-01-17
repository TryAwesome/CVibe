#!/usr/bin/env python3
"""
AI Engine - Main Entry Point
"""

import sys
import os

# Load .env file
from dotenv import load_dotenv
load_dotenv()

# Add src to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from src.grpc_server.server import serve
from src.config import settings
from src.llm.client import get_default_llm_config, get_default_vision_config


if __name__ == "__main__":
    port = int(os.getenv("GRPC_PORT", settings.grpc_port))
    
    # Print configuration info
    print("=" * 60)
    print("AI Engine Starting...")
    print("=" * 60)
    
    llm_config = get_default_llm_config()
    vision_config = get_default_vision_config()
    
    if llm_config:
        print(f"LLM Provider: {llm_config.provider}")
        print(f"LLM Model: {llm_config.model}")
        print(f"LLM Base URL: {llm_config.base_url or 'default'}")
    else:
        print("LLM: Not configured (will use user's API key)")
    
    if vision_config:
        print(f"Vision Provider: {vision_config.provider}")
        print(f"Vision Model: {vision_config.model}")
        print(f"Vision Base URL: {vision_config.base_url or 'default'}")
    else:
        print("Vision: Not configured")
    
    print("=" * 60)
    print(f"gRPC Server starting on port {port}...")
    print("=" * 60)
    
    serve(port)
