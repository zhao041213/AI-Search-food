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

接入阿里云短信时，将 `SMS_PROVIDER` 改为 `aliyun`，并配置以下环境变量。AccessKey、签名和模板必须全部有效，且短信模板参数名应为 `code`：

```env
SMS_PROVIDER=aliyun
ALIBABA_CLOUD_ACCESS_KEY_ID=你的 AccessKey ID
ALIBABA_CLOUD_ACCESS_KEY_SECRET=你的 AccessKey Secret
ALIYUN_SMS_SIGN_NAME=审核通过的短信签名
ALIYUN_SMS_TEMPLATE_CODE=审核通过的模板代码
ALIYUN_SMS_ENDPOINT=dysmsapi.aliyuncs.com
```

真实短信模式不会在接口响应或日志中回显验证码。以上敏感配置只应放在 IDEA 环境变量、系统环境变量或未提交的本地配置中。

生产部署必须同时启用 `prod` profile，例如 `SPRING_PROFILES_ACTIVE=mysql,prod`。该 profile 会禁用模拟发送器并默认选择阿里云；缺少真实短信配置时应用将拒绝启动，避免公共接口回显有效验证码。

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

新手机号需要先在“用户注册”页面获取验证码并完成注册，之后才能使用“手机号登录”。模拟短信模式会自动填入本次随机验证码；阿里云短信模式需要输入手机收到的验证码。

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
