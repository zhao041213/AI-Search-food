ALTER TABLE ai_model_configs
    ADD COLUMN endpoint_url VARCHAR(1000);

ALTER TABLE ai_model_configs
    ADD COLUMN api_key VARCHAR(1000);
