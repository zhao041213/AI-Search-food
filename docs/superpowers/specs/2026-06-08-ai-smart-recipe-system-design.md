# AI Smart Recipe Search System Design

Date: 2026-06-08

## Goal

Rebuild the current demo into a formal graduation project: an AI-first smart recipe search and recommendation system based on multimodal large models.

The homepage is centered on AI recipe search. Users can enter ingredients, dish names, or natural-language cooking goals, or upload/take a photo for ingredient recognition. The system generates structured recipe recommendations and supports nutrition advice, recommendation reasoning, ingredient substitution, full shopping lists with purchase links, video references, step-by-step cooking mode, regeneration, favorites, history, and finished-dish image evaluation.

## Product Positioning

The product is not a traditional local recipe search app. Local recipe search is an auxiliary feature and fallback. The core experience is:

1. User asks the AI what to cook.
2. The system understands text or image input.
3. The system generates a structured, personalized recipe.
4. The system records the AI search and recipe result.
5. The user can continue with shopping, cooking, regeneration, collection, and finished-dish review.

## Rebuild Strategy

Use a full rebuild instead of extending the current demo.

Target repository structure:

```text
AI-Search-food/
  backend/
    pom.xml
    src/main/java/com/example/food/
    src/main/resources/
  frontend/
    package.json
    src/
  docs/
  AGENTS.md
```

Keep `AGENTS.md` and project documentation. The existing demo code can be used as reference only, especially the DeepSeek request logic, but it is not the target architecture.

## Technology Stack

Backend:

- Java 17
- Spring Boot 3
- MySQL 8
- MyBatis Plus
- Spring Security
- JWT authentication
- Qwen text model as the primary AI model
- Qwen VL model for image understanding
- DeepSeek text model as a fallback model
- Local file storage for uploaded images, with an interface that can later be replaced by OSS

Frontend:

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts
- lucide-vue-next

## AI Model Strategy

Qwen is the primary model provider.

- Qwen text model: AI recipe search, structured recipe generation, recommendation reasoning, nutrition advice, substitution suggestions, shopping-list generation, recipe regeneration, and video keyword generation.
- Qwen VL model: ingredient image recognition and finished-dish image evaluation.
- DeepSeek text model: fallback for text recipe generation when Qwen text generation is unavailable or manually switched by an administrator.

The backend must hide model-specific details behind service interfaces so business logic does not depend directly on one provider.

Suggested backend AI components:

- `AiRecipeService`: main business entry for AI recipe search and regeneration.
- `VisionRecognitionService`: image ingredient recognition and finished-dish review.
- `QwenTextClient`: Qwen text API adapter.
- `QwenVisionClient`: Qwen vision API adapter.
- `DeepSeekTextClient`: DeepSeek text fallback adapter.
- `PromptTemplateService`: loads prompt templates from MySQL.
- `AiModelConfigService`: reads active model configuration from MySQL.

## Core User Features

### AI Recipe Search

The homepage provides one main AI search box. Users can type ingredients, dish names, or natural-language goals, such as:

- "tomato egg"
- "I want a low-fat dinner"
- "recommend something suitable for elderly people"
- "what can I cook with potatoes and beef"

The backend records the query, calls the AI model, saves the generated result, and returns a structured recipe response.

### Image Ingredient Recognition

Users can upload a photo or use a camera capture flow. The backend stores the image, calls Qwen VL to identify ingredients, then passes recognized ingredients into the AI recipe search flow.

### Personalized Diet Preferences

Users can save preferences:

- weight loss
- muscle gain
- light flavor
- low salt
- home-style cooking
- regional flavor preferences
- disliked ingredients
- allergy notes
- servings

Preferences are included in AI prompts when generating recipes.

### Recipe Result Display

Each generated recipe should include:

- title
- difficulty
- cooking time
- servings
- taste tags
- ingredients
- cooking steps
- recommendation reason
- nutrition advice
- suitable groups
- caution notes
- ingredient substitutions
- full shopping list
- video reference links or search keywords

### Full Shopping List With Purchase Links

The shopping list must include all ingredients used by the recipe, not only missing items.

Each shopping item includes:

- ingredient name
- amount
- category: main ingredient, side ingredient, seasoning, optional item
- whether the user already owns it
- whether buying is recommended
- substitute ingredient names
- Taobao search link
- JD search link

Purchase links are generated from ingredient names using search URLs. The system does not integrate real payment, product inventory, or e-commerce orders.

### Recipe Regeneration

Users can regenerate a recipe with preset actions:

- make it simpler
- make it healthier
- use only current pantry ingredients
- change to low-fat version
- change flavor
- adjust servings

The regeneration result is saved as a new recipe record and linked to the original recipe when practical.

### Step-by-Step Cooking Mode

Recipe details provide a guided cooking mode. Each step card shows:

- step number
- step title
- instruction
- estimated minutes
- cooking tip
- previous and next controls

### Finished-Dish Image Evaluation

Users can upload a finished-dish image. Qwen VL evaluates:

- color
- doneness
- plating
- likely issues
- improvement suggestions

The review result is saved and linked to the user and recipe.

### User History, Favorites, And Pantry

Users can:

- view AI search history
- view generated recipe history
- favorite recipes
- manage pantry ingredients
- use pantry data to mark shopping-list items as already owned

## Admin Features

Administrators log in with username and password.

Admin pages:

- dashboard overview
- user management
- AI search logs
- AI recipe records
- popular ingredient charts
- popular recipe charts
- search trend charts
- prompt template management
- AI model configuration and switching
- upload and vision recognition records
- finished-dish review records

Admin model switching controls whether Qwen or DeepSeek is used for text generation. Vision features remain Qwen VL based unless a new vision provider is added later.

## Authentication And Authorization

Use Spring Security with JWT.

Roles:

- `USER`: normal user
- `ADMIN`: administrator

Login modes:

- Normal user: phone number plus simulated verification code.
- Administrator: username plus password.

JWT is returned after login. The frontend stores it in Pinia and browser storage, then sends it with:

```http
Authorization: Bearer <token>
```

Public access:

- basic homepage browsing
- optional anonymous AI search

Authenticated user access:

- history
- favorites
- pantry
- personalized preferences
- finished-dish review records

Admin access:

- all `/api/admin/**` endpoints
- detailed statistics
- prompt and model configuration

## Persistence Rules

All AI searches and generated recipe results are saved to MySQL.

If the user is logged in, records are linked to `user_id`. If the user is anonymous, records use a null `user_id` and may include an anonymous session identifier.

This supports:

- search history
- generated recipe history
- favorites
- popular ingredient charts
- popular recipe charts
- admin auditing
- graduation project screenshots and analysis

## Database Design

Core tables:

- `users`
- `admins`
- `user_preferences`
- `user_pantry_items`
- `search_logs`
- `recipe_records`
- `recipe_ingredients`
- `recipe_steps`
- `shopping_items`
- `recipe_favorites`
- `finished_dish_reviews`
- `prompt_templates`
- `ai_model_configs`
- `uploaded_files`

### Table Responsibilities

`users` stores normal user accounts and phone login information.

`admins` stores administrator accounts and password hashes.

`user_preferences` stores diet preferences, dislikes, allergies, and serving preferences.

`user_pantry_items` stores ingredients the user already owns.

`search_logs` stores all text, image, and regeneration search inputs.

`recipe_records` stores AI-generated recipe summaries and AI metadata.

`recipe_ingredients` stores structured recipe ingredients.

`recipe_steps` stores step-by-step cooking instructions.

`shopping_items` stores full shopping-list entries and generated purchase links.

`recipe_favorites` stores user recipe collections.

`finished_dish_reviews` stores image review results from Qwen VL.

`prompt_templates` stores configurable prompt templates.

`ai_model_configs` stores model provider, model name, purpose, primary flag, and enabled flag.

`uploaded_files` stores uploaded image metadata.

## API Surface

Authentication:

- `POST /api/auth/user/code`
- `POST /api/auth/user/login`
- `POST /api/auth/admin/login`
- `GET /api/auth/me`

AI search:

- `POST /api/ai/search`
- `POST /api/ai/image-search`
- `POST /api/ai/recipes/{id}/regenerate`
- `POST /api/ai/recipes/{id}/finished-review`

Recipe and user features:

- `GET /api/user/recipes/history`
- `GET /api/user/search/history`
- `POST /api/user/recipes/{id}/favorite`
- `DELETE /api/user/recipes/{id}/favorite`
- `GET /api/user/favorites`
- `GET /api/user/pantry`
- `POST /api/user/pantry`
- `PUT /api/user/pantry/{id}`
- `DELETE /api/user/pantry/{id}`
- `GET /api/user/preferences`
- `PUT /api/user/preferences`

Statistics:

- `GET /api/stats/hot-ingredients`
- `GET /api/stats/hot-recipes`
- `GET /api/stats/search-trend`

Admin:

- `GET /api/admin/dashboard`
- `GET /api/admin/users`
- `GET /api/admin/search-logs`
- `GET /api/admin/recipe-records`
- `GET /api/admin/uploads`
- `GET /api/admin/finished-reviews`
- `GET /api/admin/prompt-templates`
- `PUT /api/admin/prompt-templates/{id}`
- `GET /api/admin/model-configs`
- `PUT /api/admin/model-configs/{id}`

## Frontend Design

The frontend uses a search-first product pattern. The homepage should emphasize AI search as the primary action.

Main user pages:

- login page
- AI search homepage
- AI recipe result page
- recipe detail page
- cooking step mode page
- user history page
- favorites page
- pantry page
- preferences page
- shopping-list view
- finished-dish review view

Admin pages:

- admin login page
- admin dashboard
- user management
- search logs
- recipe records
- prompt templates
- model configs
- upload records
- finished-dish review records

UI direction:

- health-tech style
- high contrast
- cyan and green as primary accents
- Element Plus components
- lucide icons for actions
- ECharts for ranking and trend charts
- avoid marketing-style landing pages
- keep the homepage functional on the first screen

## Statistics And Charts

Charts:

- hot ingredients: bar chart
- hot recipes: bar chart
- search trend: line chart
- user activity: line or summary cards
- input type distribution: pie or donut chart

Statistics are based on `search_logs`, `recipe_records`, and `recipe_favorites`.

## Error Handling

Handle these cases:

- empty search input
- invalid uploaded image type
- image too large
- AI provider timeout
- AI response is not valid JSON
- Qwen text generation failure
- Qwen VL recognition failure
- DeepSeek fallback failure
- unauthenticated access
- forbidden admin access
- duplicate favorite
- missing recipe record
- database write failure

AI response parsing should fail with a clear message and save enough context for debugging without storing API keys.

## Testing Strategy

Backend tests:

- authentication and JWT tests
- role-based authorization tests
- AI search service tests with mocked model clients
- structured AI response parsing tests
- shopping-link generation tests
- pantry ownership matching tests
- search-log persistence tests
- admin statistics tests

Frontend tests and checks:

- login flow
- AI search form validation
- recipe result rendering
- shopping-list rendering
- route guard behavior
- admin chart rendering smoke checks

Verification commands after implementation:

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm run build
```

## Implementation Phases

1. Rebuild repository structure with `backend/` and `frontend/`.
2. Add backend dependencies, MySQL configuration, MyBatis Plus, and base response format.
3. Add Spring Security, JWT, user login, and admin login.
4. Add database schema and core entities.
5. Add Qwen text, Qwen vision, and DeepSeek fallback client interfaces.
6. Add AI recipe search and recipe persistence.
7. Add Vue frontend skeleton, routing, layout, and login pages.
8. Add AI search homepage and result display.
9. Add recipe detail, shopping list, regeneration, and step mode.
10. Add image ingredient recognition and finished-dish review.
11. Add user history, favorites, pantry, and preferences.
12. Add admin dashboard, charts, logs, prompt templates, and model switching.
13. Remove or archive old demo files after the new structure is functional.
14. Update README and deployment documentation.

## Risks And Constraints

- Qwen and DeepSeek APIs require valid credentials and may fail due to quota, network, or model response format issues.
- Video references should use generated search links or public links instead of relying on third-party video platform APIs.
- Purchase links should use generated search URLs, not real e-commerce order integration.
- Phone login uses simulated verification codes for the graduation project and can be replaced by a real SMS provider later.
- The system should avoid medical claims. Nutrition content should be framed as dietary advice, not diagnosis or treatment.

## Acceptance Criteria

- The repository is rebuilt into a clean frontend/backend structure.
- Users can log in with phone and simulated code.
- Admins can log in with username and password.
- Users can search recipes through the AI-first homepage.
- Users can upload images for ingredient recognition.
- AI-generated recipes are structured and saved to MySQL.
- Recipe results include reason, nutrition advice, substitutions, shopping list, purchase links, video references, and steps.
- Users can regenerate, favorite, view history, manage pantry, and review finished dishes.
- Admins can view users, search logs, AI recipe records, statistics charts, prompt templates, and model configs.
- Qwen is the primary model and DeepSeek is available as text fallback.
- Backend tests and frontend build pass before completion claims.
