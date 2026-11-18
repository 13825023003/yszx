<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
    tag?: string
  }>(),
  {
    description: '页面骨架已就绪，业务功能将在后续迭代中实现。',
    tag: '开发中',
  },
)
</script>

<template>
  <div class="page">
    <div class="page-card">
      <div class="placeholder-header">
        <div>
          <h2 class="page-title">{{ title }}</h2>
          <p class="page-desc">{{ description }}</p>
        </div>
        <el-tag type="info" effect="plain">{{ tag }}</el-tag>
      </div>
      <el-empty description="功能开发中，敬请期待" />
    </div>
  </div>
</template>

<style scoped>
.placeholder-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 8px;
}
</style>
