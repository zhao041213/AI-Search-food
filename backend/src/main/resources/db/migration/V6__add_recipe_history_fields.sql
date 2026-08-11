ALTER TABLE search_logs ADD COLUMN meal_type VARCHAR(64);
ALTER TABLE search_logs ADD COLUMN goal VARCHAR(128);

ALTER TABLE recipe_records ADD COLUMN summary TEXT;
ALTER TABLE recipe_records ADD COLUMN effects TEXT;
ALTER TABLE recipe_records ADD COLUMN tips TEXT;
