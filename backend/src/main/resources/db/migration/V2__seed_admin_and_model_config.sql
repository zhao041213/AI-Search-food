INSERT INTO admins (username, password_hash, nickname, role, enabled)
VALUES (
    'admin',
    '$2a$10$2lvuuqfw/LzUavFxPZQMfeii910kX1Yz7JXSH5yIOqNkSdgtoNt4K',
    'Administrator',
    'ADMIN',
    0
);

INSERT INTO ai_model_configs (provider, model_name, purpose, primary_model, enabled)
VALUES
    ('qwen', 'qwen-plus', 'text_recipe', 1, 1),
    ('qwen', 'qwen-vl-plus', 'vision', 1, 1),
    ('deepseek', 'deepseek-v4-flash', 'text_recipe', 0, 1);

INSERT INTO prompt_templates (template_key, template_name, content, enabled)
VALUES
    ('recipe_search', 'AI recipe search', 'Generate a structured recipe from user ingredients, preferences, and cooking goals.', 1),
    ('image_ingredient_recognition', 'Image ingredient recognition', 'Identify visible food ingredients from the uploaded image.', 1),
    ('finished_dish_review', 'Finished dish review', 'Evaluate the finished dish image from color, doneness, plating, and improvement suggestions.', 1);
