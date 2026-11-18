# 云枢智学

面向计算机专业学生的题库练习、学习路线与 AI 模拟面试平台。采用「模块化单体后端 + 学员端 / 运营端两个独立 SPA」的架构，同域部署、单一数据库与 Redis、MinIO 对象存储。

## 工程结构

```text
云枢智学/
├── backend/          # Spring Boot 3.3 模块化单体（Java 17）
├── portal-web/       # 学员端 SPA（Vue 3 + TS + Vite，部署于 /）
├── admin-web/        # 运营端 SPA（Vue 3 + TS + Vite，部署于 /admin/）
├── deploy/           # Docker Compose 基础设施（MySQL / Redis / MinIO）
├── scripts/sql/      # 建库建表脚本
├── docs/             # 需求、设计、规则文档
└── prototype/        # 页面原型
```

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.3.5、Spring Security + JJWT、MyBatis-Plus 3.5.7 |
| 存储 | MySQL 8、Redis 7、MinIO |
| 前端 | Vue 3.5、TypeScript 5.6、Vite 5、Pinia 2、Vue Router 4、Element Plus 2.8、ECharts 5 |
| 接口文档 | springdoc-openapi 2.6 |

## 快速开始

### 1. 启动基础设施

```bash
cd deploy
cp env/.env.example .env   # 首次使用，按需修改端口与密码
docker compose up -d
```

启动 MySQL（`localhost:3306`）、Redis（`localhost:6379`）、MinIO（`localhost:9000`，控制台 `9001`）。首次启动会自动执行 `scripts/sql/schema.sql` 建库建表。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认使用 `dev` profile，连接本地容器；接口地址 `http://localhost:8080/api`，Swagger UI `http://localhost:8080/swagger-ui.html`。

### 3. 启动前端

```bash
cd portal-web && pnpm install && pnpm dev   # http://localhost:5173
cd admin-web  && pnpm install && pnpm dev   # http://localhost:5174/admin/
```

未安装 pnpm 时可使用 `npm install && npm run dev`。开发期 Vite 将 `/api` 代理到后端、`/files` 代理到 MinIO。

## 已搭建的基础工程

- **后端**：统一响应与错误码、全局异常处理、双令牌域 JWT 认证、Redis 令牌黑名单与刷新快照、RBAC 权限注解切面、MyBatis-Plus 逻辑删除与审计字段、MinIO 存储、OpenAPI、模块分包与依赖约束、生产/开发 profile 分离。
- **学员端**：路由与登录守卫、统一请求解包、401 刷新排队、SSE 客户端、品牌样式、首页与登录页可用，其余页面为骨架。
- **运营端**：后台布局与菜单、登录页、数据看板骨架、权限 store 与 `v-permission` 指令，业务页面为骨架。

## 文档

- 需求与设计：[docs/](docs/)
- 数据库设计规则：[docs/rules/database-rules.md](docs/rules/database-rules.md)
- API 设计规则：[docs/rules/api-rules.md](docs/rules/api-rules.md)
- 后端说明：[backend/README.md](backend/README.md)
- 学员端说明：[portal-web/README.md](portal-web/README.md)
- 运营端说明：[admin-web/README.md](admin-web/README.md)
