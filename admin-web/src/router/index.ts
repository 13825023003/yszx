import { createRouter, createWebHistory, type RouteLocationNormalized, type RouteRecordRaw } from 'vue-router'
import { usePermissionStore } from '@/store/permission'
import { useSettingStore } from '@/store/setting'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '数据看板', requiresAuth: true, permission: 'dashboard:view' },
      },
      {
        path: 'content/subjects',
        name: 'subjects',
        component: () => import('@/views/content/SubjectView.vue'),
        meta: { title: '学科管理', requiresAuth: true, permission: 'subject:view' },
      },
      {
        path: 'content/courses',
        name: 'courses',
        component: () => import('@/views/content/CourseView.vue'),
        meta: { title: '课程库', requiresAuth: true, permission: 'course:view' },
      },
      {
        path: 'content/questions',
        name: 'questions',
        component: () => import('@/views/content/QuestionView.vue'),
        meta: { title: '题库管理', requiresAuth: true, permission: 'question:view' },
      },
      {
        path: 'content/interview-bank',
        name: 'interview-bank',
        component: () => import('@/views/content/InterviewBankView.vue'),
        meta: { title: '面试题库', requiresAuth: true, permission: 'interviewbank:view' },
      },
      {
        path: 'content/routes',
        name: 'routes',
        component: () => import('@/views/content/RouteView.vue'),
        meta: { title: '学习路线', requiresAuth: true, permission: 'route:view' },
      },
      {
        path: 'students',
        name: 'students',
        component: () => import('@/views/students/StudentListView.vue'),
        meta: { title: '学员管理', requiresAuth: true, permission: 'student:view' },
      },
      {
        path: 'system/users',
        name: 'system-users',
        component: () => import('@/views/system/UserListView.vue'),
        meta: { title: '用户管理', requiresAuth: true, permission: 'user:view' },
      },
      {
        path: 'system/roles',
        name: 'system-roles',
        component: () => import('@/views/system/RoleListView.vue'),
        meta: { title: '角色权限', requiresAuth: true, superAdminOnly: true },
      },
      {
        path: 'system/logs',
        name: 'system-logs',
        component: () => import('@/views/system/OperationLogView.vue'),
        meta: { title: '操作日志', requiresAuth: true, permission: 'system:log_view' },
      },
      {
        path: 'system/settings',
        name: 'system-settings',
        component: () => import('@/views/system/SettingView.vue'),
        meta: { title: '系统设置', requiresAuth: true, permission: 'system:setting' },
      },
    ],
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/AdminLoginView.vue'),
    meta: { title: '登录', guestOnly: true },
  },
  {
    path: '/forbidden',
    name: 'forbidden',
    component: () => import('@/views/ForbiddenView.vue'),
    meta: { title: '无权访问', requiresAuth: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

// 开发期布局预览开关：认证接口实现前可跳过前端登录守卫（仅前端放行，后端仍会校验）
const authBypass = import.meta.env.VITE_AUTH_BYPASS === 'true'

function setTitle(to: RouteLocationNormalized, siteName: string): void {
  const suffix = `${siteName} · 运营后台`
  document.title = to.meta.title ? `${to.meta.title} · ${suffix}` : suffix
}

/** 找到当前账号第一个有权限访问的后台页面，避免无 dashboard:view 的账号被循环重定向。 */
function firstAccessiblePath(): string {
  const permissionStore = usePermissionStore()
  const accessible = router
    .getRoutes()
    .filter((route) => route.meta.requiresAuth && route.path !== '/forbidden')
    .find((route) => {
      if (route.meta.superAdminOnly && !permissionStore.isSuperAdmin) {
        return false
      }
      return permissionStore.hasPermission(route.meta.permission)
    })
  return accessible?.path ?? '/forbidden'
}

router.beforeEach(async (to) => {
  if (authBypass) {
    setTitle(to, '云枢智学')
    return true
  }

  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const settingStore = useSettingStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (userStore.isLoggedIn) {
    if (!permissionStore.initialized) {
      const profile = await userStore.ensureProfile()
      if (!profile) {
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }
    await settingStore.load()

    if (to.meta.guestOnly) {
      return { path: firstAccessiblePath() }
    }
    if (to.meta.superAdminOnly && !permissionStore.isSuperAdmin) {
      return { path: firstAccessiblePath() }
    }
    if (to.meta.permission && !permissionStore.hasPermission(to.meta.permission)) {
      return { path: firstAccessiblePath() }
    }
  }

  setTitle(to, settingStore.siteName)
  return true
})

export default router
