CREATE TABLE weekly_menu_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_weekly_menu_plans_user_week UNIQUE (user_id, week_start_date),
    CONSTRAINT fk_weekly_menu_plans_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE weekly_menu_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    menu_date DATE NOT NULL,
    meal_type VARCHAR(24) NOT NULL,
    recipe_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_weekly_menu_items_plan_slot UNIQUE (plan_id, menu_date, meal_type),
    CONSTRAINT fk_weekly_menu_items_plan
        FOREIGN KEY (plan_id) REFERENCES weekly_menu_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_weekly_menu_items_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe_records(id) ON DELETE CASCADE
);

CREATE INDEX idx_weekly_menu_items_plan_date
    ON weekly_menu_items(plan_id, menu_date);

CREATE TABLE weekly_menu_shopping_checks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    ingredient_name VARCHAR(128) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_weekly_menu_shopping_checks_identity
        UNIQUE (user_id, plan_id, ingredient_name),
    CONSTRAINT fk_weekly_menu_shopping_checks_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_weekly_menu_shopping_checks_plan
        FOREIGN KEY (plan_id) REFERENCES weekly_menu_plans(id) ON DELETE CASCADE
);

CREATE INDEX idx_weekly_menu_shopping_checks_plan
    ON weekly_menu_shopping_checks(user_id, plan_id);
