ALTER TABLE ai_model_configs
    ADD COLUMN api_protocol VARCHAR(32) NOT NULL DEFAULT 'openai';
