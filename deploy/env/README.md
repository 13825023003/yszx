# 部署与环境变量说明

本目录（`deploy/`）承载开发/测试环境的一键基础设施编排，开发与生产同构（见 `docs/design/02.整体架构设计.md` §2.4）。

## 1. 目录

```
deploy/
├── docker-compose.yml      # MySQL 8.0 + Redis 7 + MinIO（+ 一次性 bucket 初始化）
├── env/
│   ├── .env.example        # 环境变量模板
│   └── README.md           # 本文件
└── .gitignore              # 忽略 .env
```

## 2. 启动步骤

```powershell
cd deploy
Copy-Item env\.env.example .env    # 首次使用；按需修改口令
docker compose up -d
docker compose ps -a               # 三个服务应为 healthy，minio-init 为 exited(0)
```

Bash 环境使用 `cp env/.env.example .env`，其余命令相同。

停止：`docker compose down`；清空并重建（会删除所有数据）：`docker compose down -v`。

## 3. 服务与端口

| 服务 | 容器名 | 宿主机地址 | 容器内地址 | 说明 |
|---|---|---|---|---|
| MySQL 8.0 | `yszx-mysql` | `localhost:3306` | `mysql:3306` | 库名 `yszx`，utf8mb4 / utf8mb4_0900_ai_ci，时区 UTC |
| Redis 7 | `yszx-redis` | `localhost:6379` | `redis:6379` | AOF 持久化，需密码 |
| MinIO API | `yszx-minio` | `http://localhost:9000` | `http://minio:9000` | S3 兼容接口，后端 SDK 使用 |
| MinIO 控制台 | `yszx-minio` | `http://localhost:9001` | — | 浏览器登录，账号见 `.env` |
| Bucket 初始化 | `yszx-minio-init` | — | — | 启动一次后退出，创建 `yszx-files` 桶并开放匿名只读 |

后端在容器内运行时使用服务名；在宿主机运行（IDE 调试）时使用 `localhost` + 映射端口。

> **MinIO 镜像说明**：MinIO 官方已停止公开发布社区版镜像（2026-11 实测：Docker Hub / quay.io 拒绝拉取，`dl.min.io` 返回 410）。
> 编排改用 Bitnami 维护的冻结镜像 `bitnamilegacy/minio`（内置 MinIO 2025.5.24 + mc），S3 API、端口、环境变量不变，
> 仅数据目录改为 `/bitnami/minio/data`（容器内，不需关心）。Bitnami 镜像默认关闭 9001 控制台，编排已通过
> `MINIO_BROWSER=on` 打开。该镜像已冻结、不再更新，适合开发/测试；
> 生产若需要持续安全更新，建议评估改用其他 S3 兼容存储或云 OSS（见第 7 节）。

## 4. 连接示例

JDBC（Spring Boot `application-dev.yml`）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/yszx?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER:yszx}
    password: ${MYSQL_PASSWORD:YszxApp_2026}
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:YszxRedis_2026}
```

MinIO（S3 客户端）：

```yaml
endpoint: http://localhost:9000
access-key: ${MINIO_ROOT_USER:yszxminio}
secret-key: ${MINIO_ROOT_PASSWORD:YszxMinio_2026}
bucket: ${MINIO_BUCKET:yszx-files}
```

## 5. 建表脚本的执行时机

`scripts/sql/schema.sql` 通过 `/docker-entrypoint-initdb.d` 挂载，**仅在 `yszx-mysql-data` 数据卷为空（首次初始化）时执行一次**。之后修改表结构的处理方式：

- 开发期重置：`docker compose down -v` 后重新 `up -d`（数据会清空）；
- 增量变更：按 `docs/rules/database-rules.md` 编写迁移脚本，手动导入：

```powershell
# 用 cmd 做输入重定向，保证 UTF-8 中的中文初始数据不被 PowerShell 转码破坏
cmd /c "docker exec -i yszx-mysql mysql -uroot -p<你的root密码> < ..\scripts\sql\schema.sql"
```

## 6. 常用运维命令

```powershell
docker compose logs -f mysql          # 查看某个服务日志
docker compose restart redis          # 重启单个服务
docker exec -it yszx-mysql mysql -uroot -p   # 进入 MySQL 命令行
docker exec -it yszx-redis redis-cli -a <密码>  # 进入 Redis 命令行
```

MinIO 桶管理建议直接打开控制台 <http://localhost:9001>；如需命令行，可用一次性容器：

```powershell
docker run --rm --network yszx-net `
  -e "MC_HOST_local=http://<ROOT_USER>:<ROOT_PASSWORD>@minio:9000" `
  minio/mc ls local
```

备份与恢复（示例）：

```powershell
cmd /c "docker exec yszx-mysql mysqldump -uroot -p<密码> --single-transaction --routines --events yszx > backup.sql"
cmd /c "docker exec -i yszx-mysql mysql -uroot -p<密码> yszx < backup.sql"
```

## 7. 常见问题

| 现象 | 原因与处理 |
|---|---|
| 启动报错“请先复制 env/.env.example 为 .env” | 未创建 `deploy/.env`，按第 2 节复制后重试 |
| 镜像拉取超时 | 国内网络限制，把 `.env` 中的 `*_IMAGE` 换成镜像站地址（模板中已给示例） |
| 报错 `minio/mc ... denied` 或 `No such image: minio/minio` | 旧配置仍指向官方 MinIO 镜像，而官方已停止公开分发。确认 `deploy/.env` 中 `MINIO_IMAGE` / `MC_IMAGE` 为 `bitnamilegacy/*`（国内可加 `docker.m.daocloud.io/` 前缀），改完执行 `docker compose up -d` |
| 长期方案：MinIO 冻结镜像如何替换 | 后端通过 S3 兼容接口访问，换存储只改 `endpoint` / 凭据 / 桶名。可选：云 OSS/COS（改配置即可）、其他自建 S3 兼容服务（需在 `docker-compose.yml` 增加服务并更新后端配置） |
| 修改了 schema.sql 但库里没变化 | 初始化脚本只在空数据卷时执行，见第 5 节 |
| 从宿主机连 MySQL 报 Access denied | 宿主机请用 `MYSQL_USER`（对 `yszx` 库有全部权限），root 默认只允许容器内登录 |
| MinIO 控制台 9001 打不开 | 确认容器健康：`docker compose ps`、`docker compose logs minio` |
| 时间相差 8 小时 | 数据库统一按 UTC 连接（JDBC 已带 `serverTimezone=UTC`），展示层再转 Asia/Shanghai |

## 8. 安全提醒

- 默认口令仅用于本地开发，生产环境必须在 `.env` 中替换，且不应提交到仓库；
- 生产环境建议将 `3306` / `6379` / `9000` / `9001` 端口限制在内网或仅绑定 `127.0.0.1`，仅通过 Nginx 暴露 `/files` 与业务接口；
- `minio-init` 将业务桶设为匿名只读（供 `/files/**` 直链访问），如后续改为预签名访问，删除该步并调整 Nginx 反代。
