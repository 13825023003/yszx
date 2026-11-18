<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { logApi, type LogListQuery } from '@/api/system'
import type { OperationLogItem } from '@/types/system'

const loading = ref(false)
const exporting = ref(false)
const list = ref<OperationLogItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dateRange = ref<[string, string] | null>(null)

const filters = reactive<{
  keyword: string
  operationType: string | undefined
}>({
  keyword: '',
  operationType: undefined,
})

const actionOptions = [
  { label: '登录', value: '登录' },
  { label: '登出', value: '登出' },
  { label: '新增 / 创建 / 上传', value: '新增' },
  { label: '修改 / 编辑', value: '修改' },
  { label: '删除', value: '删除' },
  { label: '禁用 / 启用', value: '禁用启用' },
  { label: '导出', value: '导出' },
  { label: '其他', value: '其他' },
]

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<OperationLogItem | null>(null)

function actionTagType(action: string): 'success' | 'primary' | 'warning' | 'danger' | 'info' {
  if (action === '登录') return 'success'
  if (action === '新增') return 'primary'
  if (action === '修改') return 'warning'
  if (action === '删除') return 'danger'
  if (action === '禁用启用') return 'warning'
  if (action === '导出') return 'success'
  return 'info'
}

function queryParams(): LogListQuery {
  return {
    keyword: filters.keyword.trim() || undefined,
    operationType: filters.operationType,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1],
    page: page.value,
    pageSize: pageSize.value,
  }
}

async function load() {
  loading.value = true
  try {
    const data = await logApi.list(queryParams())
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function resetFilters() {
  filters.keyword = ''
  filters.operationType = undefined
  dateRange.value = null
  page.value = 1
  load()
}

async function openDetail(row: OperationLogItem) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await logApi.detail(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function exportLogs() {
  if (total.value === 0) {
    ElMessage.warning('没有可导出的日志')
    return
  }
  exporting.value = true
  try {
    const blob = await logApi.exportFile({
      keyword: filters.keyword.trim() || undefined,
      operationType: filters.operationType,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `操作日志_${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    // 延迟释放，避免部分浏览器在下载启动前失效
    window.setTimeout(() => URL.revokeObjectURL(url), 3000)
    ElMessage.success('日志已导出')
    await load()
  } finally {
    exporting.value = false
  }
}

async function clearLogs() {
  const confirmed = await ElMessageBox.confirm(
    '清空后所有历史操作记录将被移除且不可恢复，请确认已完成必要的日志导出。',
    '清空操作日志',
    { confirmButtonText: '确认清空', cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  await logApi.clear()
  ElMessage.success('操作日志已清空')
  page.value = 1
  await load()
}

onMounted(load)
</script>

<template>
  <div class="log-page">
    <PageHeader title="操作日志" description="记录管理后台的关键操作，点击任意一行可查看完整详情。">
      <template #actions>
        <el-button :loading="exporting" @click="exportLogs">
          <el-icon><Download /></el-icon>
          <span>导出日志</span>
        </el-button>
        <el-button type="danger" plain @click="clearLogs">
          <el-icon><Delete /></el-icon>
          <span>清空日志</span>
        </el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          class="keyword-input"
          clearable
          placeholder="搜索用户 / 操作 / 目标"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select
          v-model="filters.operationType"
          class="filter-select"
          clearable
          placeholder="全部操作类型"
          @change="search"
        >
          <el-option
            v-for="item in actionOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          :unlink-panels="true"
          class="date-range"
          @change="search"
        />
        <el-button type="primary" plain @click="search">
          <el-icon><Search /></el-icon>
          <span>查询</span>
        </el-button>
        <el-button text @click="resetFilters">
          <el-icon><RefreshLeft /></el-icon>
          <span>重置</span>
        </el-button>
        <span class="toolbar-hint">共 {{ total }} 条日志</span>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        empty-text="暂无匹配的日志记录"
        class="log-table"
        @row-click="openDetail"
      >
        <el-table-column label="用户" width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="26" class="log-avatar">
                {{ row.username.slice(0, 1).toUpperCase() }}
              </el-avatar>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-tag :type="actionTagType(row.action)" effect="light" size="small">
              {{ row.action }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="目标" min-width="260" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP 地址" width="150" />
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column width="42">
          <template #default>
            <el-icon class="row-arrow"><ArrowRight /></el-icon>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="search"
        />
      </div>
    </section>

    <el-dialog v-model="detailVisible" title="日志详情" width="560px" destroy-on-close>
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-head">
            <span class="detail-icon">
              <el-icon :size="20"><Document /></el-icon>
            </span>
            <div>
              <div class="detail-action">
                <b>{{ detail.action }}</b>
                <el-tag :type="actionTagType(detail.action)" size="small" effect="plain">
                  {{ detail.targetType }}
                </el-tag>
              </div>
              <div class="detail-target">{{ detail.targetName }}</div>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-item"><span>操作用户</span><b>{{ detail.username }}</b></div>
            <div class="detail-item"><span>操作类型</span><b>{{ detail.action }}</b></div>
            <div class="detail-item"><span>操作对象</span><b>{{ detail.targetName }}</b></div>
            <div class="detail-item"><span>IP 地址</span><b>{{ detail.ip }}</b></div>
            <div class="detail-item"><span>操作时间</span><b>{{ detail.createdAt }}</b></div>
            <div class="detail-item"><span>日志 ID</span><b>{{ detail.id }}</b></div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">
          <el-icon><Close /></el-icon>
          <span>关闭</span>
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.log-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel {
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--surface);
}

.toolbar {
  display: flex;
  margin-bottom: 16px;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.keyword-input {
  width: 250px;
}

.filter-select {
  width: 170px;
}

.date-range {
  width: 260px !important;
}

.toolbar-hint {
  margin-left: auto;
  color: var(--muted);
  font-size: 12.5px;
}

.log-table :deep(.el-table__row) {
  cursor: pointer;
}

.log-table :deep(.el-table__row:hover > td) {
  background: #f8f9ff;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.log-avatar {
  flex: none;
  background: linear-gradient(135deg, #818cf8, #c084fc);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.row-arrow {
  color: var(--muted-2);
}

.pager {
  display: flex;
  margin-top: 16px;
  justify-content: flex-end;
}

.detail-body {
  min-height: 100px;
}

.detail-head {
  display: flex;
  padding: 14px;
  border-radius: 12px;
  align-items: flex-start;
  gap: 12px;
  background: var(--surface-2);
}

.detail-icon {
  display: grid;
  width: 38px;
  height: 38px;
  border: 1px solid var(--line);
  border-radius: 10px;
  place-items: center;
  background: #fff;
  color: var(--brand);
  flex: none;
}

.detail-action {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.detail-target {
  margin-top: 5px;
  color: var(--muted);
  font-size: 12.5px;
}

.detail-grid {
  display: grid;
  margin-top: 16px;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.detail-item {
  display: flex;
  padding: 10px 12px;
  border-radius: 9px;
  justify-content: space-between;
  gap: 12px;
  background: var(--surface-2);
  font-size: 12.5px;
}

.detail-item span {
  color: var(--muted);
}

.detail-item b {
  overflow: hidden;
  color: var(--text-2);
  font-weight: 600;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .keyword-input,
  .filter-select,
  .date-range {
    width: 100% !important;
  }

  .toolbar-hint {
    width: 100%;
    margin-left: 0;
  }
}
</style>
