# 云枢智学 · 运营后台（admin-web）

运营端单页应用，独立工程、独立构建产物与令牌域，部署于同域 `/admin/` 路径（设计文档 D9/D20）。

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
admin-web/
├── src/
│   ├── api/                 # 请求封装与认证接口
│   ├── components/          # 通用组件（图表、占位）
│   ├── directives/          # v-permission 权限指令
│   ├── layouts/             # 后台布局（侧边栏 / 顶栏）
│   ├── router/              # 路由与登录守卫
│   ├── store/               # 用户、权限 store
│   ├── styles/              # 全局样式与品牌变量
│   ├── types/               # 公共类型与 OpenAPI 生成类型
│   ├── utils/               # 令牌存储等工具
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
pnpm dev          # http://localhost:5174/admin/
pnpm type-check   # TypeScript 校验
pnpm build        # 类型校验 + 生产构建，产物在 dist/
```

未安装 pnpm 时也可使用 npm：`npm install && npm run dev`。

开发期 Vite 将 `/api` 代理到 `http://localhost:8080`，将 `/files` 代理到 MinIO 桶；`base` 固定为 `/admin/`，与生产 Nginx 路由保持一致。

## 环境变量

| 变量 | 说明 | 开发默认值 |
| --- | --- | --- |
| `VITE_APP_TITLE` | 浏览器标题 | 云枢智学 · 运营后台 |
| `VITE_APP_BASE` | 应用基础路径 | `/admin/` |
| `VITE_DEV_PORT` | 开发服务器端口 | `5174` |
| `VITE_API_BASE_URL` | 接口前缀 | `/api/admin/v1` |
| `VITE_PROXY_TARGET` | 后端代理目标 | `http://localhost:8080` |
| `VITE_MINIO_TARGET` | MinIO 代理目标 | `http://localhost:9000` |
| `VITE_MINIO_BUCKET` | 文件桶名 | `yszx-files` |
| `VITE_PORTAL_URL` | 学员前台地址（顶栏「返回学员前台」） | `http://localhost:5173` |
| `VITE_AUTH_BYPASS` | 开发期布局预览开关：`true` 时跳过前端登录守卫（后端仍校验） | `false` |

## 已封装的工程能力

- **统一响应解包**：按 `{ code, message, data, fieldErrors, fileErrors }` 约定返回 `data`，非 0 业务码统一抛出 `ApiError` 并提示；403/10002 由请求层提示。
- **凭证刷新**：401 时使用 `X-Refresh-Token` 调用管理端刷新接口，并发请求排队等待新令牌后重放；刷新失败清理本地令牌并跳转 `/admin/login`。
- **双令牌隔离**：管理端令牌存储于 `yszh.admin.access_token` / `yszh.admin.refresh_token`，与学员端互不通用。
- **登录守卫**：布局内路由均要求登录，登录后回跳 `redirect` 参数。
- **统一页面布局**：`AdminLayout` 提供分组侧边栏（可折叠并记忆状态）、顶栏面包屑、通知与用户菜单；页面通过 `PageHeader` + `PagePlaceholder` 统一「页头 + 内容区」结构。
- **权限骨架**：路由 `meta.permission` 标注权限码；`usePermissionStore` 维护权限码集合（`*` 为超管）；`v-permission` 指令与侧边栏过滤已就绪。
- **OpenAPI 类型**：后端启动后执行 `pnpm gen:types` 生成 `src/types/generated/api.d.ts`，业务接口类型以生成结果为准。

## 登录与开发期放行

管理端登录、刷新、登出接口已接入。默认 `VITE_AUTH_BYPASS=false`：未登录访问布局路由会跳转 `/admin/login`，登录成功后回到 `redirect` 指定页面。

开发期如需临时预览布局，可显式设置 `VITE_AUTH_BYPASS=true` 跳过前端路由守卫：

- 该开关仅决定前端路由是否放行，所有真实接口仍由后端安全链校验，未携带有效管理端令牌会返回 401；
- 生产构建不加载 `.env.development`，因此默认不生效。

## 权限加载

- 登录成功后调用 `GET /auth/me`（A-A04）获取当前管理员角色与权限码，写入 `usePermissionStore`；
  超级管理员返回 `["*"]`。
- 侧边栏菜单、路由守卫与 `v-permission` 均按权限码过滤；角色管理路由仅超级管理员可访问。
- 刷新页面时路由守卫会重新拉取当前管理员信息；权限保存后后端按库实时校验。
- 前端控制仅用于菜单与操作入口展示，真正的访问控制以服务端 RBAC 切面（403 + 10002）为准。

## 页面骨架状态

| 模块 | 页面 | 状态 |
| --- | --- | --- |
| 登录 | 管理员登录 | 已对接登录 / 当前管理员信息 / 登出 |
| 看板 | 数据看板 | 统计卡 / 图表 / 活动表的界面骨架，接口待接入 |
| 内容管理 | 学科管理 | 已完成：统计卡、学科/阶段新增编辑、排序、启停、删除保护 |
| 内容管理 | 课程库、题库、面试题库、学习路线 | 占位页 |
| 学员管理 | 学员列表 | 占位页 |
| 系统管理 | 用户管理 | 已完成：筛选、批量启停/删除、新增/编辑、详情、重置密码 |
| 系统管理 | 角色权限 | 已完成：角色卡片、新增/删除、9 组 34 项权限树、全量覆盖保存 |
| 系统管理 | 操作日志 | 已完成：筛选、分页、详情、CSV 导出、清空 |
| 系统管理 | 系统设置 | 已完成：基本信息、Logo/默认头像上传、保存与恢复默认 |
| 其他 | 403 / 404 | 可运行 |

业务功能在后续迭代中按设计文档实现，不在本次基础工程范围内。
