<script setup lang="ts">
const types = [
  { value: 1, label: '单选题' },
  { value: 2, label: '多选题' },
  { value: 3, label: '判断题' },
  { value: 4, label: '简答题' },
]
const difficulties = [
  { value: 1, label: '简单' },
  { value: 2, label: '中等' },
  { value: 3, label: '困难' },
]
</script>

<template>
  <div class="page">
    <div class="page-card">
      <div class="header-row">
        <div>
          <h2 class="page-title">题库中心</h2>
          <p class="page-desc">
            游客可浏览题目列表与选项；登录后可查看答案、解析并进入练习。
          </p>
        </div>
        <el-tag type="info" effect="plain">接口待接入</el-tag>
      </div>

      <div class="filters">
        <el-select placeholder="全部学科" clearable style="width: 170px" disabled />
        <el-select placeholder="全部阶段" clearable style="width: 170px" disabled />
        <el-select placeholder="全部题型" clearable style="width: 140px" disabled>
          <el-option v-for="item in types" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select placeholder="全部难度" clearable style="width: 140px" disabled>
          <el-option
            v-for="item in difficulties"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-input placeholder="搜索题干关键词" style="width: 220px" disabled />
      </div>

      <el-empty description="题库接口接入后展示题目卡片" />
    </div>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 24px 0 8px;
}
</style>
