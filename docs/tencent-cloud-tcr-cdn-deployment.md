# 腾讯云 TCR 镜像发布与 CDN 部署

本文用于 4 核、4GB 内存、3Mbps 带宽的腾讯云服务器。生产服务器只拉取 TCR 镜像并启动容器，不在服务器上拉 GitHub 源码或现场构建。

## 1. 上线前置条件

开始前准备以下信息，但不要写入仓库或聊天记录：

- TCR 仓库域名、命名空间，以及后端和前端仓库名称。
- TCR 临时登录凭证或用户级访问凭证。
- 服务器 SSH 地址与登录方式。
- 已备案（中国大陆加速时）的站点域名、可用 HTTPS 证书，以及可修改 DNS 的权限。
- CDN 加速域名和源站地址。建议加速域名直接作为用户访问域名，源站填服务器公网 IP，并正确设置回源 Host。

首次开通 TCR 企业版、CDN、HTTPS 请求或 QUIC 可能产生费用。应先确认地域、计费方式和预算告警，再执行购买、开通或生产 DNS 修改。

## 2. 生产配置说明

`docker-compose.prod.yml` 与本地 `docker-compose.yml` 分离：

- 生产配置只引用 TCR 镜像，镜像仓库和版本完全由 `.env.prod` 指定。
- 项目名固定为 `ai-search-food`，继续使用 `mysql_data`、`review_uploads`、`user_avatars` 三个命名卷。
- MySQL 仅在内部网络开放；后端不映射宿主机端口；只有前端 Nginx 对外提供服务。
- 后端内存上限 1536MB，JVM 堆上限 1024MB；MySQL 内存上限 1024MB、InnoDB 缓冲池 384MB；Nginx 内存上限 256MB。总容器上限约 2.75GB，为操作系统、Docker 和突发开销保留空间。
- 容器日志启用轮转，避免长期运行挤满磁盘。

这些参数优先保证 4GB 主机稳定性。上线后根据 `docker stats`、JVM 与 MySQL 实际指标调整，不应盲目增大堆或连接数。

## 3. 构建并推送 TCR

在可信的构建机上从目标提交构建。标签必须是唯一版本，例如 `20260907-001` 或 `git-<短提交号>`，不要只发布 `latest`。

PowerShell 示例：

```powershell
$env:TCR_REGISTRY = '填写 TCR 仓库域名，不带 https://'
$env:TCR_NAMESPACE = '填写命名空间'
$env:IMAGE_TAG = '20260907-001'

docker login $env:TCR_REGISTRY

$backendImage = "$env:TCR_REGISTRY/$env:TCR_NAMESPACE/ai-search-food-backend:$env:IMAGE_TAG"
$frontendImage = "$env:TCR_REGISTRY/$env:TCR_NAMESPACE/ai-search-food-frontend:$env:IMAGE_TAG"

docker build --pull -t $backendImage .\backend
docker build --pull -t $frontendImage .\frontend
docker push $backendImage
docker push $frontendImage
```

登录密码只通过交互输入，或以临时环境变量配合 `--password-stdin` 使用。发布完成后清除当前会话中的临时凭证。不要把凭证写进 Dockerfile、Compose、`.env.prod`、脚本或文档。

推送后记录两个镜像的 digest：

```powershell
docker image inspect $backendImage --format '{{index .RepoDigests 0}}'
docker image inspect $frontendImage --format '{{index .RepoDigests 0}}'
```

## 4. 服务器首次部署

服务器只需要 Docker、Compose、`docker-compose.prod.yml` 和本机私有的 `.env.prod`。将 Compose 文件复制到例如 `/opt/ai-search-food/`，再从示例创建生产环境文件：

```bash
cd /opt/ai-search-food
cp .env.prod.example .env.prod
chmod 600 .env.prod
```

填写 `.env.prod` 后先校验配置。`IMAGE_TAG` 必须是已推送到 TCR 的不可变版本标签：

```bash
TCR_REGISTRY=$(sed -n 's/^TCR_REGISTRY=//p' .env.prod)
docker login "$TCR_REGISTRY"
docker compose --env-file .env.prod -f docker-compose.prod.yml config --quiet
docker compose --env-file .env.prod -f docker-compose.prod.yml pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --wait
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
curl --fail --silent http://127.0.0.1/healthz
```

若旧部署使用同一项目名，命名卷会被复用。首次切换前仍需执行数据库备份并确认卷名：

```bash
docker volume ls --filter name=ai-search-food
mkdir -p backups
chmod 700 backups
docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T mysql \
  sh -c 'exec mysqldump --single-transaction -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  > "backups/pre-tcr-$(date +%Y%m%d-%H%M%S).sql"
```

禁止执行 `docker compose down -v`，也不要删除或重新创建生产数据卷。

## 5. 无源码更新与回滚

更新前记录当前标签，并先拉取新镜像。`up -d` 只重建镜像变化的服务，不会删除命名卷；前后端重建期间可能出现短暂不可用，因此应在维护窗口执行。

```bash
cd /opt/ai-search-food
OLD_IMAGE_TAG=$(sed -n 's/^IMAGE_TAG=//p' .env.prod)
printf '%s\n' "$OLD_IMAGE_TAG" > .previous-image-tag

NEW_IMAGE_TAG='填写新版本标签'
sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$NEW_IMAGE_TAG/" .env.prod

docker compose --env-file .env.prod -f docker-compose.prod.yml config --quiet
docker compose --env-file .env.prod -f docker-compose.prod.yml pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --wait
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
curl --fail --silent http://127.0.0.1/healthz
```

若健康检查失败，切回旧标签：

```bash
OLD_IMAGE_TAG=$(cat .previous-image-tag)
sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$OLD_IMAGE_TAG/" .env.prod
docker compose --env-file .env.prod -f docker-compose.prod.yml pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --wait
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
```

镜像回滚不能自动撤销 Flyway 数据库迁移。含数据库变更的版本发布前，必须单独确认迁移向后兼容并保留可用备份。

## 6. CDN 推荐配置

建议选择“网页小文件”业务类型，将整个站点域名接入 CDN，再按路径配置缓存。腾讯云 CDN 多条缓存规则以列表底部为更高优先级，保存后要再次确认实际排序。

### 节点缓存和浏览器缓存

按从低到高优先级配置：

| 匹配规则 | CDN 节点缓存 | 浏览器缓存 | 说明 |
| --- | ---: | ---: | --- |
| 全部文件 | 不缓存 | 不缓存 | 安全默认值，避免漏掉动态路径 |
| 文件夹 `/images/` | 30 天 | 30 天 | 公共食材图片 |
| 文件夹 `/sprites/` | 7 天 | 7 天 | 文件名未可靠版本化，不使用 `immutable` |
| 文件夹 `/assets/` | 365 天 | 365 天 | Vite 生成的带哈希 JS、CSS、字体等，可设 `immutable` |
| 文件类型 `html;htm` | 不缓存 | 不缓存 | 保证新版本入口立即生效 |
| 首页 `/` 与全路径 `/index.html` | 不缓存 | 不缓存 | SPA 入口不强缓存 |
| 文件夹 `/api/` | 不缓存 | 不缓存 | 最高优先级；涵盖登录、上传、头像、AI 流式和个性化接口 |

缓存键默认保留完整查询参数。除非确认参数不影响资源内容，否则不要开启全局“忽略参数”。不要缓存 301/302、401、403、404、429、5xx 等状态码。

源站 Nginx 已同时返回相应 `Cache-Control`，并对 `/api/` 增加 `no-store`。CDN 控制台仍需显式配置 `/api/` 不缓存，不能只依赖源站响应头。

### HTTPS、协议和压缩

- 部署有效证书，开启 HTTP 到 HTTPS 的 301 强制跳转和 HTTP/2。
- 确认 HTTPS 全站稳定后再开启 HSTS；初始可使用较短有效期，不要立即包含所有子域。
- 在“高级配置 > 智能压缩”同时开启 Gzip 和 Brotli，按 Content-Type 覆盖 HTML、CSS、JavaScript、JSON、XML、SVG 和文本。不要压缩 PNG、JPEG、WebP、GIF、视频等已压缩格式。
- 源站 Nginx 保留 Gzip，供直连源站或 CDN 首次回源场景使用。若 CDN 响应始终没有 `br`，检查回源请求是否携带 `Accept-Encoding`；CDN 收到已带 `Content-Encoding` 的源站响应时不会再次做 Brotli 压缩。
- QUIC/HTTP/3 只有在证书配置完成后才能开启，而且按请求量计费。确认预算后再启用；它只用于客户端到 CDN 节点，CDN 不使用 QUIC 回源。

### 域名和回源安全

- 中国大陆 CDN 加速域名需满足备案要求。添加域名后，先使用腾讯云分配的 CNAME 测试，再修改生产 DNS。
- 回源 Host 必须与源站 Nginx/证书匹配。若源站只开放 HTTP，CDN 回源协议选 HTTP；若源站已有有效证书，优先 HTTPS 回源。
- 安全组只开放实际需要的 80/443 和受限来源的 SSH。若使用 CDN 回源 IP 白名单，必须同时准备规则更新机制，避免节点 IP 变化导致回源失败。
- 配置带宽/流量告警和访问频控，防止 3Mbps 源站被回源洪峰打满或产生异常 CDN 费用。

## 7. 上线验证清单

源站验证：

```bash
curl -I http://127.0.0.1/index.html
curl -I -H 'Accept-Encoding: gzip' http://127.0.0.1/assets/实际文件.js
curl -I http://127.0.0.1/sprites/1-D-1.png
curl -I http://127.0.0.1/任意前端路由
curl -I http://127.0.0.1/api/任意健康的只读接口
```

期望结果：

- HTML 与 SPA 路由响应 `Cache-Control: no-store`。
- `/assets/` 响应一年缓存和 `immutable`，压缩请求返回 `Content-Encoding: gzip` 及 `Vary: Accept-Encoding`。
- `/sprites/` 仅缓存 7 天且没有 `immutable`；PNG/JPEG 不返回 Gzip。
- `/api/` 响应 `Cache-Control: no-store`，流式接口无代理缓冲。
- `docker compose ps` 中 MySQL、后端和前端均为 healthy。

CDN 生效后，把上述地址替换为 HTTPS 生产域名重复验证，并确认：证书链正确、HTTP 自动跳 HTTPS、HTTP/2 可用；支持 Brotli 的客户端获得 `Content-Encoding: br`；连续请求静态资源可命中节点，而 `/api/` 始终不命中缓存。生产 DNS 切换应安排回滚窗口，并保留原记录值和 TTL。

## 8. 腾讯云官方参考

- [TCR 个人版快速入门](https://cloud.tencent.com/document/product/1141/63910)
- [TCR 企业版快速入门](https://cloud.tencent.com/document/product/1141/39287)
- [CDN 节点与浏览器缓存配置](https://cloud.tencent.com/document/product/228/41534)
- [CDN 智能压缩（Gzip/Brotli）](https://cloud.tencent.com/document/product/228/41736)
- [CDN HTTPS 配置指南](https://cloud.tencent.com/document/product/228/41687)
- [CDN QUIC 配置与计费说明](https://cloud.tencent.com/document/product/228/51800)
