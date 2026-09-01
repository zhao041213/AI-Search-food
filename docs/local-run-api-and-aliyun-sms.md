# 本地启动、API 与阿里云短信配置

本文说明如何在 Windows 本地启动项目、检查服务是否正常、调用 API，以及配置阿里云号码认证服务（PNVS）短信验证码。

## 1. 选择启动方式

| 场景 | 推荐方式 | 数据库 | 短信模式 |
| --- | --- | --- | --- |
| 日常开发、无需真实短信 | 本地启动 | H2 内存数据库 | mock |
| 本地演示、需要真实短信 | 本地启动 + 环境变量 | H2 或 MySQL | aliyun-pnvs |
| 毕业设计演示或完整部署 | Docker Compose | MySQL 容器 | aliyun-pnvs |

Docker 生产配置会强制校验 JWT、千问和 PNVS 配置；缺少必填项时 backend 会主动启动失败，这是为了避免带着不安全配置运行。

## 2. Docker Compose 启动

### 2.1 准备环境

启动 Docker Desktop，然后在项目根目录执行：

```powershell
docker version
docker compose version
```

两条命令都能正常返回版本号，才表示 Docker daemon 和 Compose 已就绪。

### 2.2 创建并填写 `.env`

首次使用时复制模板：

```powershell
Copy-Item .env.example .env
notepad .env
```

至少填写下面这些值。`.env` 已被 Git 忽略，不要把它提交到 GitHub。

```env
MYSQL_DATABASE=ai_smart_recipe
MYSQL_USER=recipe_app
MYSQL_PASSWORD=替换为数据库密码
MYSQL_ROOT_PASSWORD=替换为不同的数据库 root 密码

# 至少 32 字节，不能继续使用示例值
JWT_SECRET=替换为随机且至少 32 字节的字符串

# 阿里云百炼/千问
DASHSCOPE_API_KEY=替换为百炼 API Key
DASHSCOPE_MODEL=qwen-plus
DASHSCOPE_VISION_MODEL=qwen-vl-plus
DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions

# 阿里云号码认证服务 PNVS
ALIBABA_CLOUD_ACCESS_KEY_ID=替换为 RAM 用户的 AccessKey ID
ALIBABA_CLOUD_ACCESS_KEY_SECRET=替换为 RAM 用户的 AccessKey Secret
ALIYUN_PNVS_SIGN_NAME=替换为 PNVS 控制台中的系统签名
ALIYUN_PNVS_TEMPLATE_CODE=替换为 PNVS 控制台中的系统模板 Code
ALIYUN_PNVS_ENDPOINT=dypnsapi.aliyuncs.com
ALIYUN_PNVS_SCHEME_NAME=

# 可选运行参数
SMS_CODE_EXPIRY=5m
SMS_RESEND_INTERVAL=60s
SMS_MAX_ATTEMPTS=5
PNVS_CONNECT_TIMEOUT=5s
PNVS_READ_TIMEOUT=10s
FRONTEND_PORT=80
BACKEND_DEBUG_PORT=7068
```

`docker-compose.yml` 会自动设置 `SMS_PROVIDER=aliyun-pnvs`。PNVS 的 endpoint 填主机名即可，不要填写 `http://`；项目客户端会强制使用 HTTPS。

### 2.3 校验、启动和访问

```powershell
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

当 `mysql`、`backend`、`frontend` 都显示 `healthy` 后，打开：

```text
http://localhost
```

如果 80 端口被占用，可以在 `.env` 中改成：

```env
FRONTEND_PORT=8080
```

然后访问 `http://localhost:8080`。

### 2.4 调试 backend

需要从宿主机直接访问 backend 时，使用 debug 覆盖文件：

```powershell
docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d --build
```

此时：

- 前端入口仍是 `http://localhost`（或自定义的 `FRONTEND_PORT`）；
- backend 只绑定到 `127.0.0.1:7068`，也可以通过 `BACKEND_DEBUG_PORT` 修改；
- backend 健康检查地址为 `http://127.0.0.1:7068/actuator/health`。

### 2.5 查看日志、停止和清理

```powershell
docker compose logs --tail 100 backend
docker compose logs --tail 100 frontend
docker compose logs --tail 100 mysql

docker compose stop
docker compose start
docker compose down
```

普通 `docker compose down` 会保留 `mysql_data` 和 `review_uploads` 数据卷。只有明确要清空本地数据库和上传文件时才执行：

```powershell
docker compose down -v
```

## 3. 不使用 Docker 的本地开发启动

### 3.1 默认开发模式：H2 + mock 短信

在项目根目录启动 backend：

```powershell
$env:SMS_PROVIDER="mock"
mvn -f backend/pom.xml spring-boot:run
```

另开一个 PowerShell 窗口启动 frontend：

```powershell
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。Vite 会把 `/api` 请求代理到 `http://localhost:7068`。

mock 模式下，验证码会在接口响应中返回，适合开发调试；H2 数据库是内存数据库，backend 重启后数据会清空。

### 3.2 本地开发调用真实 PNVS 和千问

不使用 Docker 时，建议通过当前 PowerShell 会话设置环境变量，不要把密钥写进代码：

```powershell
$env:SMS_PROVIDER="aliyun-pnvs"
$env:ALIBABA_CLOUD_ACCESS_KEY_ID="你的 RAM AccessKey ID"
$env:ALIBABA_CLOUD_ACCESS_KEY_SECRET="你的 RAM AccessKey Secret"
$env:ALIYUN_PNVS_SIGN_NAME="你的 PNVS 系统签名"
$env:ALIYUN_PNVS_TEMPLATE_CODE="你的 PNVS 系统模板 Code"
$env:ALIYUN_PNVS_ENDPOINT="dypnsapi.aliyuncs.com"
$env:ALIYUN_PNVS_SCHEME_NAME=""
$env:DASHSCOPE_API_KEY="你的百炼 API Key"
$env:DASHSCOPE_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"

mvn -f backend/pom.xml spring-boot:run
```

真实 PNVS 模式不会在响应或日志中返回明文验证码，验证码会发送到手机。每次发送可能产生费用，并受 60 秒默认冷却时间限制。

如果希望本地使用 MySQL，参考 `backend/src/main/resources/application-local.example.yml`，配置 MySQL 后再启用 `SPRING_PROFILES_ACTIVE=local`。日常开发不需要 MySQL 时不要启用该 profile。

## 4. API 地址和调用方式

### 4.1 API 基地址

| 启动方式 | API 基地址 |
| --- | --- |
| Docker 默认 | `http://localhost/api` |
| Docker 自定义端口 | `http://localhost:<FRONTEND_PORT>/api` |
| Docker debug backend | `http://127.0.0.1:<BACKEND_DEBUG_PORT>/api` |
| 本地 Vite 开发 | `http://localhost:5173/api` |
| 本地 backend 直连 | `http://localhost:7068/api` |

前端 axios 已将 `baseURL` 配置为 `/api`，因此浏览器访问时通常只需要使用相对路径。需要登录的接口使用：

```http
Authorization: Bearer <登录接口返回的 token>
Content-Type: application/json
```

### 4.2 注册和登录接口

发送注册验证码：

```powershell
$api = "http://localhost/api"
$phone = "13800138000"
$body = @{ phone = $phone } | ConvertTo-Json

Invoke-RestMethod "$api/auth/user/register/code" `
  -Method Post -ContentType "application/json" -Body $body
```

收到验证码后注册：

```powershell
$body = @{
  phone = $phone
  code = "收到的六位验证码"
  nickname = "测试用户"
} | ConvertTo-Json

Invoke-RestMethod "$api/auth/user/register" `
  -Method Post -ContentType "application/json" -Body $body
```

已有用户发送登录验证码并登录：

```powershell
$body = @{ phone = $phone } | ConvertTo-Json
Invoke-RestMethod "$api/auth/user/code" `
  -Method Post -ContentType "application/json" -Body $body

$body = @{ phone = $phone; code = "收到的六位验证码" } | ConvertTo-Json
$login = Invoke-RestMethod "$api/auth/user/login" `
  -Method Post -ContentType "application/json" -Body $body
$token = $login.data.token
```

真实 PNVS 模式下，验证码接口的 `data.code` 通常为 `null`，这是正常行为；验证码应该从手机短信中读取。mock 模式才会在响应中返回验证码。

查询当前登录用户：

```powershell
Invoke-RestMethod "$api/auth/me" `
  -Headers @{ Authorization = "Bearer $token" }
```

测试 AI 菜谱接口：

```powershell
$body = @{
  ingredients = "西红柿、鸡蛋"
  mealType = "晚餐"
  goal = "快手家常菜"
  searchMode = "ingredient"
} | ConvertTo-Json -Depth 5

Invoke-RestMethod "$api/ai/recipes/generate" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{ "X-Anonymous-Id" = "local-test-user" } `
  -Body $body
```

常用接口还包括：

- `POST /api/ai/ingredients/recognize`：以 `file` 字段上传 JPG、PNG 或 WebP 图片；
- `GET/PUT /api/users/me/weekly-menu`：读取或保存周菜单；
- `GET /api/recipes/saved`：读取当前用户收藏菜谱；
- `GET /api/stats/hot-ingredients`：读取热门食材统计。

未携带 token 访问 `/api/auth/me` 返回 `401` 是正常的，表示 API 代理和认证拦截器正在生效。

## 5. 阿里云 PNVS 配置步骤

项目使用阿里云号码认证服务的短信认证接口 `SendSmsVerifyCode` 和 `CheckSmsVerifyCode`，不是普通短信服务的自定义签名模板。

### 5.1 在阿里云准备资源

1. 开通并进入[号码认证服务短信认证](https://help.aliyun.com/zh/pnvs/user-guide/sms-authentication-service)。
2. 在短信认证配置中准备系统签名和系统验证码模板，记录签名名称和模板 Code。
3. 创建专用 RAM 用户和 AccessKey，按最小权限授予调用 PNVS API 所需权限。不要在应用中使用阿里云主账号 AccessKey。
4. 保存创建时显示的 AccessKey Secret；阿里云通常不会再次显示完整 Secret。
5. 如果使用方案名，在 PNVS 控制台记录方案名并填写 `ALIYUN_PNVS_SCHEME_NAME`；没有方案名就留空。

可参考阿里云的[PNVS API 参考](https://help.aliyun.com/zh/pnvs/developer-reference/api-dypnsapi-2017-05-25-overview/)和[创建 RAM 用户](https://help.aliyun.com/zh/ram/user-guide/create-a-ram-user)说明。

### 5.2 参数对应关系

| `.env` 参数 | 控制台/代码含义 | 是否必填 |
| --- | --- | --- |
| `ALIBABA_CLOUD_ACCESS_KEY_ID` | RAM AccessKey ID | 是 |
| `ALIBABA_CLOUD_ACCESS_KEY_SECRET` | RAM AccessKey Secret | 是 |
| `ALIYUN_PNVS_SIGN_NAME` | PNVS 系统签名名称 | 是 |
| `ALIYUN_PNVS_TEMPLATE_CODE` | PNVS 系统验证码模板 Code | 是 |
| `ALIYUN_PNVS_ENDPOINT` | PNVS API endpoint，默认 `dypnsapi.aliyuncs.com` | 是 |
| `ALIYUN_PNVS_SCHEME_NAME` | 可选方案名，最多 20 个字符 | 否 |
| `SMS_CODE_EXPIRY` | 验证码有效期，默认 5 分钟 | 否 |
| `SMS_RESEND_INTERVAL` | 同一用途的重复发送间隔，默认 60 秒 | 否 |
| `SMS_MAX_ATTEMPTS` | 本地失败次数上限，默认 5 次 | 否 |
| `PNVS_CONNECT_TIMEOUT` | 连接超时，默认 5 秒 | 否 |
| `PNVS_READ_TIMEOUT` | 读取超时，默认 10 秒 | 否 |

签名和模板必须来自 PNVS 短信认证配置，不能把普通短信服务的自定义签名或模板 Code 混过来。测试阶段可以优先使用控制台提供的快速测试签名、模板和已绑定测试手机号；真实短信仍可能计费。

### 5.3 如何确认配置成功

保存 `.env` 后重建并观察 backend：

```powershell
docker compose up -d --build
docker compose ps
docker compose logs --tail 100 backend
```

然后在页面执行“用户注册”或“手机号登录”的“获取验证码”。成功时：

- 页面提示发送成功；
- 手机收到验证码；
- backend 日志只记录脱敏手机号、状态和 requestId，不会打印 AccessKey 或验证码；
- 使用手机收到的验证码完成注册或登录。

如果启动失败，优先检查：

- `ALIBABA_CLOUD_ACCESS_KEY_ID` 和 Secret 是否为空或粘贴了多余空格；
- 签名名称和模板 Code 是否属于 PNVS；
- endpoint 是否仍为 `dypnsapi.aliyuncs.com`；
- RAM 用户是否拥有 PNVS API 调用权限；
- 测试手机号是否已按控制台要求绑定或授权；
- 是否仍处于 60 秒发送冷却期。

## 6. 千问 API 配置

在阿里云百炼创建 API Key，并填写：

```env
DASHSCOPE_API_KEY=你的百炼 API Key
DASHSCOPE_MODEL=qwen-plus
DASHSCOPE_VISION_MODEL=qwen-vl-plus
DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
```

`DASHSCOPE_MODEL` 用于文本菜谱和周菜单，`DASHSCOPE_VISION_MODEL` 用于图片食材识别和成品菜评价。endpoint 必须使用 HTTPS，并填写完整的 OpenAI 兼容 `chat/completions` 地址。

可参考阿里云[百炼 OpenAI 兼容接口](https://help.aliyun.com/zh/model-studio/compatibility-of-openai-with-dashscope)文档。

## 7. 安全提醒

- 不要把 `.env`、AccessKey、API Key 或真实数据库密码提交到 GitHub。
- 生产环境不要使用阿里云主账号 AccessKey；使用专用 RAM 用户并限制权限。
- 不要为了调试把 `returnVerifyCode` 改成返回明文验证码；项目当前真实 PNVS 模式不会返回明文验证码。
- 真实 PNVS 和百炼调用可能产生费用；先使用控制台测试手机号和低频请求。
- `docker compose down -v` 会删除本地 MySQL 和上传文件，执行前确认确实需要清空数据。
