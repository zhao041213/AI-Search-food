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

默认读取本地 MySQL：

```env
MYSQL_URL=jdbc:mysql://localhost:3306/ai_smart_recipe?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=root
JWT_SECRET=change-this-secret-change-this-secret-32
MOCK_LOGIN_CODE=123456
DASHSCOPE_API_KEY=你的千问 API Key
DASHSCOPE_MODEL=qwen-plus
```

生产或演示部署时必须替换 `JWT_SECRET`。`DASHSCOPE_API_KEY` 可作为兜底环境变量；管理员后台保存的 AI 接入配置会优先生效。Flyway 会自动创建业务表和初始配置。

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
