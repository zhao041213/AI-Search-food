# 智能菜谱生成与搜索系统 Demo

基于 Spring Boot 3 和 DeepSeek API 的最小可运行 Demo，包含本地菜谱搜索和 AI 菜谱生成。

## 线上访问

当前服务器访问地址：

```text
http://8.166.138.245/food/
```

根路径 `http://8.166.138.245/` 会通过 nginx 自动跳转到 `/food/`。

## 环境要求

- Java 17+
- Maven 3.8+
- DeepSeek API Key

## 本地配置 API Key

不要把 API Key 写入代码。PowerShell 中这样设置当前终端会话的环境变量：

```powershell
$env:DEEPSEEK_API_KEY="你的DeepSeek API Key"
```

默认模型是 `deepseek-v4-flash`。如需切换模型：

```powershell
$env:DEEPSEEK_MODEL="deepseek-v4-pro"
```

## 本地启动

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" spring-boot:run
```

启动后访问：

```text
http://localhost:8080
```

## 服务器配置 DeepSeek API Key

线上服务通过 systemd 读取这个环境变量文件：

```text
/etc/ai-search-food/ai-search-food.env
```

在服务器上编辑它：

```bash
vi /etc/ai-search-food/ai-search-food.env
```

配置内容示例：

```env
DEEPSEEK_API_KEY=你的DeepSeek API Key
DEEPSEEK_MODEL=deepseek-v4-flash
```

保存后重启服务：

```bash
systemctl restart ai-search-food
```

查看运行状态和日志：

```bash
systemctl status ai-search-food
journalctl -u ai-search-food -f
```

## 接口

搜索菜谱：

```http
GET /api/recipes/search?keyword=鸡蛋
```

AI 生成菜谱：

```http
POST /api/recipes/generate
Content-Type: application/json

{
  "ingredients": "鸡蛋、番茄、青椒"
}
```

线上 API 地址：

```http
GET http://8.166.138.245/api/recipes/search?keyword=鸡蛋
POST http://8.166.138.245/api/recipes/generate
```

## 打包

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" package
```

构建产物：

```text
target/ai-search-food-0.0.1-SNAPSHOT.jar
```

## 线上部署位置

服务器上的主要文件：

```text
/opt/ai-search-food/app.jar
/opt/java/jre17
/etc/systemd/system/ai-search-food.service
/etc/nginx/conf.d/ai-search-food.conf
/etc/ai-search-food/ai-search-food.env
```

本地部署配置模板在 `.deploy/` 目录中。
