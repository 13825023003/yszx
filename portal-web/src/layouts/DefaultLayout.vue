<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { label: '首页', path: '/' },
  { label: '题库中心', path: '/questions' },
  { label: '刷题练习', path: '/practice' },
  { label: '错题本', path: '/wrong-book' },
  { label: '学习路线', path: '/routes' },
  { label: 'AI 面试', path: '/interview' },
]

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="portal-shell">
    <header class="portal-header">
      <div class="header-inner">
        <router-link to="/" class="brand">
          <span class="brand-mark">云</span>
          <span class="brand-name">云枢智学</span>
        </router-link>

        <nav class="main-nav">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            active-class="nav-link--active"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="header-actions">
          <template v-if="userStore.isLoggedIn">
            <router-link to="/profile" class="profile-link">
              {{ userStore.profile?.nickname || '我的账户' }}
            </router-link>
            <el-button text @click="handleLogout">退出登录</el-button>
          </template>
          <template v-else>
            <router-link to="/login">
              <el-button text>登录</el-button>
            </router-link>
            <router-link to="/register">
              <el-button type="primary">注册</el-button>
            </router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="portal-main">
      <router-view />
    </main>

    <footer class="portal-footer">
      <span>云枢智学 · AI 赋能的智能面试训练平台</span>
      <span class="footer-muted">基础工程骨架</span>
    </footer>
  </div>
</template>

<style scoped>
.portal-shell {
  display: flex;
  min-height: 100%;
  flex-direction: column;
}

.portal-header {
  position: sticky;
  top: 0;
  z-index: 20;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}

.header-inner {
  display: flex;
  max-width: 1180px;
  align-items: center;
  gap: 28px;
  margin: 0 auto;
  padding: 14px 20px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  background: var(--grad);
  color: #fff;
  font-size: 17px;
}

.brand-name {
  letter-spacing: 0.5px;
}

.main-nav {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 4px;
}

.nav-link {
  padding: 8px 12px;
  border-radius: 9px;
  color: var(--text-2);
  font-size: 14px;
  transition: background-color 0.2s, color 0.2s;
}

.nav-link:hover {
  background: var(--brand-soft);
  color: var(--brand);
}

.nav-link--active {
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.profile-link {
  color: var(--text-2);
  font-size: 14px;
}

.portal-main {
  flex: 1;
}

.portal-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  max-width: 1180px;
  margin: 0 auto;
  padding: 20px;
  border-top: 1px solid var(--line);
  color: var(--muted);
  font-size: 13px;
}

.footer-muted {
  color: var(--muted-2);
}

@media (max-width: 900px) {
  .header-inner {
    flex-wrap: wrap;
    gap: 12px;
  }

  .main-nav {
    order: 3;
    width: 100%;
  }

  .header-actions {
    margin-left: auto;
  }
}
</style>
