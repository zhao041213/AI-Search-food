# DeepSeek Food Demo Design

## Goal

Build a minimal Spring Boot 3 demo that verifies a recipe search workflow and a real DeepSeek-backed recipe generation workflow.

## Scope

- Provide a static browser page at `/`.
- Provide `GET /api/recipes/search?keyword=...` for searching built-in demo recipes.
- Provide `POST /api/recipes/generate` for generating a recipe from ingredients through the DeepSeek Chat Completions API.
- Read the DeepSeek key from `DEEPSEEK_API_KEY`; never store it in source files.
- Return clear JSON errors when input is missing, the API key is missing, or DeepSeek fails.

## Architecture

The app is a single Spring Boot module. Controllers expose HTTP endpoints and delegate to services. `RecipeService` owns local demo search data. `DeepSeekRecipeService` owns request construction, API authentication, and response parsing for DeepSeek.

## Components

- `FoodApplication`: Spring Boot entry point.
- `RecipeController`: REST endpoints for search and generation.
- `RecipeService`: in-memory recipe search.
- `DeepSeekRecipeService`: DeepSeek HTTP client using `RestTemplate`.
- `Recipe`: simple response model for built-in recipes.
- `GenerateRequest`: request body model for AI generation.
- `index.html`: simple UI for manual verification.

## Data Flow

Search flow: browser sends a keyword, `RecipeController` calls `RecipeService`, and the service returns matching built-in recipes.

Generation flow: browser sends ingredients, `RecipeController` validates the request, `DeepSeekRecipeService` builds a Chinese cooking prompt, calls DeepSeek, extracts the first assistant message, and returns it as text.

## Error Handling

- Blank search keyword returns all demo recipes.
- Blank ingredients return HTTP 400.
- Missing `DEEPSEEK_API_KEY` returns HTTP 500 with a setup message.
- DeepSeek HTTP or parsing failures return HTTP 502 with a user-readable message.

## Testing

Unit and MVC tests cover local recipe search, missing ingredient validation, and DeepSeek request behavior with a mocked `RestTemplate`. Build verification uses Maven tests. Manual verification uses the static page at `http://localhost:8080`.
