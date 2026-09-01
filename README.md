# AI 智能菜谱推荐系统

基于多模态大模型的食材识别与智能菜谱推荐系统。当前分支包含标准前后端分离结构、MySQL/Flyway 数据库基础、JWT 认证、手机号注册与登录、管理员登录和前端基础页面。

## 项目结构

```text
backend/   Spring Boot 3 后端服务
frontend/  Vue 3 构建应用与 Nginx 静态托管配置
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
SMS_PROVIDER=mock
DASHSCOPE_API_KEY=你的千问 API Key
DASHSCOPE_MODEL=qwen-plus
DASHSCOPE_VISION_MODEL=qwen-vl-plus
```

默认启动使用内置 H2 临时数据库，方便前端和 AI 功能开发时不依赖 MySQL。注册数据会写入当前启用的数据源；H2 数据在应用重启后会清空，需要长期保存用户数据时应启用 MySQL 配置。生产或演示部署时必须替换 `JWT_SECRET`。`DASHSCOPE_API_KEY` 可作为兜底环境变量；管理员后台保存的 AI 接入配置会优先生效。图片食材识别默认使用 `DASHSCOPE_VISION_MODEL`，并复用千问兼容模式接口。Flyway 会自动创建业务表和初始配置。

短信验证码默认使用 `mock` 模式：后端随机生成 6 位验证码并在接口响应中返回，前端会自动填入，仅用于本地开发。验证码有效期为 5 分钟，60 秒内不能重复获取，连续输错 5 次后失效。注册和登录使用不同用途的验证码，验证码使用后立即失效。

如果没有企业短信资质，建议使用阿里云号码认证服务的短信认证模式。将 `SMS_PROVIDER` 改为 `aliyun-pnvs`，并配置号码认证服务控制台提供的系统签名和系统验证码模板：

```env
SMS_PROVIDER=aliyun-pnvs
ALIBABA_CLOUD_ACCESS_KEY_ID=你的 AccessKey ID
ALIBABA_CLOUD_ACCESS_KEY_SECRET=你的 AccessKey Secret
ALIYUN_PNVS_SIGN_NAME=恒创联众
ALIYUN_PNVS_TEMPLATE_CODE=100001
ALIYUN_PNVS_ENDPOINT=dypnsapi.aliyuncs.com
ALIYUN_PNVS_SCHEME_NAME=可选的方案名称
```

PNVS 模式使用 `SendSmsVerifyCode` 发送验证码，并使用 `CheckSmsVerifyCode` 校验验证码。系统签名必须和系统模板配套使用，不能与普通短信服务的自定义签名或模板混用。真实短信模式不会在接口响应或日志中回显验证码。以上敏感配置只应放在 IDEA 环境变量、系统环境变量或未提交的本地配置中。

生产部署必须同时启用 `prod` profile，例如 `SPRING_PROFILES_ACTIVE=mysql,prod`。该 profile 会禁用模拟发送器并默认选择 PNVS；缺少真实短信配置时应用将拒绝启动，避免公共接口回显有效验证码。

如果希望本地每次启动都自动带千问 Key，可以复制示例文件：

```powershell
Copy-Item backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml
```

然后在 `application-local.yml` 中填写自己的 MySQL 连接信息和千问 Key，并在 IDEA 启动配置中设置：

```env
SPRING_PROFILES_ACTIVE=local
```

IDEA 路径：`Run/Debug Configurations` -> `FoodApplication` -> `Environment variables`。

`application-local.yml` 已被 Git 忽略，不会提交到 GitHub。管理员后台保存的 AI 接入配置仍然优先生效。后续迁移到服务器数据库时，只需要把 `application-local.yml` 里的 `spring.datasource.url` 从 `localhost` 改成服务器地址。

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
- `GET/PUT /api/users/me/weekly-menu`：读取或保存当前用户的一周菜单。
- `POST /api/users/me/weekly-menu/auto-generate`：结合用户已保存菜谱、健康档案、饮食偏好和已有食材，调用 AI 自动安排一周 21 个餐次；覆盖已有菜单时传入 `overwrite=true`。
- `PUT /api/users/me/weekly-menu/shopping-status`：更新本周采购清单中的食材状态。

PowerShell 临时配置千问 Key：

```powershell
$env:DASHSCOPE_API_KEY="你的千问 API Key"
```

## 本地启动

后端：

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml spring-boot:run
```

前端默认使用 Nginx 启动：

```powershell
cd frontend
npm install
npm start
```

可使用 `winget install -e --id nginxinc.nginx` 安装官方 Windows 版 Nginx；启动脚本会自动识别 WinGet、`C:\nginx` 或 `NGINX_HOME` 指向的安装目录。`npm start` 会先执行构建，再由 Nginx 托管 `dist` 并将 `/api` 反向代理到后端 `7068` 端口。

默认访问：

```text
http://localhost:5173
```

停止本项目启动的 Nginx：

```powershell
npm run stop
```

`npm run dev` 仅保留给前端源码开发时的热更新调试；正式本地演示和部署使用 `npm start`。

## 登录说明

新手机号需要先在“用户注册”页面获取验证码并完成注册，之后才能使用“手机号登录”。模拟短信模式会自动填入本次随机验证码；PNVS 真实短信模式需要输入手机收到的验证码。

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

## Docker Compose 部署

完整的本地启动、API 调用、千问和阿里云 PNVS 配置说明见：[本地启动、API 与阿里云短信配置](docs/local-run-api-and-aliyun-sms.md)。

Docker Compose 用于本地演示、测试环境和毕业设计展示。安装 Docker Desktop（包含 Compose）后，宿主机不需要安装 Java、Maven、Node.js、MySQL 或 Nginx。

首次使用：

```powershell
Copy-Item .env.example .env
# 编辑 .env，填写数据库、JWT、千问和阿里云 PNVS 配置
docker compose up -d --build
docker compose ps
```

前端默认入口为 `http://localhost`。端口冲突时，在 `.env` 中设置 `FRONTEND_PORT=8080` 后访问 `http://localhost:8080`。前端容器通过 Nginx 将 `/api/*` 转发到后端，后端和 MySQL 默认不会绑定到宿主机端口。

查看日志：

```powershell
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

停止和再次启动：

```powershell
docker compose stop
docker compose start
docker compose down
docker compose up -d
```

普通 `docker compose down` 会保留 `mysql_data` 和 `review_uploads` 数据卷；只有明确需要清空数据时才执行危险命令：

```powershell
docker compose down -v
```

需要从宿主机调试后端时，使用仅绑定回环地址的调试覆盖文件：

```powershell
docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d --build
```

调试后端地址为 `http://127.0.0.1:7068`（可通过 `BACKEND_DEBUG_PORT` 修改）。生产 Docker 模式固定使用真实 PNVS，不会自动回退为模拟短信；真实短信调用可能产生费用。

常见问题：

- 数据库未健康：查看 `docker compose logs mysql`，确认 `MYSQL_*` 配置后等待健康检查完成。
- Flyway 迁移失败：查看 `docker compose logs backend`，确认数据库账号有建表权限；不要修改既有迁移文件。
- 千问或短信配置缺失：后端启动日志只会列出缺失的变量名，不会输出密钥值。
- 端口冲突：修改 `FRONTEND_PORT`；不要停止其他项目正在使用的进程。
- 重新构建镜像：执行 `docker compose build --no-cache` 后再 `docker compose up -d`。

Docker 配置不会改变原有的 `npm run dev`、`npm start` 和 `mvn spring-boot:run` 本地开发方式。
