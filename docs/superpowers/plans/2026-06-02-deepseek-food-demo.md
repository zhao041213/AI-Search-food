# DeepSeek Food Demo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a minimal Spring Boot 3 recipe search and DeepSeek recipe generation demo.

**Architecture:** A single Maven module exposes REST endpoints and serves one static HTML page. Local search uses in-memory demo recipes. AI generation uses `RestTemplate` to call DeepSeek Chat Completions with `DEEPSEEK_API_KEY` from the environment and defaults to `deepseek-v4-flash`.

**Tech Stack:** Java 17, Spring Boot 3.3.x, Maven, RestTemplate, JUnit 5, Mockito, static HTML/CSS/JS.

---

## File Structure

- Create `pom.xml`: Maven dependencies and Java 17 settings.
- Create `src/test/java/com/example/food/service/RecipeServiceTest.java`: local search behavior tests.
- Create `src/test/java/com/example/food/service/DeepSeekRecipeServiceTest.java`: DeepSeek request and parsing tests.
- Create `src/test/java/com/example/food/controller/RecipeControllerTest.java`: HTTP validation tests.
- Create `src/main/java/com/example/food/FoodApplication.java`: Spring Boot entry point and `RestTemplate` bean.
- Create `src/main/java/com/example/food/model/Recipe.java`: recipe response record.
- Create `src/main/java/com/example/food/model/GenerateRequest.java`: AI generation request record.
- Create `src/main/java/com/example/food/service/RecipeService.java`: built-in recipe search.
- Create `src/main/java/com/example/food/service/DeepSeekRecipeService.java`: DeepSeek API client.
- Create `src/main/java/com/example/food/service/DeepSeekConfigurationException.java`: missing API key exception.
- Create `src/main/java/com/example/food/service/DeepSeekApiException.java`: upstream API exception.
- Create `src/main/java/com/example/food/controller/RecipeController.java`: REST API.
- Create `src/main/resources/application.yml`: DeepSeek configuration from environment.
- Create `src/main/resources/static/index.html`: manual browser demo.
- Create `README.md`: run instructions without storing secrets.

### Task 1: Project Skeleton and RED Tests

**Files:**
- Create: `pom.xml`
- Create: `src/test/java/com/example/food/service/RecipeServiceTest.java`
- Create: `src/test/java/com/example/food/service/DeepSeekRecipeServiceTest.java`
- Create: `src/test/java/com/example/food/controller/RecipeControllerTest.java`

- [ ] **Step 1: Add Maven project configuration**

Create `pom.xml` with Spring Boot web and test dependencies.

- [ ] **Step 2: Write failing recipe search tests**

Test that blank keyword returns all demo recipes and keyword search can match recipe content.

- [ ] **Step 3: Write failing DeepSeek service tests**

Test that the service sends a bearer token, uses the configured model, and extracts the assistant response.

- [ ] **Step 4: Write failing controller validation test**

Test that blank ingredients return HTTP 400 with a JSON `error` field.

- [ ] **Step 5: Run tests and confirm RED**

Run: `mvn test`

Expected: FAIL because production classes do not exist yet.

### Task 2: Backend Implementation

**Files:**
- Create: `src/main/java/com/example/food/FoodApplication.java`
- Create: `src/main/java/com/example/food/model/Recipe.java`
- Create: `src/main/java/com/example/food/model/GenerateRequest.java`
- Create: `src/main/java/com/example/food/service/RecipeService.java`
- Create: `src/main/java/com/example/food/service/DeepSeekRecipeService.java`
- Create: `src/main/java/com/example/food/service/DeepSeekConfigurationException.java`
- Create: `src/main/java/com/example/food/service/DeepSeekApiException.java`
- Create: `src/main/java/com/example/food/controller/RecipeController.java`
- Create: `src/main/resources/application.yml`

- [ ] **Step 1: Implement application entry point**

Add `FoodApplication` with a `RestTemplate` bean using reasonable connect and read timeouts.

- [ ] **Step 2: Implement models**

Add `Recipe` and `GenerateRequest` Java records.

- [ ] **Step 3: Implement local recipe search**

Add a focused `RecipeService` with in-memory recipes and case-insensitive keyword matching across name, ingredients, and steps.

- [ ] **Step 4: Implement DeepSeek client**

Add `DeepSeekRecipeService` that validates the API key, builds a Chinese cooking prompt, calls DeepSeek, and extracts `choices[0].message.content`.

- [ ] **Step 5: Implement controller**

Add search and generation endpoints with JSON error responses for invalid input, missing configuration, and DeepSeek failures.

- [ ] **Step 6: Run tests and confirm GREEN**

Run: `mvn test`

Expected: PASS with all tests green.

### Task 3: Browser Demo and Documentation

**Files:**
- Create: `src/main/resources/static/index.html`
- Create: `README.md`

- [ ] **Step 1: Add static page**

Create a single page with a search form, a generation form, result rendering, and visible error messages.

- [ ] **Step 2: Add run instructions**

Document Java 17, Maven, `DEEPSEEK_API_KEY`, `mvn spring-boot:run`, and `http://localhost:8080`.

- [ ] **Step 3: Build verification**

Run: `mvn test`

Expected: PASS.

### Task 4: Runtime Verification

**Files:**
- No file changes.

- [ ] **Step 1: Start the app**

Run: `mvn spring-boot:run`

Expected: Spring Boot starts on port `8080`.

- [ ] **Step 2: Verify search endpoint**

Run: `Invoke-RestMethod 'http://localhost:8080/api/recipes/search?keyword=鸡蛋'`

Expected: JSON array with at least one recipe.

- [ ] **Step 3: Verify AI endpoint**

Run with `DEEPSEEK_API_KEY` set in the process environment and POST ingredients to `/api/recipes/generate`.

Expected: JSON object containing a non-empty `content` field generated by DeepSeek.

## Notes

The workspace is not a Git repository, so commit steps are intentionally omitted. Do not write the DeepSeek key into files.
