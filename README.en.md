# AI Smart Recipe Recommendation System

<p align="center">
  <strong>A multimodal AI workspace for ingredient recognition and personalized recipes</strong>
</p>

<p align="center">
  <a href="README.md">中文</a>
  &nbsp;·&nbsp;
  <a href="README.en.md"><strong>English</strong></a>
</p>

This project connects ingredient input, image recognition, AI recipe generation, pantry management, shopping preparation, cooking, and feedback into one practical workflow for home users. It also provides an admin workspace for operations, model configuration, audit logs, and system error logs.

> A graduation project built with Java 17, Spring Boot 3, Vue 3, and Docker Compose. It supports local development, MySQL persistence, and containerized deployment.

## Features

| Area | Implemented capabilities |
| --- | --- |
| Smart input | Text input, ingredient image upload, and camera capture |
| AI recipes | Personalized recommendations, streaming generation, nutrition estimates, cooking steps, and video keywords |
| Ingredient planning | Required ingredients, pantry matching, shortage analysis, shopping lists, and expiry alerts |
| User workspace | Saved recipes, search history, recommendation feedback, health profiles, and diet preferences |
| Pantry and menus | Stock-in, cooking consumption, undoable operations, weekly menus, and AI-generated 21-meal plans |
| Finished-dish review | AI evaluation of an uploaded finished-dish image with persisted review history |
| Authentication | SMS registration/login, phone-password login, password reset, and failed-login lockout |
| Admin workspace | Operations dashboard, popular ingredients, AI configuration, operation logs, and error logs |

## Highlights

- Recipe generation uses Server-Sent Events so returned fields are progressively rendered in the existing result view.
- The result-first interaction is preserved: the result page takes over after generation starts, without a full-screen loading layer hiding streamed text.
- Recommendations can incorporate ingredients, meal type, diet preferences, health profile, and pantry state.
- User passwords are stored as BCrypt hashes and can be used alongside SMS authentication.
- Admin operations and system errors are recorded separately, with sensitive values sanitized before persistence and display.

## Architecture

```text
┌──────────────────────────────────────────────────────────────┐
│                         Vue 3 frontend                       │
│ Element Plus · Pinia · Vue Router · Nginx · SSE consumer       │
└──────────────────────────────┬───────────────────────────────┘
                               │ /api reverse proxy
┌──────────────────────────────▼───────────────────────────────┐
│                     Spring Boot 3 backend                    │
│ Auth · AI orchestration · recipes · pantry · menus · admin    │
└───────────────┬───────────────────────────┬───────────────────┘
                │                           │
        ┌───────▼────────┐          ┌───────▼────────────────┐
        │ MySQL / H2     │          │ Qwen-compatible API     │
        │ Flyway          │          │ Aliyun SMS / PNVS       │
        └────────────────┘          └────────────────────────┘
```

## Project structure

```text
AI-Search-food/
├─ backend/
│  ├─ src/main/java/com/example/food/
│  │  ├─ ai/             # Recipe generation, streaming, vision, AI config
│  │  ├─ auth/           # User/admin auth, verification codes, password policy
│  │  ├─ admin/          # Dashboard, operation logs, error logs
│  │  ├─ recipe/         # Saved recipes, search history, feedback
│  │  ├─ pantry/         # Pantry, stock-in, consumption, readiness
│  │  ├─ weekly/         # Weekly menus and shopping status
│  │  ├─ user/           # Health profiles and diet preferences
│  │  ├─ review/         # Finished-dish reviews
│  │  └─ security/       # JWT, roles, and Spring Security
│  ├─ src/main/resources/db/migration/  # Flyway migrations
│  ├─ src/test/                         # Unit, controller, integration tests
│  ├─ Dockerfile
│  └─ pom.xml
├─ frontend/
│  ├─ src/
│  │  ├─ api/            # API clients
│  │  ├─ components/     # Reusable business components
│  │  ├─ stores/         # Pinia stores
│  │  ├─ utils/          # Stream parsing and business utilities
│  │  └─ views/          # User and admin views
│  ├─ nginx/             # Local and container Nginx configuration
│  ├─ scripts/           # Nginx lifecycle scripts
│  ├─ Dockerfile
│  └─ package.json
├─ docs/                 # Design docs, plans, and setup notes
├─ docker-compose.yml
├─ docker-compose.debug.yml
├─ .env.example
├─ README.md
└─ README.en.md
```

## Requirements

- Java 17+
- Maven 3.8+
- Node.js 20+
- MySQL 8+ (H2 is available for local development)
- Docker Desktop for Compose deployment

## Quick start

### Start the backend

The default profile uses an in-memory H2 database:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml spring-boot:run
```

The backend listens on `http://localhost:7068` by default.

### Start the frontend

The standard local demo uses Nginx to serve the built frontend and proxy `/api` requests:

```powershell
cd frontend
npm install
npm start
```

Open `http://localhost:5173`. Use `npm run dev` for Vite hot reload. Stop the Nginx process started by this project with:

```powershell
npm run stop
```

On Windows, install Nginx with `winget install -e --id nginxinc.nginx` if needed.

## Configuration

### AI and local development

```env
JWT_SECRET=change-this-secret-change-this-secret-32
SMS_PROVIDER=mock
DASHSCOPE_API_KEY=your-qwen-api-key
DASHSCOPE_MODEL=qwen-plus
DASHSCOPE_VISION_MODEL=qwen-vl-plus
```

The `DASHSCOPE_API_KEY` environment variable is a fallback. AI settings saved from the admin workspace take precedence. Replace `JWT_SECRET` in shared or production environments with a random value of at least 32 bytes.

### H2 and MySQL

For persistent local data, copy the local configuration template and set `SPRING_PROFILES_ACTIVE=local`:

```powershell
Copy-Item backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml
```

Alternatively, enable the `mysql` profile:

```env
SPRING_PROFILES_ACTIVE=mysql
MYSQL_URL=jdbc:mysql://localhost:3306/ai_smart_recipe?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=your-mysql-password
```

Flyway runs migrations on application startup. Add a new migration instead of editing an already-applied migration.

### SMS verification

The default `mock` provider returns a generated code in the API response and auto-fills it in the frontend. This mode is for local development only. Codes expire after 5 minutes, cannot be requested again within 60 seconds, and become invalid after 5 failed attempts.

For real SMS verification, configure Aliyun PNVS:

```env
SMS_PROVIDER=aliyun-pnvs
ALIBABA_CLOUD_ACCESS_KEY_ID=your-access-key-id
ALIBABA_CLOUD_ACCESS_KEY_SECRET=your-access-key-secret
ALIYUN_PNVS_SIGN_NAME=your-system-signature
ALIYUN_PNVS_TEMPLATE_CODE=your-template-code
ALIYUN_PNVS_ENDPOINT=dypnsapi.aliyuncs.com
```

Production deployment must enable the `mysql,prod` profiles. Production mode disables the mock sender and refuses to start when real SMS configuration is missing. Keep secrets in environment variables or untracked local configuration.

## Authentication

Regular users can choose phone verification-code login or phone-password login. New users set a password during registration. Existing accounts without a password can continue using SMS login and set a password through the “forgot password” flow.

Passwords must be 8–64 characters and contain at least one letter and one digit. Password login is locked for 15 minutes after five consecutive failures; SMS login and password reset remain available. Passwords are not stored in browser local storage.

Default admin account:

```text
Username: admin
Password: Admin@123456
```

Change the initial admin password before any real deployment.

## API overview

### AI and recipes

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/ai/recipes/generate` | Generate a recipe in one response |
| `POST` | `/api/ai/recipes/generate/stream` | Generate a recipe over SSE |
| `POST` | `/api/ai/ingredients/recognize` | Recognize ingredients from JPG, PNG, or WebP, up to 5 MB |
| `POST` | `/api/ai/finished-dish-reviews` | Request an AI review from a finished-dish image |

### Authentication

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/auth/user/register/code` | Request a registration code |
| `POST` | `/api/auth/user/register` | Register and sign in |
| `POST` | `/api/auth/user/code` | Request a login code |
| `POST` | `/api/auth/user/login` | SMS login |
| `POST` | `/api/auth/user/password-login` | Password login |
| `POST` | `/api/auth/user/password/reset/code` | Request a password reset code |
| `POST` | `/api/auth/user/password/reset` | Reset a user password |
| `POST` | `/api/auth/admin/login` | Admin login |
| `GET` | `/api/auth/me` | Read the current principal |

### User data and administration

- `/api/users/me/pantry`: pantry, expiry alerts, readiness, stock-in, cooking consumption, and undo.
- `/api/users/me/health-profile`: health profile.
- `/api/users/me/diet-preferences`: diet preferences.
- `/api/users/me/weekly-menu`: weekly menu, AI generation, and shopping status.
- `/api/recipes/saved`: saved recipes.
- `/api/search-history/recent`: recent searches.
- `/api/recommendation-feedbacks/{searchLogId}`: recommendation feedback and cooked status.
- `/api/admin/dashboard/overview`: operations overview and popular ingredients.
- `/api/admin/ai-config/text-recipe`: text-recipe AI configuration.
- `/api/admin/operation-logs`: admin operation audit logs.
- `/api/admin/error-logs`: system error logs.
- `/api/stats/hot-ingredients`: popular ingredient statistics.

Protected endpoints use:

```http
Authorization: Bearer <token>
```

See [Local setup, API, and Aliyun SMS configuration](docs/local-run-api-and-aliyun-sms.md) for request examples and provider details.

## Test and build

Backend tests:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test
```

Frontend production build:

```powershell
cd frontend
npm run build
```

Backend tests cover authentication, password policy, lockout, verification codes, AI clients, controllers, authorization, and core services. Frontend utility tests are kept alongside the source and the production build checks Vue, routing, and static assets.

## Docker Compose

From the project root:

```powershell
Copy-Item .env.example .env
# Edit .env with database, JWT, Qwen, and Aliyun PNVS settings
docker compose up -d --build
docker compose ps
```

The frontend is available at `http://localhost`. Set `FRONTEND_PORT=8080` in `.env` if port 80 is unavailable.

Useful commands:

```powershell
docker compose logs -f backend
docker compose logs -f frontend
docker compose stop
docker compose start
docker compose down
```

`docker compose down` keeps the `mysql_data` and `review_uploads` volumes. Use `docker compose down -v` only when you explicitly want to remove database and uploaded-file data.

## Security and development conventions

- Never commit `.env`, API keys, SMS credentials, database passwords, or real user data.
- Store user passwords as BCrypt hashes; never write passwords or verification codes to logs.
- Use real SMS, a random JWT secret, and MySQL in production; never use the mock verification provider publicly.
- Protect admin endpoints by role and isolate user resources by the authenticated principal.
- Manage schema changes through Flyway versioned migrations.
- Keep new user-facing copy primarily in Chinese to match the current product experience.

## Documentation

- [Local setup, API, and Aliyun SMS configuration](docs/local-run-api-and-aliyun-sms.md)
- [Design documents and phase plans](docs/superpowers/)
- [中文 README](README.md)
