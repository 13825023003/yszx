# 云枢智学 · 学员端（portal-web）

学员端单页应用，独立工程、独立构建产物与令牌域，部署于同域根路径 `/`（设计文档 D9/D20）。

## 技术栈

| 分类 | 选型 |
| --- | --- |
| 框架 | Vue 3.5 + TypeScript 5.6 |
| 构建 | Vite 5 |
| 状态管理 | Pinia 2 |
| 路由 | Vue Router 4 |
| 组件库 | Element Plus 2.8 |
| 图表 | ECharts 5 |
| HTTP | Axios 1.7 |

## 目录结构

```text
portal-web/
├── src/
│   ├── api/                 # 请求封装与接口模块（含 SSE 客户端）
│   ├── components/          # 通用组件（图表、占位、认证外壳）
│   ├── layouts/             # 页面布局
│   ├── router/              # 路由与访问守卫
│   ├── store/               # Pinia store（用户、练习、面试）
│   ├── styles/              # 全局样式与品牌变量
│   ├── types/               # 公共类型与 OpenAPI 生成类型
│   ├── utils/               # 工具函数（令牌存储等）
│   └── views/               # 页面
├── public/
├── .env.development
├── .env.production
├── vite.config.ts
└── tsconfig.json
```

## 本地开发

要求 Node.js >= 20，推荐使用 pnpm 9（`packageManager` 已声明）。

```bash
pnpm install
pnpm dev          # http://localhost:5173
pnpm type-check   # TypeScript 校验
pnpm build        # 类型校验 + 生产构建，产物在 dist/
```

未安装 pnpm 时也可使用 npm：`npm install && npm run dev`。

开发期 Vite 将 `/api` 代理到 `http://localhost:8080`，将 `/files` 代理到 MinIO 桶，模拟 Nginx 的同域路由与静态文件直链。

## 环境变量

| 变量 | 说明 | 开发默认值 |
| --- | --- | --- |
| `VITE_APP_TITLE` | 浏览器标题 | 云枢智学 |
| `VITE_APP_BASE` | 应用基础路径 | `/` |
| `VITE_DEV_PORT` | 开发服务器端口 | `5173` |
| `VITE_API_BASE_URL` | 接口前缀 | `/api/portal/v1` |
| `VITE_PROXY_TARGET` | 后端代理目标 | `http://localhost:8080` |
| `VITE_MINIO_TARGET` | MinIO 代理目标 | `http://localhost:9000` |
| `VITE_MINIO_BUCKET` | 文件桶名 | `yszx-files` |

生产环境由 Nginx 把 `/` 指向学员端产物、`/api` 反向代理到后端，因此无需配置 CORS。

## 已封装的工程能力

- **统一响应解包**：`src/api/request.ts` 按 `{ code, message, data, fieldErrors, fileErrors }` 约定返回 `data`，非 0 业务码统一抛出 `ApiError` 并提示。
- **凭证刷新**：401 时使用 `X-Refresh-Token` 调用刷新接口，并发请求排队等待新令牌后重放；刷新失败清理本地令牌并跳转登录页。
- **双令牌隔离**：访问令牌与刷新令牌分别存储于 `yszh.portal.access_token` / `yszh.portal.refresh_token`，与运营端互不影响。
- **路由守卫**：`requiresAuth` / `guestOnly` 元信息控制访问，登录后回跳 `redirect` 参数。
- **SSE 客户端**：`src/api/sse.ts` 基于 `fetch + ReadableStream` 消费 `status` / `token` / `grade` / `error` 事件帧，用于 AI 面试流式反馈。
- **OpenAPI 类型**：后端启动后执行 `pnpm gen:types` 生成 `src/types/generated/api.d.ts`，业务接口类型以生成结果为准。

## 页面骨架状态

| 页面 | 状态 |
| --- | --- |
| 首页 | 可运行，展示品牌与功能入口 |
| 题库中心 | 筛选与列表骨架 |
| 刷题练习 / 错题本 / 学习路线 | 占位页 |
| AI 面试（配置 / 会话 / 记录 / 报告） | 占位页，SSE 客户端已就绪 |
| 个人中心 | 占位页 |
| 登录 | 对接密码登录接口的可用表单 |
| 注册 / 找回密码 | 表单骨架，待认证模块接口就绪后接入 |
| 404 | 可运行 |

业务页面在后续迭代中按设计文档实现，不在本次基础工程范围内。
