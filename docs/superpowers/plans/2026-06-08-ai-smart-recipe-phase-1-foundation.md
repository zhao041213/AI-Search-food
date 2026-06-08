# AI Smart Recipe Phase 1 Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the repository foundation into a standard frontend/backend project with MySQL schema management, Spring Security + JWT authentication, simulated phone login for users, admin password login, and a Vue 3 frontend shell.

**Architecture:** This phase replaces the old single-module demo with `backend/` and `frontend/`. The backend is a Spring Boot API service with Flyway-managed MySQL tables, MyBatis Plus persistence, JWT authentication, and role-based authorization. The frontend is a Vue 3 + Vite app with Element Plus, Pinia, Vue Router, Axios, and guarded user/admin routes.

**Tech Stack:** Java 17, Spring Boot 3, MySQL 8, MyBatis Plus, Flyway, Spring Security, JWT, JUnit 5, Mockito, Vue 3, Vite, Element Plus, Pinia, Vue Router, Axios, ECharts, lucide-vue-next.

---

## Scope Note

The full system design covers independent subsystems. This plan implements only Phase 1 foundation.

Later plans should cover:

- Phase 2: Qwen/DeepSeek AI search, structured recipe persistence, shopping-link generation.
- Phase 3: image ingredient recognition, pantry, favorites, history, recipe detail, regeneration, step mode.
- Phase 4: admin charts, prompt templates, model switching, finished-dish review.
- Phase 5: deployment, README, screenshots, and cleanup.

## Target File Structure

Create this structure:

```text
AI-Search-food/
  backend/
    pom.xml
    src/main/java/com/example/food/
    src/main/resources/
    src/test/java/com/example/food/
  frontend/
    package.json
    index.html
    vite.config.js
    src/
  docs/
  AGENTS.md
```

Remove the old root Maven demo after the new skeleton compiles:

```text
pom.xml
src/
```

Keep:

```text
AGENTS.md
docs/
.gitignore
```

## Phase 1 Deliverables

- Backend runs from `backend/`.
- Frontend builds from `frontend/`.
- MySQL schema is versioned under Flyway.
- User login supports phone number plus simulated code.
- Admin login supports username plus password.
- JWT authentication works for `USER` and `ADMIN`.
- Frontend has login pages, route guards, base layout, and minimal dashboards.
- Old demo source is removed after the new foundation is verified.

---

### Task 1: Rebuild Repository Skeleton

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/example/food/FoodApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Modify: `.gitignore`
- Delete after verification: `pom.xml`
- Delete after verification: `src/`

- [ ] **Step 1: Create backend directory structure**

Run:

```powershell
New-Item -ItemType Directory -Force backend/src/main/java/com/example/food
New-Item -ItemType Directory -Force backend/src/main/resources
New-Item -ItemType Directory -Force backend/src/test/java/com/example/food
```

Expected: directories exist.

- [ ] **Step 2: Create backend Maven configuration**

Create `backend/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.6</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>ai-smart-recipe-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>ai-smart-recipe-backend</name>
    <description>AI smart recipe search backend</description>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <jjwt.version>0.12.6</jjwt.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Create backend application entry point**

Create `backend/src/main/java/com/example/food/FoodApplication.java`:

```java
package com.example.food;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FoodApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodApplication.class, args);
    }
}
```

- [ ] **Step 4: Create backend base configuration**

Create `backend/src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: ai-smart-recipe-backend
  datasource:
    url: ${MYSQL_URL:jdbc:mysql://localhost:3306/ai_smart_recipe?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true}
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

app:
  jwt:
    secret: ${JWT_SECRET:change-this-secret-change-this-secret-32}
    expiration-minutes: ${JWT_EXPIRATION_MINUTES:1440}
  auth:
    mock-code: ${MOCK_LOGIN_CODE:123456}
```

- [ ] **Step 5: Update `.gitignore`**

Ensure `.gitignore` contains:

```gitignore
target/
.m2/
node_modules/
dist/
.env
*.log
.idea/
.vscode/
```

- [ ] **Step 6: Verify backend skeleton compiles**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test
```

Expected: build succeeds or fails only because no database is available during application-context tests. If it fails due to database auto-configuration, continue to Task 2 test profile before deleting old files.

- [ ] **Step 7: Delete old root demo files after backend skeleton verification**

Run:

```powershell
git rm -r src
git rm pom.xml
```

Expected: root `src/` and root `pom.xml` are staged for deletion. Do not remove `docs/`, `AGENTS.md`, `.gitignore`, `.m2/`, or deployment files in this task.

- [ ] **Step 8: Review and commit Task 1**

Run:

```powershell
git status --short
git diff --check
```

Expected: no whitespace errors. Ask the user before committing, then commit if approved:

```powershell
git add backend/pom.xml backend/src/main/java/com/example/food/FoodApplication.java backend/src/main/resources/application.yml .gitignore
git add -u src pom.xml
git commit -m "chore: rebuild backend project skeleton"
```

---

### Task 2: Add Base API Response And Test Profile

**Files:**
- Create: `backend/src/main/java/com/example/food/common/ApiResponse.java`
- Create: `backend/src/main/java/com/example/food/common/GlobalExceptionHandler.java`
- Create: `backend/src/test/resources/application-test.yml`
- Create: `backend/src/test/java/com/example/food/FoodApplicationTest.java`

- [ ] **Step 1: Create `ApiResponse`**

Create `backend/src/main/java/com/example/food/common/ApiResponse.java`:

```java
package com.example.food.common;

public record ApiResponse<T>(
        int code,
        String message,
        T data
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

- [ ] **Step 2: Create global exception handler**

Create `backend/src/main/java/com/example/food/common/GlobalExceptionHandler.java`:

```java
package com.example.food.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid request parameters"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400, exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Internal server error"));
    }
}
```

- [ ] **Step 3: Add test profile**

Create `backend/src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:ai_smart_recipe;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    username: sa
    password:
    driver-class-name: org.h2.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration

app:
  jwt:
    secret: test-secret-test-secret-test-secret-32
    expiration-minutes: 1440
  auth:
    mock-code: "123456"
```

- [ ] **Step 4: Add application context test**

Create `backend/src/test/java/com/example/food/FoodApplicationTest.java`:

```java
package com.example.food;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FoodApplicationTest {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 5: Run backend tests**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test
```

Expected: test phase reaches application context. If Flyway fails because migrations do not exist yet, continue to Task 3 before expecting green.

- [ ] **Step 6: Review and commit Task 2**

Run:

```powershell
git status --short
git diff --check
```

Ask the user before committing, then commit if approved:

```powershell
git add backend/src/main/java/com/example/food/common backend/src/test
git commit -m "feat: add backend base response and test profile"
```

---

### Task 3: Add MySQL Schema Migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/main/resources/db/migration/V2__seed_admin_and_model_config.sql`

- [ ] **Step 1: Create initial schema migration**

Create `backend/src/main/resources/db/migration/V1__init_schema.sql`:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(32) NOT NULL UNIQUE,
    nickname VARCHAR(64) NOT NULL,
    avatar_url VARCHAR(512),
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at DATETIME
);

CREATE TABLE admins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'ADMIN',
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    preference_type VARCHAR(64) NOT NULL,
    preference_value VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_pantry_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    ingredient_name VARCHAR(128) NOT NULL,
    category VARCHAR(64),
    quantity DECIMAL(10, 2),
    unit VARCHAR(32),
    expire_date DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_pantry_items_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE search_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    anonymous_id VARCHAR(64),
    query_text VARCHAR(1000),
    input_type VARCHAR(32) NOT NULL,
    recognized_ingredients TEXT,
    ai_model VARCHAR(128),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_search_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE recipe_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    search_log_id BIGINT,
    parent_recipe_id BIGINT,
    title VARCHAR(128) NOT NULL,
    difficulty VARCHAR(64),
    cooking_time VARCHAR(64),
    taste VARCHAR(128),
    servings INT,
    reason TEXT,
    nutrition_advice TEXT,
    video_keywords TEXT,
    ai_model VARCHAR(128),
    raw_response MEDIUMTEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recipe_records_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_recipe_records_search_log FOREIGN KEY (search_log_id) REFERENCES search_logs(id),
    CONSTRAINT fk_recipe_records_parent FOREIGN KEY (parent_recipe_id) REFERENCES recipe_records(id)
);

CREATE TABLE recipe_ingredients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    ingredient_name VARCHAR(128) NOT NULL,
    amount VARCHAR(128),
    category VARCHAR(64),
    already_owned TINYINT NOT NULL DEFAULT 0,
    substitute_names VARCHAR(512),
    CONSTRAINT fk_recipe_ingredients_recipe FOREIGN KEY (recipe_id) REFERENCES recipe_records(id)
);

CREATE TABLE recipe_steps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    title VARCHAR(128),
    instruction TEXT NOT NULL,
    estimated_minutes INT,
    tip TEXT,
    CONSTRAINT fk_recipe_steps_recipe FOREIGN KEY (recipe_id) REFERENCES recipe_records(id)
);

CREATE TABLE shopping_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    ingredient_name VARCHAR(128) NOT NULL,
    amount VARCHAR(128),
    category VARCHAR(64),
    already_owned TINYINT NOT NULL DEFAULT 0,
    recommended_buy TINYINT NOT NULL DEFAULT 1,
    substitute_names VARCHAR(512),
    taobao_url VARCHAR(1000),
    jd_url VARCHAR(1000),
    CONSTRAINT fk_shopping_items_recipe FOREIGN KEY (recipe_id) REFERENCES recipe_records(id)
);

CREATE TABLE recipe_favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_recipe_favorites_user_recipe UNIQUE (user_id, recipe_id),
    CONSTRAINT fk_recipe_favorites_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_recipe_favorites_recipe FOREIGN KEY (recipe_id) REFERENCES recipe_records(id)
);

CREATE TABLE uploaded_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    purpose VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_uploaded_files_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE finished_dish_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    recipe_id BIGINT,
    uploaded_file_id BIGINT NOT NULL,
    review_result TEXT NOT NULL,
    ai_model VARCHAR(128),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_finished_dish_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_finished_dish_reviews_recipe FOREIGN KEY (recipe_id) REFERENCES recipe_records(id),
    CONSTRAINT fk_finished_dish_reviews_file FOREIGN KEY (uploaded_file_id) REFERENCES uploaded_files(id)
);

CREATE TABLE prompt_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_key VARCHAR(128) NOT NULL UNIQUE,
    template_name VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE ai_model_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    purpose VARCHAR(64) NOT NULL,
    primary_model TINYINT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_logs_created_at ON search_logs(created_at);
CREATE INDEX idx_search_logs_user_id ON search_logs(user_id);
CREATE INDEX idx_recipe_records_created_at ON recipe_records(created_at);
CREATE INDEX idx_recipe_records_user_id ON recipe_records(user_id);
CREATE INDEX idx_user_pantry_items_user_id ON user_pantry_items(user_id);
```

- [ ] **Step 2: Create seed migration**

Create `backend/src/main/resources/db/migration/V2__seed_admin_and_model_config.sql`:

```sql
INSERT INTO admins (username, password_hash, nickname, role, enabled)
VALUES (
    'admin',
    '$2a$10$t/6IxhrR085QmHJSGuWlcedUq3wMSQ4AAXD7QWuZnSx3yhal/t.bC',
    'Administrator',
    'ADMIN',
    1
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
```

The seeded admin password is `admin123`.

- [ ] **Step 3: Run backend tests**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test
```

Expected: `FoodApplicationTest` passes under H2 MySQL mode.

- [ ] **Step 4: Review and commit Task 3**

Run:

```powershell
git status --short
git diff --check
```

Ask the user before committing, then commit if approved:

```powershell
git add backend/src/main/resources/db/migration
git commit -m "feat: add initial database schema"
```

---

### Task 4: Implement JWT Security Foundation

**Files:**
- Create: `backend/src/main/java/com/example/food/security/AppRole.java`
- Create: `backend/src/main/java/com/example/food/security/AuthPrincipal.java`
- Create: `backend/src/main/java/com/example/food/security/JwtService.java`
- Create: `backend/src/main/java/com/example/food/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/example/food/security/SecurityConfig.java`
- Create: `backend/src/test/java/com/example/food/security/JwtServiceTest.java`

- [ ] **Step 1: Create role enum**

Create `backend/src/main/java/com/example/food/security/AppRole.java`:

```java
package com.example.food.security;

public enum AppRole {
    USER,
    ADMIN
}
```

- [ ] **Step 2: Create auth principal**

Create `backend/src/main/java/com/example/food/security/AuthPrincipal.java`:

```java
package com.example.food.security;

public record AuthPrincipal(
        Long id,
        String username,
        AppRole role
) {
}
```

- [ ] **Step 3: Create JWT service**

Create `backend/src/main/java/com/example/food/security/JwtService.java`:

```java
package com.example.food.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes
    ) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationMinutes * 60);
        return Jwts.builder()
                .subject(principal.username())
                .claim("id", principal.id())
                .claim("role", principal.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public AuthPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number id = claims.get("id", Number.class);
        String role = claims.get("role", String.class);
        return new AuthPrincipal(id.longValue(), claims.getSubject(), AppRole.valueOf(role));
    }
}
```

- [ ] **Step 4: Test JWT service**

Create `backend/src/test/java/com/example/food/security/JwtServiceTest.java`:

```java
package com.example.food.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void generatesAndParsesUserToken() {
        JwtService service = new JwtService("test-secret-test-secret-test-secret-32", 1440);

        String token = service.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));
        AuthPrincipal principal = service.parseToken(token);

        assertThat(principal.id()).isEqualTo(7L);
        assertThat(principal.username()).isEqualTo("13800138000");
        assertThat(principal.role()).isEqualTo(AppRole.USER);
    }
}
```

- [ ] **Step 5: Create JWT authentication filter**

Create `backend/src/main/java/com/example/food/security/JwtAuthenticationFilter.java`:

```java
package com.example.food.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            AuthPrincipal principal = jwtService.parseToken(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 6: Create security configuration**

Create `backend/src/main/java/com/example/food/security/SecurityConfig.java`:

```java
package com.example.food.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/user/**").hasRole("USER")
                        .requestMatchers("/api/ai/**").permitAll()
                        .requestMatchers("/api/stats/hot-ingredients", "/api/stats/hot-recipes").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
```

- [ ] **Step 7: Run JWT tests**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test -Dtest=JwtServiceTest
```

Expected: `JwtServiceTest` passes.

- [ ] **Step 8: Run all backend tests**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test
```

Expected: all backend tests pass.

- [ ] **Step 9: Review and commit Task 4**

Run:

```powershell
git status --short
git diff --check
```

Ask the user before committing, then commit if approved:

```powershell
git add backend/src/main/java/com/example/food/security backend/src/test/java/com/example/food/security
git commit -m "feat: add jwt security foundation"
```

---

### Task 5: Implement User And Admin Login

**Files:**
- Create: `backend/src/main/java/com/example/food/auth/AuthController.java`
- Create: `backend/src/main/java/com/example/food/auth/AuthService.java`
- Create: `backend/src/main/java/com/example/food/auth/dto/AdminLoginRequest.java`
- Create: `backend/src/main/java/com/example/food/auth/dto/AuthResponse.java`
- Create: `backend/src/main/java/com/example/food/auth/dto/PhoneCodeRequest.java`
- Create: `backend/src/main/java/com/example/food/auth/dto/PhoneLoginRequest.java`
- Create: `backend/src/main/java/com/example/food/user/User.java`
- Create: `backend/src/main/java/com/example/food/user/UserMapper.java`
- Create: `backend/src/main/java/com/example/food/admin/AdminAccount.java`
- Create: `backend/src/main/java/com/example/food/admin/AdminMapper.java`
- Create: `backend/src/test/java/com/example/food/auth/AuthControllerTest.java`

- [ ] **Step 1: Create user entity**

Create `backend/src/main/java/com/example/food/user/User.java`:

```java
package com.example.food.user;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("users")
public class User {

    private Long id;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
```

- [ ] **Step 2: Create user mapper**

Create `backend/src/main/java/com/example/food/user/UserMapper.java`:

```java
package com.example.food.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

- [ ] **Step 3: Create admin entity and mapper**

Create `backend/src/main/java/com/example/food/admin/AdminAccount.java`:

```java
package com.example.food.admin;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("admins")
public class AdminAccount {

    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

Create `backend/src/main/java/com/example/food/admin/AdminMapper.java`:

```java
package com.example.food.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper extends BaseMapper<AdminAccount> {
}
```

- [ ] **Step 4: Create auth DTOs**

Create `backend/src/main/java/com/example/food/auth/dto/PhoneCodeRequest.java`:

```java
package com.example.food.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneCodeRequest(
        @NotBlank String phone
) {
}
```

Create `backend/src/main/java/com/example/food/auth/dto/PhoneLoginRequest.java`:

```java
package com.example.food.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneLoginRequest(
        @NotBlank String phone,
        @NotBlank String code
) {
}
```

Create `backend/src/main/java/com/example/food/auth/dto/AdminLoginRequest.java`:

```java
package com.example.food.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
```

Create `backend/src/main/java/com/example/food/auth/dto/AuthResponse.java`:

```java
package com.example.food.auth.dto;

public record AuthResponse(
        String token,
        Long id,
        String displayName,
        String role
) {
}
```

- [ ] **Step 5: Create auth service**

Create `backend/src/main/java/com/example/food/auth/AuthService.java`:

```java
package com.example.food.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.food.admin.AdminAccount;
import com.example.food.admin.AdminMapper;
import com.example.food.auth.dto.AdminLoginRequest;
import com.example.food.auth.dto.AuthResponse;
import com.example.food.auth.dto.PhoneLoginRequest;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.example.food.user.User;
import com.example.food.user.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final String mockCode;

    public AuthService(
            UserMapper userMapper,
            AdminMapper adminMapper,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.mock-code}") String mockCode
    ) {
        this.userMapper = userMapper;
        this.adminMapper = adminMapper;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.mockCode = mockCode;
    }

    public String issueMockCode(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone is required");
        }
        return mockCode;
    }

    @Transactional
    public AuthResponse loginUser(PhoneLoginRequest request) {
        if (!mockCode.equals(request.code())) {
            throw new IllegalArgumentException("Invalid verification code");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, request.phone()));
        if (user == null) {
            user = new User();
            user.setPhone(request.phone());
            user.setNickname("User " + request.phone().substring(Math.max(0, request.phone().length() - 4)));
            user.setRole(AppRole.USER.name());
            user.setEnabled(true);
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.insert(user);
        } else {
            if (!Boolean.TRUE.equals(user.getEnabled())) {
                throw new IllegalArgumentException("User account is disabled");
            }
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
        String token = jwtService.generateToken(new AuthPrincipal(user.getId(), user.getPhone(), AppRole.USER));
        return new AuthResponse(token, user.getId(), user.getNickname(), AppRole.USER.name());
    }

    public AuthResponse loginAdmin(AdminLoginRequest request) {
        AdminAccount admin = adminMapper.selectOne(
                new LambdaQueryWrapper<AdminAccount>().eq(AdminAccount::getUsername, request.username())
        );
        if (admin == null || !Boolean.TRUE.equals(admin.getEnabled())) {
            throw new IllegalArgumentException("Invalid admin credentials");
        }
        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid admin credentials");
        }
        String token = jwtService.generateToken(new AuthPrincipal(admin.getId(), admin.getUsername(), AppRole.ADMIN));
        return new AuthResponse(token, admin.getId(), admin.getNickname(), AppRole.ADMIN.name());
    }
}
```

- [ ] **Step 6: Create auth controller**

Create `backend/src/main/java/com/example/food/auth/AuthController.java`:

```java
package com.example.food.auth;

import com.example.food.auth.dto.AdminLoginRequest;
import com.example.food.auth.dto.AuthResponse;
import com.example.food.auth.dto.PhoneCodeRequest;
import com.example.food.auth.dto.PhoneLoginRequest;
import com.example.food.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/user/code")
    public ApiResponse<Map<String, String>> userCode(@Valid @RequestBody PhoneCodeRequest request) {
        return ApiResponse.ok(Map.of("code", authService.issueMockCode(request.phone())));
    }

    @PostMapping("/user/login")
    public ApiResponse<AuthResponse> userLogin(@Valid @RequestBody PhoneLoginRequest request) {
        return ApiResponse.ok(authService.loginUser(request));
    }

    @PostMapping("/admin/login")
    public ApiResponse<AuthResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(authService.loginAdmin(request));
    }
}
```

- [ ] **Step 7: Add auth controller tests**

Create `backend/src/test/java/com/example/food/auth/AuthControllerTest.java`:

```java
package com.example.food.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void userCanRequestMockCode() throws Exception {
        mockMvc.perform(post("/api/auth/user/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("123456"));
    }

    @Test
    void userCanLoginWithMockCode() throws Exception {
        mockMvc.perform(post("/api/auth/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800138000\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.token", not(blankOrNullString())));
    }

    @Test
    void userCannotLoginWithWrongCode() throws Exception {
        mockMvc.perform(post("/api/auth/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800138000\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void adminCanLoginWithSeededAccount() throws Exception {
        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.token", not(blankOrNullString())));
    }
}
```

- [ ] **Step 8: Run auth controller tests**

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test -Dtest=AuthControllerTest
```

Expected: user login and admin login tests pass.

- [ ] **Step 9: Run all backend tests**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test
```

Expected: all backend tests pass.

- [ ] **Step 10: Review and commit Task 5**

Run:

```powershell
git status --short
git diff --check
```

Ask the user before committing, then commit if approved:

```powershell
git add backend/src/main/java/com/example/food/auth backend/src/main/java/com/example/food/user backend/src/main/java/com/example/food/admin backend/src/test/java/com/example/food/auth
git commit -m "feat: add user and admin authentication"
```

---

### Task 6: Create Frontend Skeleton

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/index.html`
- Create: `frontend/vite.config.js`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/stores/auth.js`
- Create: `frontend/src/api/http.js`
- Create: `frontend/src/api/auth.js`
- Create: `frontend/src/views/LoginView.vue`
- Create: `frontend/src/views/HomeView.vue`
- Create: `frontend/src/views/AdminDashboardView.vue`

- [ ] **Step 1: Create frontend project files**

Create `frontend/package.json`:

```json
{
  "name": "ai-smart-recipe-frontend",
  "version": "0.0.1",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "vite build",
    "preview": "vite preview --host 0.0.0.0"
  },
  "dependencies": {
    "@element-plus/icons-vue": "^2.3.1",
    "@vitejs/plugin-vue": "^5.2.1",
    "axios": "^1.7.9",
    "echarts": "^5.5.1",
    "element-plus": "^2.9.1",
    "lucide-vue-next": "^0.468.0",
    "pinia": "^2.3.0",
    "vue": "^3.5.13",
    "vue-router": "^4.5.0"
  },
  "devDependencies": {
    "vite": "^6.0.5"
  }
}
```

Create `frontend/index.html`:

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>AI Smart Recipe</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

Create `frontend/vite.config.js`:

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
```

- [ ] **Step 2: Create Vue app entry**

Create `frontend/src/main.js`:

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

createApp(App)
  .use(createPinia())
  .use(router)
  .use(ElementPlus)
  .mount('#app')
```

- [ ] **Step 3: Create app layout**

Create `frontend/src/App.vue`:

```vue
<template>
  <RouterView />
</template>
```

- [ ] **Step 4: Create auth store**

Create `frontend/src/stores/auth.js`:

```js
import { defineStore } from 'pinia'

const TOKEN_KEY = 'ai_smart_recipe_token'
const ROLE_KEY = 'ai_smart_recipe_role'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    role: localStorage.getItem(ROLE_KEY) || '',
    displayName: localStorage.getItem('ai_smart_recipe_display_name') || ''
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    isAdmin: (state) => state.role === 'ADMIN',
    isUser: (state) => state.role === 'USER'
  },
  actions: {
    setAuth(auth) {
      this.token = auth.token
      this.role = auth.role
      this.displayName = auth.displayName
      localStorage.setItem(TOKEN_KEY, auth.token)
      localStorage.setItem(ROLE_KEY, auth.role)
      localStorage.setItem('ai_smart_recipe_display_name', auth.displayName)
    },
    logout() {
      this.token = ''
      this.role = ''
      this.displayName = ''
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(ROLE_KEY)
      localStorage.removeItem('ai_smart_recipe_display_name')
    }
  }
})
```

- [ ] **Step 5: Create HTTP and auth APIs**

Create `frontend/src/api/http.js`:

```js
import axios from 'axios'
import { useAuthStore } from '../stores/auth'

export const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})
```

Create `frontend/src/api/auth.js`:

```js
import { http } from './http'

export function requestUserCode(phone) {
  return http.post('/auth/user/code', { phone })
}

export function loginUser(phone, code) {
  return http.post('/auth/user/login', { phone, code })
}

export function loginAdmin(username, password) {
  return http.post('/auth/admin/login', { username, password })
}
```

- [ ] **Step 6: Create router and route guards**

Create `frontend/src/router/index.js`:

```js
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/admin',
      name: 'admin-dashboard',
      component: AdminDashboardView,
      meta: { requiresAdmin: true }
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
```

- [ ] **Step 7: Create minimal views**

Create `frontend/src/views/HomeView.vue`:

```vue
<template>
  <main class="page">
    <section class="hero">
      <p class="eyebrow">AI recipe search</p>
      <h1>AI Smart Recipe</h1>
      <p class="summary">Search by ingredients, dish names, goals, or photos.</p>
      <el-input
        v-model="query"
        size="large"
        aria-label="AI recipe search"
      >
        <template #append>
          <el-button type="primary">Search</el-button>
        </template>
      </el-input>
    </section>
  </main>
</template>

<script setup>
import { ref } from 'vue'

const query = ref('')
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #ecfeff;
  color: #164e63;
  padding: 48px 24px;
}

.hero {
  max-width: 880px;
  margin: 0 auto;
}

.eyebrow {
  color: #059669;
  font-weight: 700;
}

h1 {
  margin: 0 0 12px;
  font-size: 44px;
}

.summary {
  margin-bottom: 24px;
  font-size: 18px;
}
```

Create `frontend/src/views/LoginView.vue`:

```vue
<template>
  <main class="login-page">
    <section class="login-panel">
      <h1>Sign in</h1>
      <el-tabs v-model="mode">
        <el-tab-pane label="Phone" name="user">
          <el-form label-position="top">
            <el-form-item label="Phone number">
              <el-input v-model="phone" />
            </el-form-item>
            <el-form-item label="Verification code">
              <el-input v-model="code" />
            </el-form-item>
            <el-button @click="requestCode">Get code</el-button>
            <el-button type="primary" @click="submitUserLogin">Login</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="Admin" name="admin">
          <el-form label-position="top">
            <el-form-item label="Username">
              <el-input v-model="username" />
            </el-form-item>
            <el-form-item label="Password">
              <el-input v-model="password" type="password" show-password />
            </el-form-item>
            <el-button type="primary" @click="submitAdminLogin">Login</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginAdmin, loginUser, requestUserCode } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const mode = ref('user')
const phone = ref('')
const code = ref('')
const username = ref('')
const password = ref('')

async function requestCode() {
  const response = await requestUserCode(phone.value)
  code.value = response.data.data.code
  ElMessage.success(`Mock code: ${code.value}`)
}

async function submitUserLogin() {
  const response = await loginUser(phone.value, code.value)
  auth.setAuth(response.data.data)
  router.push('/')
}

async function submitAdminLogin() {
  const response = await loginAdmin(username.value, password.value)
  auth.setAuth(response.data.data)
  router.push('/admin')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: #ecfeff;
}

.login-panel {
  width: min(420px, calc(100vw - 32px));
  border: 1px solid #bae6fd;
  border-radius: 8px;
  background: #ffffff;
  padding: 24px;
}
</style>
```

Create `frontend/src/views/AdminDashboardView.vue`:

```vue
<template>
  <main class="admin-page">
    <header>
      <h1>Admin Dashboard</h1>
      <p>Statistics and management panels will be added in later phases.</p>
    </header>
  </main>
</template>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #f8fafc;
  color: #164e63;
  padding: 32px;
}
</style>
```

- [ ] **Step 8: Install frontend dependencies**

Run:

```powershell
cd frontend
npm install
```

Expected: `node_modules/` and `package-lock.json` are created.

- [ ] **Step 9: Build frontend**

Run:

```powershell
cd frontend
npm run build
```

Expected: Vite build succeeds and creates `frontend/dist/`.

- [ ] **Step 10: Review and commit Task 6**

Run:

```powershell
git status --short
git diff --check
```

Ask the user before committing, then commit if approved:

```powershell
git add frontend/package.json frontend/package-lock.json frontend/index.html frontend/vite.config.js frontend/src
git commit -m "feat: add vue frontend skeleton"
```

---

### Task 7: Add Backend Auth Smoke Endpoints And Frontend Integration Check

**Files:**
- Create: `backend/src/main/java/com/example/food/auth/MeController.java`
- Create: `backend/src/test/java/com/example/food/auth/AuthSecurityTest.java`
- Modify: `frontend/src/api/auth.js`
- Modify: `frontend/src/stores/auth.js`

- [ ] **Step 1: Add `me` endpoint**

Create `backend/src/main/java/com/example/food/auth/MeController.java`:

```java
package com.example.food.auth;

import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class MeController {

    @GetMapping("/me")
    public ApiResponse<AuthPrincipal> me(Authentication authentication) {
        return ApiResponse.ok((AuthPrincipal) authentication.getPrincipal());
    }
}
```

- [ ] **Step 2: Add security smoke test**

Create `backend/src/test/java/com/example/food/auth/AuthSecurityTest.java`:

```java
package com.example.food.auth;

import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void meReturnsCurrentPrincipalWithToken() throws Exception {
        String token = jwtService.generateToken(new AuthPrincipal(1L, "13800138000", AppRole.USER));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("13800138000"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }
}
```

- [ ] **Step 3: Add frontend `me` API**

Modify `frontend/src/api/auth.js`:

```js
import { http } from './http'

export function requestUserCode(phone) {
  return http.post('/auth/user/code', { phone })
}

export function loginUser(phone, code) {
  return http.post('/auth/user/login', { phone, code })
}

export function loginAdmin(username, password) {
  return http.post('/auth/admin/login', { username, password })
}

export function getMe() {
  return http.get('/auth/me')
}
```

- [ ] **Step 4: Run backend tests and frontend build**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test
```

Expected: all backend tests pass.

Run:

```powershell
cd frontend
npm run build
```

Expected: frontend build succeeds.

- [ ] **Step 5: Review and commit Task 7**

Run:

```powershell
git status --short
git diff --check
```

Ask the user before committing, then commit if approved:

```powershell
git add backend/src/main/java/com/example/food/auth/MeController.java backend/src/test/java/com/example/food/auth/AuthSecurityTest.java frontend/src/api/auth.js frontend/src/stores/auth.js
git commit -m "feat: add auth smoke integration"
```

---

## Final Verification For Phase 1

- [ ] **Step 1: Run backend tests**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test
```

Expected: all backend tests pass.

- [ ] **Step 2: Run frontend build**

Run:

```powershell
cd frontend
npm run build
```

Expected: Vite build succeeds.

- [ ] **Step 3: Run git checks**

Run:

```powershell
git status --short
git diff --check
```

Expected: only intentional files are modified and there are no whitespace errors.

- [ ] **Step 4: Ask user about GitHub push**

After the final Phase 1 commit, ask:

```text
Do you want me to push the Phase 1 branch to GitHub?
```

Do not push unless the user approves.
