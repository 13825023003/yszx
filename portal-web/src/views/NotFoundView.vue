<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="code">404</div>
    <h1>页面不存在</h1>
    <p class="text-muted">你访问的页面可能已被移动或删除。</p>
    <el-button type="primary" @click="router.replace('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px 20px;
  text-align: center;
}

.code {
  background: var(--grad);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-size: 92px;
  font-weight: 800;
  line-height: 1;
}

h1 {
  margin: 8px 0 0;
}

p {
  margin: 0 0 18px;
}
</style>
