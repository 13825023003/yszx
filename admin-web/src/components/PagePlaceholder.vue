<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'

withDefaults(
  defineProps<{
    title: string
    description?: string
    emptyText?: string
  }>(),
  {
    description: '',
    emptyText: '页面骨架已就绪，业务功能将按设计文档逐步接入',
  },
)
</script>

<template>
  <div class="page">
    <PageHeader :title="title" :description="description">
      <template v-if="$slots.actions" #actions>
        <slot name="actions" />
      </template>
    </PageHeader>

    <section class="page-panel">
      <el-empty :description="emptyText" />
      <slot />
    </section>
  </div>
</template>

<style scoped>
.page {
  width: 100%;
}

.page-panel {
  padding: 48px 20px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--surface);
}
</style>
