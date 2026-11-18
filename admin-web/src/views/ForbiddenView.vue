<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

async function logout() {
  await userStore.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="forbidden">
    <div class="forbidden-card">
      <span class="code">403</span>
      <h1>暂无访问权限</h1>
      <p>当前账号未被分配任何可访问的后台页面，请联系超级管理员调整角色权限。</p>
      <div class="actions">
        <el-button type="primary" @click="logout">
          <el-icon><SwitchButton /></el-icon>
          <span>重新登录</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.forbidden {
  display: grid;
  min-height: 100%;
  padding: 40px;
  place-items: center;
  background: var(--bg);
}

.forbidden-card {
  width: min(460px, 100%);
  padding: 40px 36px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--surface);
  text-align: center;
  box-shadow: 0 18px 50px rgba(16, 24, 40, 0.08);
}

.code {
  color: var(--brand);
  font-size: 54px;
  font-weight: 800;
  letter-spacing: 2px;
  line-height: 1;
}

h1 {
  margin: 18px 0 10px;
  font-size: 22px;
}

p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.8;
}

.actions {
  margin-top: 26px;
}
</style>
