# AI 智能菜谱推荐系统

基于多模态大模型的食材识别与智能菜谱推荐系统。当前分支是 Phase 1 基础工程：标准前后端分离结构、MySQL/Flyway 数据库基础、JWT 认证、模拟手机号登录、管理员登录和前端基础页面。

## 项目结构

```text
backend/   Spring Boot 3 后端服务
frontend/  Vue 3 + Vite 前端应用
docs/      需求设计与阶段计划
```

## 环境要求

- Java 17+
- Maven 3.8+
- Node.js 20+
- MySQL 8+

## 后端配置

默认本地启动配置：

```env
JWT_SECRET=change-this-secret-change-this-secret-32
MOCK_LOGIN_CODE=123456
DASHSCOPE_API_KEY=你的千问 API Key
DASHSCOPE_MODEL=qwen-plus
DASHSCOPE_VISION_MODEL=qwen-vl-plus
```

默认启动使用内置 H2 临时数据库，方便前端和 AI 功能开发时不依赖 MySQL。生产或演示部署时必须替换 `JWT_SECRET`。`DASHSCOPE_API_KEY` 可作为兜底环境变量；管理员后台保存的 AI 接入配置会优先生效。图片食材识别默认使用 `DASHSCOPE_VISION_MODEL`，并复用千问兼容模式接口。Flyway 会自动创建业务表和初始配置。

如果希望本地每次启动都自动带千问 Key，可以复制示例文件：

```powershell
Copy-Item backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml
```

然后在 `application-local.yml` 中填写自己的 Key，并在 IDEA 启动配置中设置：

```env
SPRING_PROFILES_ACTIVE=local
```

IDEA 路径：`Run/Debug Configurations` -> `FoodApplication` -> `Environment variables`。

`application-local.yml` 已被 Git 忽略，不会提交到 GitHub。管理员后台保存的 AI 接入配置仍然优先生效。

需要接入 MySQL 时启用 `mysql` profile，并配置：

```powershell
SPRING_PROFILES_ACTIVE=mysql
MYSQL_URL=jdbc:mysql://localhost:3306/ai_smart_recipe?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=你的 MySQL 密码
```

当前 AI 接口：

- `POST /api/ai/recipes/generate`：根据文字食材生成菜谱。
- `POST /api/ai/ingredients/recognize`：上传 JPG、PNG 或 WebP 图片识别食材，最大 5MB。

PowerShell 临时配置千问 Key：

```powershell
$env:DASHSCOPE_API_KEY="你的千问 API Key"
```

## 本地启动

后端：

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml spring-boot:run
```

前端：

```powershell
cd frontend
npm install
npm run dev
```

默认访问：

```text
http://localhost:5173
```

## 初始账号

模拟手机号登录：

```text
验证码：123456
```

管理员登录：

```text
账号：admin
密码：Admin@123456
```

正式部署后应在 MySQL 中修改初始管理员密码。

## 验证命令

后端测试：

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test
```

前端构建：

```powershell
cd frontend
npm run build
```
