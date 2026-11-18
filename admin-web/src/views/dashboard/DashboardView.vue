<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import BaseChart from '@/components/BaseChart.vue'
import PageHeader from '@/components/PageHeader.vue'

const stats = [
  { key: 'subjects', label: '学科数', icon: 'Collection', accent: '#4f46e5' },
  { key: 'questions', label: '题目数', icon: 'EditPen', accent: '#7c3aed' },
  { key: 'students', label: '注册用户', icon: 'User', accent: '#0ea5e9' },
  { key: 'interviews', label: 'AI 面试次数', icon: 'ChatDotSquare', accent: '#12b76a' },
]

const growthOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 16, right: 24, top: 24, bottom: 16, containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: [] },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f1f5' } } },
  series: [
    {
      name: '新增用户',
      type: 'line',
      smooth: true,
      data: [],
      symbolSize: 6,
      lineStyle: { width: 2.5, color: '#4f46e5' },
      areaStyle: { color: 'rgba(79, 70, 229, 0.10)' },
    },
  ],
  graphic: [
    {
      type: 'text',
      left: 'center',
      top: 'middle',
      style: { text: '看板数据接入后展示', fill: '#98a2b3', fontSize: 13 },
    },
  ],
}))

const activities = [] as Array<{ time: string; operator: string; action: string; target: string }>
</script>

<template>
  <div class="dashboard">
    <PageHeader title="数据看板" description="平台核心指标与近期动态，数据来源 A-D01 ~ A-D04。">
      <template #actions>
        <el-tag type="info" effect="plain">接口待接入</el-tag>
      </template>
    </PageHeader>

    <section class="stat-grid">
      <article v-for="item in stats" :key="item.key" class="stat-card">
        <div class="stat-icon" :style="{ background: `${item.accent}14`, color: item.accent }">
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-label">{{ item.label }}</span>
          <strong class="stat-value">--</strong>
          <span class="stat-trend">环比数据待接入</span>
        </div>
      </article>
    </section>

    <section class="panel-grid">
      <article class="panel">
        <header class="panel-head">
          <h2>用户增长</h2>
          <el-radio-group model-value="7" size="small" disabled>
            <el-radio-button value="7">7 天</el-radio-button>
            <el-radio-button value="30">30 天</el-radio-button>
            <el-radio-button value="90">90 天</el-radio-button>
          </el-radio-group>
        </header>
        <BaseChart :option="growthOption" height="300px" />
      </article>

      <article class="panel">
        <header class="panel-head">
          <h2>最近活动</h2>
          <el-tag type="info" effect="plain" size="small">operation_log</el-tag>
        </header>
        <el-table :data="activities" size="default" empty-text="暂无活动记录">
          <el-table-column prop="time" label="时间" width="160" />
          <el-table-column prop="operator" label="操作人" width="120" />
          <el-table-column prop="action" label="操作" width="120" />
          <el-table-column prop="target" label="对象" />
        </el-table>
      </article>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--surface);
}

.stat-icon {
  display: grid;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 12px;
}

.stat-meta {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  color: var(--muted);
  font-size: 13px;
}

.stat-value {
  font-size: 26px;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.stat-trend {
  color: var(--muted-2);
  font-size: 12px;
}

.panel-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  gap: 16px;
}

.panel {
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--surface);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-head h2 {
  margin: 0;
  font-size: 16px;
}

@media (max-width: 1100px) {
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
