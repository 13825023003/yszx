# 云枢智学 · 后端工程

Spring Boot 3.3 / Java 17 的模块化单体，按业务领域分包。详细设计见
[`docs/design/02.整体架构设计.md`](../docs/design/02.整体架构设计.md)。

## 1. 技术栈

| 能力 | 选型 |
|---|---|
| Web / 校验 | Spring MVC + Jakarta Validation |
| 安全 | Spring Security + JWT（JJWT，学员端/管理端双令牌域） |
| 持久层 | MyBatis-Plus 3.5.7 + MySQL 8 |
| 缓存 | Redis 7（`StringRedisTemplate` + JSON `RedisTemplate`） |
| 对象存储 | MinIO（S3 兼容，`StorageService` 抽象） |
| 接口契约 | springdoc-openapi（`/v3/api-docs`） |
| Excel / Markdown | FastExcel（EasyExcel 继任者）/ commonmark-java |

## 2. 快速启动

```bash
# 1. 启动 MySQL + Redis + MinIO（首次使用先复制 .env）
cd deploy
cp env/.env.example .env
docker compose up -d

# 2. 启动后端（dev profile，默认连 localhost）
cd ../backend
mvn spring-boot:run
```

默认端口 `8080`：

- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>
- 健康检查：<http://localhost:8080/actuator/health>

开发环境默认口令与 `deploy/env/.env.example` 对齐；生产 profile 的所有连接信息与密钥
必须由环境变量提供，缺失即启动失败。

## 3. 环境变量

| 变量 | 说明 |
|---|---|
| `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DATABASE` / `MYSQL_USER` / `MYSQL_PASSWORD` | 数据库连接 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 |
| `JWT_PORTAL_SECRET` / `JWT_ADMIN_SECRET` | 两端 JWT 密钥（至少 32 字节） |
| `MINIO_ENDPOINT` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` / `MINIO_BUCKET` | 对象存储 |
| `AI_BASE_URL` / `AI_API_KEY` / `AI_MODEL` | 大模型（OpenAI 兼容协议） |
| `APP_DEFAULT_PASSWORD` | 内置管理员初始密码 / 后台重置密码的默认值（开发默认 `Yszx@123456`，生产必须覆盖） |

## 4. 工程结构

```
com.yunshu.zhixue
├── common/            # 统一响应、错误码、异常处理、常量、工具、实体基类
├── config/            # MyBatis-Plus、Redis、Jackson、OpenAPI、异步、MinIO
├── security/          # JWT 双令牌域、认证过滤器、黑名单、权限注解与切面
├── modules/           # 12 个业务领域（包内再分 controller/service/mapper/entity/dto/convert）
│   ├── auth/ system/ student/ subject/ course/ question/
│   └── interviewbank/ route/ study/ interview/ stats/ ai/
└── support/           # 横切支撑：对象存储抽象等
```

**模块依赖硬约束**（02 §3.2）：

1. 模块间单向依赖，禁止反向调用；
2. 不跨模块直接访问对方 Mapper/实体，只经 Service 接口与 DTO；
3. `security`、`common`、`support` 为最底层，不依赖业务模块；
4. `ai` 无状态、不落库；`stats` 只读。

## 5. 已内置的基础能力

- 统一响应 `ApiResponse`、分页 `PageResult`、错误码 `ErrorCode`、全局异常处理；
- 双端独立 JWT 签发/校验、Redis 登出黑名单、refresh token 快照；
- 管理端登录 / 刷新 / 登出（`/api/admin/v1/auth/*`）：连续 5 次失败锁定 5 分钟、
  禁用账号拦截、refresh token 轮换与幂等登出；账号禁用/删除后旧 access token
  经状态校验立即失效；
- `GET /auth/me` 返回当前管理员角色与权限码，`PermissionProvider` 按库实时加载，
  供管理端菜单与按钮渲染；
- 启动时自动创建内置超级管理员 `admin`（仅当不存在时），初始密码取
  `APP_DEFAULT_PASSWORD`，角色为 `super_admin`；
- 系统管理模块：后台用户 CRUD/批量启停/重置密码、角色与 9 组 34 项权限树、
  操作日志（记录/检索/CSV 导出/清空）、系统设置（4 项配置 upsert）与图片上传；
- 学科与阶段管理：统计卡、学科/阶段 CRUD、上下移排序、阶段启停、删除保护；
  关联数量由 course/question/route/interviewbank 通过 subject 模块 SPI 汇总，
  不跨模块直连数据库；
- B站课程库：统计卡、课程 CRUD/筛选/分页、主学科 + 额外学科、BV 唯一性、
  被学习路线引用时阻止删除；Excel 两段式导入（预览 token + 确认），
  列格式为课程名称/所属学科/来源/时长（分钟）/BV号/封面/播放链接；
- 学习路线：路线 CRUD、发布/下架、学员预览、路线课程添加/移除/排序；
  删除路线同步逻辑删除 `route_course`，课程被路线引用时反向阻止删除；
- 学员端/管理端两条独立 SecurityFilterChain，公开路径按 API 04 预置；
- `@RequirePermission` 权限注解与切面（`PermissionProvider` 待 system 模块实现）；
- MyBatis-Plus 分页、全表更新拦截、UTC 审计字段自动填充、逻辑删除基类；
- `deleted_token` 逻辑删除工具（唯一键释放，R4）；
- MinIO 存储抽象与上传/删除实现；
- 上海业务日界工具（每日配额/验证码日计数，D17）；
- OpenAPI 契约与两种 Bearer 认证方案。

## 6. 实施约定

- 实体继承：普通表继承 `BaseEntity`；仅有 `delete_flag` 的表继承 `LogicDeleteEntity`；
  含 `deleted_token` 的表继承 `TokenizedLogicDeleteEntity`。
- 含 `deleted_token` 的表删除时必须使用 `LogicDeleteUtils.markDeleted(id)`，
  以保证 `delete_flag=1` 与 `deleted_token=id` 同时写入。
- 接口返回统一使用 `ApiResponse`，分页统一使用 `PageResult`；
  业务异常统一抛 `BizException`。
- 管理端接口权限：`@RequirePermission("question:edit")`（权限码见 API 04 附录 A）。
- 前端业务 DTO 类型不在本仓库手写，统一由 OpenAPI 契约生成。

## 7. 构建

```bash
mvn clean package          # 产物 target/yunshu-zhixue-backend-0.1.0-SNAPSHOT.jar
mvn test                   # 单元测试
```
