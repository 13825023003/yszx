<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <router-link to="/" class="brand">
        <span class="brand-mark">云</span>
        <span class="brand-name">云枢智学</span>
      </router-link>
      <h1>{{ title }}</h1>
      <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
      <slot />
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100%;
  place-items: center;
  padding: 32px 20px;
  background:
    radial-gradient(circle at 12% 16%, rgba(79, 70, 229, 0.16), transparent 32%),
    radial-gradient(circle at 88% 84%, rgba(168, 85, 247, 0.14), transparent 34%),
    var(--bg);
}

.auth-card {
  width: min(440px, 100%);
  padding: 36px 34px;
  border: 1px solid rgba(234, 236, 240, 0.9);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 24px 60px rgba(16, 24, 40, 0.12);
  backdrop-filter: blur(10px);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 26px;
  font-size: 17px;
  font-weight: 700;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  background: var(--grad);
  color: #fff;
}

h1 {
  margin: 0 0 8px;
  font-size: 26px;
}

.subtitle {
  margin: 0 0 24px;
  color: var(--muted);
  line-height: 1.7;
}
</style>
