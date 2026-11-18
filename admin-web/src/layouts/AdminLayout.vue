<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'
import { useSettingStore } from '@/store/setting'

interface MenuItem {
  title: string
  icon: string
  path: string
  permission?: string
  superAdminOnly?: boolean
}

interface MenuGroup {
  title: string
  icon: string
  items: MenuItem[]
}

const menuGroups: MenuGroup[] = [
  {
    title: '概览',
    icon: 'Odometer',
    items: [{ title: '数据看板', icon: 'DataLine', path: '/dashboard', permission: 'dashboard:view' }],
  },
  {
    title: '内容管理',
    icon: 'Files',
    items: [
      {
        title: '学科管理',
        icon: 'Collection',
        path: '/content/subjects',
        permission: 'subject:view',
      },
      { title: 'B站课程库', icon: 'VideoPlay', path: '/content/courses', permission: 'course:view' },
      { title: '题库管理', icon: 'EditPen', path: '/content/questions', permission: 'question:view' },
      {
        title: '面试题库',
        icon: 'ChatDotSquare',
        path: '/content/interview-bank',
        permission: 'interviewbank:view',
      },
      { title: '学习路线', icon: 'Guide', path: '/content/routes', permission: 'route:view' },
    ],
  },
  {
    title: '学员管理',
    icon: 'User',
    items: [{ title: '学员管理', icon: 'User', path: '/students', permission: 'student:view' }],
  },
  {
    title: '系统管理',
    icon: 'Setting',
    items: [
      { title: '用户管理', icon: 'UserFilled', path: '/system/users', permission: 'user:view' },
      { title: '角色管理', icon: 'Lock', path: '/system/roles', superAdminOnly: true },
      { title: '操作日志', icon: 'Document', path: '/system/logs', permission: 'system:log_view' },
      { title: '系统设置', icon: 'Tools', path: '/system/settings', permission: 'system:setting' },
    ],
  },
]

const COLLAPSE_KEY = 'yszh.admin.sidebar_collapsed'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const settingStore = useSettingStore()

const isCollapse = ref(localStorage.getItem(COLLAPSE_KEY) === 'true')

function canShow(item: MenuItem): boolean {
  // 权限码未初始化时不隐藏菜单；接入权限加载后自动生效
  if (!permissionStore.initialized) {
    return true
  }
  if (item.superAdminOnly && !permissionStore.isSuperAdmin) {
    return false
  }
  return permissionStore.hasPermission(item.permission)
}

const visibleGroups = computed(() =>
  menuGroups
    .map((group) => ({ ...group, items: group.items.filter(canShow) }))
    .filter((group) => group.items.length > 0),
)

const defaultOpeneds = menuGroups.map((group) => group.title)

const activeGroup = computed(
  () => menuGroups.find((group) => group.items.some((item) => item.path === route.path))?.title ?? '',
)

const accountInitial = computed(() => (userStore.account || 'A').slice(0, 1).toUpperCase())

const portalUrl = computed(() => import.meta.env.VITE_PORTAL_URL || '/')

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
  localStorage.setItem(COLLAPSE_KEY, String(isCollapse.value))
}

function handleResize() {
  if (window.innerWidth < 900) {
    isCollapse.value = true
  }
}

async function handleLogout() {
  const confirmed = await ElMessageBox.confirm('确定退出登录吗？', '提示', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning',
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  await userStore.logout()
  permissionStore.reset()
  router.replace('/login')
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
  settingStore.load()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="admin-shell">
    <aside class="sidebar" :class="{ collapsed: isCollapse }">
      <router-link to="/dashboard" class="brand">
        <span class="brand-mark">
          <img v-if="settingStore.logoUrl" :src="settingStore.logoUrl" alt="Logo" />
          <template v-else>云</template>
        </span>
        <span v-show="!isCollapse" class="brand-text">{{ settingStore.siteName }} · 管理端</span>
      </router-link>

      <nav class="sidebar-nav">
        <el-menu
          :default-active="route.path"
          :default-openeds="defaultOpeneds"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          class="sidebar-menu"
        >
          <el-sub-menu v-for="group in visibleGroups" :key="group.title" :index="group.title">
            <template #title>
              <el-icon><component :is="group.icon" /></el-icon>
              <span class="group-title">{{ group.title }}</span>
            </template>
            <el-menu-item
              v-for="item in group.items"
              :key="item.path"
              :index="item.path"
              :title="item.title"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </nav>

      <div class="sidebar-foot">
        <div class="sidebar-user">
          <el-avatar :size="30" class="user-avatar">{{ accountInitial }}</el-avatar>
          <div v-show="!isCollapse" class="sidebar-user-info">
            <div class="name">{{ userStore.account || '管理员' }}</div>
            <div class="role">{{ userStore.roleName }}</div>
          </div>
        </div>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="topbar-left">
          <el-button text class="icon-button" title="折叠菜单" @click="toggleCollapse">
            <el-icon :size="18">
              <Expand v-if="isCollapse" />
              <Fold v-else />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-if="activeGroup">{{ activeGroup }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="topbar-right">
          <el-popover placement="bottom-end" :width="300" trigger="click">
            <template #reference>
              <el-button text class="icon-button" title="通知">
                <el-icon :size="17"><Bell /></el-icon>
              </el-button>
            </template>
            <div class="notify-panel">
              <div class="notify-head">
                <strong>通知中心</strong>
                <span class="text-muted">暂无未读消息</span>
              </div>
              <el-empty :image-size="56" description="暂无通知" />
            </div>
          </el-popover>

          <el-dropdown trigger="click">
            <span class="user-entry">
              <el-avatar :size="30" class="user-avatar">{{ accountInitial }}</el-avatar>
              <span class="user-name">{{ userStore.account || '管理员' }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <div class="dropdown-head">
                  <div class="dropdown-name">{{ userStore.account || '管理员' }}</div>
                  <div class="dropdown-role">{{ userStore.roleName }}</div>
                </div>
                <el-dropdown-item divided>
                  <a :href="portalUrl" class="dropdown-link">
                    <el-icon><Back /></el-icon>
                    <span>返回学员前台</span>
                  </a>
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100%;
  background: var(--bg);
}

.sidebar {
  position: sticky;
  top: 0;
  display: flex;
  width: 236px;
  height: 100vh;
  flex-shrink: 0;
  flex-direction: column;
  overflow: hidden;
  background: var(--sidebar-bg);
  transition: width 0.2s ease;
}

.sidebar.collapsed {
  width: 66px;
}

.brand {
  display: flex;
  height: 64px;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  color: #fff;
  font-size: 15.5px;
  font-weight: 750;
  white-space: nowrap;
}

.brand-mark {
  display: grid;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 9px;
  background: var(--grad);
  font-weight: 700;
  overflow: hidden;
}

.brand-mark img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sidebar-nav {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 12px 12px 20px;
}

.sidebar-nav::-webkit-scrollbar {
  width: 0;
}

.sidebar-menu {
  border-right: 0;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #a9b0c3;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-active-color: #fff;
}

.sidebar-menu :deep(.el-sub-menu) {
  margin-bottom: 4px;
}

.sidebar-menu :deep(.el-sub-menu__title),
.sidebar-menu :deep(.el-menu-item) {
  height: 44px;
  border-radius: 9px;
  font-size: 16px;
  line-height: 44px;
}

.sidebar-menu :deep(.el-sub-menu__title) {
  font-weight: 650;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  height: 42px;
  font-weight: 550;
  line-height: 42px;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.28), rgba(124, 58, 237, 0.14));
  font-weight: 650;
}

.sidebar-menu :deep(.el-sub-menu__title .el-icon) {
  font-size: 18px;
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  font-size: 16px;
}

.sidebar.collapsed .brand {
  justify-content: center;
  padding: 0;
}

.sidebar.collapsed .sidebar-nav {
  padding: 12px 0 20px;
}

.sidebar.collapsed .sidebar-menu :deep(.el-menu-item),
.sidebar.collapsed .sidebar-menu :deep(.el-sub-menu__title) {
  justify-content: center;
  padding: 0;
}

.sidebar.collapsed .sidebar-menu :deep(.el-sub-menu__icon-arrow) {
  display: none;
}

/* 折叠态下的子菜单通过 Teleport 渲染到 body，需全局设置字号 */
:global(.el-menu--popup) {
  min-width: 180px;
}

:global(.el-menu--popup .el-menu-item) {
  height: 42px;
  font-size: 16px;
  line-height: 42px;
}

.sidebar.collapsed .sidebar-foot {
  padding: 14px 0;
}

.sidebar.collapsed .sidebar-user {
  justify-content: center;
  padding: 6px 0;
}

.sidebar-foot {
  flex-shrink: 0;
  padding: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 9px;
}

.sidebar-user-info {
  min-width: 0;
}

.sidebar-user-info .name {
  overflow: hidden;
  color: #e5e7ef;
  font-size: 12.5px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-user-info .role {
  color: #7b8399;
  font-size: 10.5px;
}

.main {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  height: 64px;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid var(--line);
  background: #fff;
}

.topbar-left {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.topbar-left :deep(.el-breadcrumb) {
  font-size: 14px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.icon-button {
  width: 34px;
  height: 34px;
  padding: 0;
  border-radius: 8px;
  color: var(--muted);
}

.icon-button:hover {
  background: #f2f4f7;
  color: var(--text);
}

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px 4px 4px;
  border-radius: 999px;
  color: var(--text-2);
  cursor: pointer;
  outline: none;
  transition: background 0.15s;
}

.user-entry:hover {
  background: #f5f6fa;
}

.user-avatar {
  flex-shrink: 0;
  background: var(--grad);
  font-weight: 600;
}

.user-name {
  max-width: 96px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content {
  width: 100%;
  max-width: 1560px;
  flex: 1;
  margin: 0 auto;
  padding: 24px;
}

.notify-panel {
  padding: 2px 4px 4px;
}

.notify-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--line);
  font-size: 13px;
}

.notify-head .text-muted {
  font-size: 11.5px;
}

.dropdown-head {
  padding: 9px 11px 10px;
}

.dropdown-name {
  font-size: 13px;
  font-weight: 700;
}

.dropdown-role {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11.5px;
}

.dropdown-link {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 6px;
}

@media (max-width: 900px) {
  .sidebar {
    width: 66px;
  }

  .content {
    padding: 16px;
  }
}
</style>
