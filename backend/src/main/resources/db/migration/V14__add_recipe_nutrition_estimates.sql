ALTER TABLE recipe_records ADD COLUMN calories_kcal DECIMAL(10,2) NULL;
ALTER TABLE recipe_records ADD COLUMN protein_g DECIMAL(10,2) NULL;
ALTER TABLE recipe_records ADD COLUMN fat_g DECIMAL(10,2) NULL;
ALTER TABLE recipe_records ADD COLUMN carbohydrate_g DECIMAL(10,2) NULL;
ALTER TABLE recipe_records ADD COLUMN nutrition_source VARCHAR(32) NULL;
