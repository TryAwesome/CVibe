-- V12: Add Vision Model Configuration Fields
-- Separates Language Model and Vision Model configurations

-- Add vision model columns to user_ai_configs table
ALTER TABLE user_ai_configs 
ADD COLUMN IF NOT EXISTS vision_base_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS vision_api_key_encrypted TEXT,
ADD COLUMN IF NOT EXISTS vision_model_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS vision_provider VARCHAR(50);

-- Add comments for documentation
COMMENT ON COLUMN user_ai_configs.vision_base_url IS 'Vision Model API base URL (e.g., https://api.openai.com/v1)';
COMMENT ON COLUMN user_ai_configs.vision_api_key_encrypted IS 'Vision Model API key (encrypted)';
COMMENT ON COLUMN user_ai_configs.vision_model_name IS 'Vision Model name (e.g., gpt-4o, gemini-pro-vision)';
COMMENT ON COLUMN user_ai_configs.vision_provider IS 'Vision Model provider (openai, anthropic, google, custom)';
